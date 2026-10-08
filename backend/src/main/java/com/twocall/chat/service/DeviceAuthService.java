package com.twocall.chat.service;

import com.twocall.chat.domain.entity.Device;
import com.twocall.chat.domain.entity.DeviceSession;
import com.twocall.chat.domain.entity.Pair;
import com.twocall.chat.domain.enums.PairStatus;
import com.twocall.chat.dto.response.DevicePairInfoResponse;
import com.twocall.chat.dto.response.TokenResponse;
import com.twocall.chat.exception.ResourceNotFoundException;
import com.twocall.chat.exception.UnauthorizedPairAccessException;
import com.twocall.chat.repository.DeviceRepository;
import com.twocall.chat.repository.DeviceSessionRepository;
import com.twocall.chat.repository.PairRepository;
import com.twocall.chat.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.twocall.chat.domain.enums.WsEventType;
import com.twocall.chat.dto.ws.WsEvent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DeviceAuthService {

    private static final Logger log = LoggerFactory.getLogger(DeviceAuthService.class);

    private final DeviceSessionRepository sessionRepository;
    private final DeviceRepository deviceRepository;
    private final PairRepository pairRepository;
    private final JwtTokenProvider tokenProvider;
    private final WsSessionManager wsSessionManager;

    public DeviceAuthService(
            DeviceSessionRepository sessionRepository,
            DeviceRepository deviceRepository,
            PairRepository pairRepository,
            JwtTokenProvider tokenProvider,
            WsSessionManager wsSessionManager) {
        this.sessionRepository = sessionRepository;
        this.deviceRepository = deviceRepository;
        this.pairRepository = pairRepository;
        this.tokenProvider = tokenProvider;
        this.wsSessionManager = wsSessionManager;
    }

    @Transactional
    public String createSession(Device device, String accessToken) {
        String rawRefreshToken = tokenProvider.generateRawRefreshToken();
        String refreshHash = tokenProvider.hashToken(rawRefreshToken);
        Instant expiry = Instant.now().plusMillis(tokenProvider.getRefreshTokenExpirationMs());

        DeviceSession session = new DeviceSession(
                UUID.randomUUID(),
                device,
                refreshHash,
                tokenProvider.getClaims(accessToken).getId(),
                expiry
        );
        sessionRepository.save(session);
        return rawRefreshToken;
    }

    @Transactional
    public TokenResponse refreshAccessToken(String rawRefreshToken) {
        String refreshHash = tokenProvider.hashToken(rawRefreshToken);
        DeviceSession session = sessionRepository.findByRefreshTokenHashAndRevokedFalse(refreshHash)
                .orElseThrow(() -> new UnauthorizedPairAccessException("Invalid or revoked refresh token"));

        if (!session.isValid()) {
            session.setRevoked(true);
            sessionRepository.save(session);
            throw new UnauthorizedPairAccessException("Refresh token has expired");
        }

        Device device = session.getDevice();
        Pair pair = device.getPair();

        if (pair.getStatus() != PairStatus.ACTIVE) {
            throw new UnauthorizedPairAccessException("Pair is terminated");
        }

        // Rotate refresh token (One-Time-Use Refresh Token Security)
        session.setRevoked(true);
        sessionRepository.save(session);

        String newAccessToken = tokenProvider.generateAccessToken(device.getId(), pair.getId());
        String newRefreshToken = createSession(device, newAccessToken);

        return new TokenResponse(newAccessToken, newRefreshToken, tokenProvider.getAccessTokenExpirationMs());
    }

    @Transactional(readOnly = true)
    public DevicePairInfoResponse getPairInfo(UUID deviceId, UUID pairId) {
        Pair pair = pairRepository.findById(pairId)
                .orElseThrow(() -> new ResourceNotFoundException("Pair not found: " + pairId));

        List<Device> devices = deviceRepository.findAllByPairId(pairId);
        Device myDevice = null;
        Device partnerDevice = null;

        for (Device d : devices) {
            if (d.getId().equals(deviceId)) {
                myDevice = d;
            } else {
                partnerDevice = d;
            }
        }

        if (myDevice == null) {
            throw new UnauthorizedPairAccessException("Device not in pair");
        }

        return new DevicePairInfoResponse(
                pairId,
                deviceId,
                partnerDevice != null ? partnerDevice.getId() : null,
                partnerDevice != null ? partnerDevice.getPublicIdentityKey() : null,
                pair.getStatus()
        );
    }

    @Transactional
    public void disconnectDevice(UUID deviceId, UUID pairId) {
        List<Device> devices = deviceRepository.findAllByPairId(pairId);
        for (Device d : devices) {
            if (!d.getId().equals(deviceId)) {
                wsSessionManager.sendToDevice(d.getId(), new WsEvent<>(
                        WsEventType.PRESENCE,
                        pairId,
                        deviceId,
                        d.getId(),
                        Map.of(
                                "event", "PARTNER_DISCONNECTED",
                                "message", "Your partner has disconnected"
                        )
                ));
            }
        }

        for (Device d : devices) {
            wsSessionManager.closeDeviceSession(d.getId());
        }

        Pair pair = pairRepository.findById(pairId)
                .orElseThrow(() -> new ResourceNotFoundException("Pair not found: " + pairId));
        pairRepository.delete(pair);
        log.info("Device {} disconnected. Pair {} deleted and partner notified.", deviceId, pairId);
    }

    @Transactional
    public void deletePair(UUID pairId, UUID callerDeviceId) {
        Pair pair = pairRepository.findById(pairId)
                .orElseThrow(() -> new ResourceNotFoundException("Pair not found"));

        List<Device> devices = deviceRepository.findAllByPairId(pairId);
        for (Device d : devices) {
            if (!d.getId().equals(callerDeviceId)) {
                wsSessionManager.sendToDevice(d.getId(), new WsEvent<>(
                        WsEventType.PRESENCE,
                        pairId,
                        callerDeviceId,
                        d.getId(),
                        Map.of(
                                "event", "PAIR_DELETED",
                                "message", "The pair has been deleted by your partner"
                        )
                ));
            }
        }

        for (Device d : devices) {
            wsSessionManager.closeDeviceSession(d.getId());
        }

        pairRepository.delete(pair);
        log.info("Pair {} and all associated data permanently deleted by device {}", pairId, callerDeviceId);
    }
}
