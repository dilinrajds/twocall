package com.twocall.chat.service;

import com.twocall.chat.config.RateLimitConfig;
import com.twocall.chat.domain.entity.Device;
import com.twocall.chat.domain.entity.Pair;
import com.twocall.chat.domain.entity.PairingCode;
import com.twocall.chat.dto.request.CreatePairRequest;
import com.twocall.chat.dto.request.JoinPairRequest;
import com.twocall.chat.dto.response.CreatePairResponse;
import com.twocall.chat.dto.response.JoinPairResponse;
import com.twocall.chat.dto.ws.WsEvent;
import com.twocall.chat.domain.enums.WsEventType;
import com.twocall.chat.exception.InvalidPairingCodeException;
import com.twocall.chat.exception.PairLimitExceededException;
import com.twocall.chat.exception.PairingCodeExpiredException;
import com.twocall.chat.repository.DeviceRepository;
import com.twocall.chat.repository.PairRepository;
import com.twocall.chat.repository.PairingCodeRepository;
import com.twocall.chat.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PairingService {

    private static final Logger log = LoggerFactory.getLogger(PairingService.class);

    private final PairRepository pairRepository;
    private final DeviceRepository deviceRepository;
    private final PairingCodeRepository pairingCodeRepository;
    private final DeviceAuthService deviceAuthService;
    private final JwtTokenProvider tokenProvider;
    private final RateLimitConfig rateLimitConfig;
    private final WsSessionManager wsSessionManager;

    private final int codeTtlSeconds;
    private final int maxAttempts;
    private final SecureRandom secureRandom = new SecureRandom();

    public PairingService(
            PairRepository pairRepository,
            DeviceRepository deviceRepository,
            PairingCodeRepository pairingCodeRepository,
            DeviceAuthService deviceAuthService,
            JwtTokenProvider tokenProvider,
            RateLimitConfig rateLimitConfig,
            WsSessionManager wsSessionManager,
            @Value("${app.pairing.code-ttl-seconds:300}") int codeTtlSeconds,
            @Value("${app.pairing.max-attempts:5}") int maxAttempts) {
        this.pairRepository = pairRepository;
        this.deviceRepository = deviceRepository;
        this.pairingCodeRepository = pairingCodeRepository;
        this.deviceAuthService = deviceAuthService;
        this.tokenProvider = tokenProvider;
        this.rateLimitConfig = rateLimitConfig;
        this.wsSessionManager = wsSessionManager;
        this.codeTtlSeconds = codeTtlSeconds;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public CreatePairResponse createPair(CreatePairRequest request, String clientIp) {
        rateLimitConfig.checkRateLimit(clientIp);

        // 1. Generate 6-digit random code
        int codeInt = 100000 + secureRandom.nextInt(900000);
        String plainCode = String.valueOf(codeInt);
        String codeHash = tokenProvider.hashToken(plainCode);

        // 2. Create Pair
        Pair pair = new Pair();
        pair = pairRepository.save(pair);

        // 3. Create Creator Device
        Device creatorDevice = new Device(
                UUID.randomUUID(),
                pair,
                request.getDeviceFingerprint(),
                request.getPublicIdentityKey(),
                request.getDeviceLabel() != null ? request.getDeviceLabel() : "Device A"
        );
        creatorDevice = deviceRepository.save(creatorDevice);

        // 4. Save PairingCode (hashed only)
        Instant expiresAt = Instant.now().plusSeconds(codeTtlSeconds);
        PairingCode pairingCode = new PairingCode(
                UUID.randomUUID(),
                codeHash,
                pair,
                creatorDevice,
                expiresAt,
                maxAttempts
        );
        pairingCodeRepository.save(pairingCode);

        // 5. Generate secure session credentials
        String accessToken = tokenProvider.generateAccessToken(creatorDevice.getId(), pair.getId());
        String refreshToken = deviceAuthService.createSession(creatorDevice, accessToken);

        log.info("Pair created: pairId={} creatorDeviceId={} (code expires in {}s)",
                pair.getId(), creatorDevice.getId(), codeTtlSeconds);

        return new CreatePairResponse(
                pair.getId(),
                plainCode,
                expiresAt,
                creatorDevice.getId(),
                accessToken,
                refreshToken
        );
    }

    @Transactional
    public JoinPairResponse joinPair(JoinPairRequest request, String clientIp) {
        rateLimitConfig.checkRateLimit(clientIp);

        String codeHash = tokenProvider.hashToken(request.getCode().trim());
        PairingCode pairingCode = pairingCodeRepository.findByCodeHash(codeHash)
                .orElseThrow(() -> new InvalidPairingCodeException("Invalid pairing code entered"));

        // Check if code has been exhausted or is expired
        if (pairingCode.isUsed()) {
            throw new InvalidPairingCodeException("Pairing code has already been used");
        }

        pairingCode.incrementAttempts();

        if (Instant.now().isAfter(pairingCode.getExpiresAt())) {
            pairingCodeRepository.save(pairingCode);
            throw new PairingCodeExpiredException("Pairing code has expired. Request a new code.");
        }

        if (pairingCode.getAttemptsCount() > pairingCode.getMaxAttempts()) {
            pairingCodeRepository.save(pairingCode);
            throw new InvalidPairingCodeException("Too many incorrect attempts. Code is now invalidated.");
        }

        Pair pair = pairingCode.getPair();
        long currentDeviceCount = deviceRepository.countByPairId(pair.getId());
        if (currentDeviceCount >= 2) {
            throw new PairLimitExceededException("This pair already has the maximum of 2 devices connected.");
        }

        // Create Joined Device
        Device joinedDevice = new Device(
                UUID.randomUUID(),
                pair,
                request.getDeviceFingerprint(),
                request.getPublicIdentityKey(),
                request.getDeviceLabel() != null ? request.getDeviceLabel() : "Device B"
        );
        joinedDevice = deviceRepository.save(joinedDevice);

        // Immediately invalidate pairing code
        pairingCode.markUsed();
        pairingCodeRepository.save(pairingCode);

        // Fetch User A (Creator)
        Device creatorDevice = pairingCode.getCreatorDevice();

        // Generate credentials for User B
        String accessToken = tokenProvider.generateAccessToken(joinedDevice.getId(), pair.getId());
        String refreshToken = deviceAuthService.createSession(joinedDevice, accessToken);

        // Notify User A via WebSocket if currently connected
        wsSessionManager.sendToDevice(creatorDevice.getId(), new WsEvent<>(
                WsEventType.PRESENCE,
                pair.getId(),
                joinedDevice.getId(),
                creatorDevice.getId(),
                Map.of(
                        "event", "PAIRING_COMPLETE",
                        "partnerDeviceId", joinedDevice.getId().toString(),
                        "partnerPublicKey", joinedDevice.getPublicIdentityKey()
                )
        ));

        log.info("Pair joined successfully: pairId={} joinedDeviceId={} partnerDeviceId={}",
                pair.getId(), joinedDevice.getId(), creatorDevice.getId());

        return new JoinPairResponse(
                pair.getId(),
                joinedDevice.getId(),
                accessToken,
                refreshToken,
                creatorDevice.getId(),
                creatorDevice.getPublicIdentityKey()
        );
    }
}
