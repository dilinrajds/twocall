package com.twocall.chat;

import com.twocall.chat.config.RateLimitConfig;
import com.twocall.chat.domain.entity.Device;
import com.twocall.chat.domain.entity.Pair;
import com.twocall.chat.domain.entity.PairingCode;
import com.twocall.chat.dto.request.CreatePairRequest;
import com.twocall.chat.dto.request.JoinPairRequest;
import com.twocall.chat.dto.response.CreatePairResponse;
import com.twocall.chat.dto.response.JoinPairResponse;
import com.twocall.chat.exception.InvalidPairingCodeException;
import com.twocall.chat.exception.PairLimitExceededException;
import com.twocall.chat.exception.PairingCodeExpiredException;
import com.twocall.chat.exception.RateLimitExceededException;
import com.twocall.chat.repository.DeviceRepository;
import com.twocall.chat.repository.PairRepository;
import com.twocall.chat.repository.PairingCodeRepository;
import com.twocall.chat.security.JwtTokenProvider;
import com.twocall.chat.service.DeviceAuthService;
import com.twocall.chat.service.PairingService;
import com.twocall.chat.service.WsSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PairingServiceTest {

    @Mock
    private PairRepository pairRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private PairingCodeRepository pairingCodeRepository;
    @Mock
    private DeviceAuthService deviceAuthService;
    @Mock
    private WsSessionManager wsSessionManager;

    private JwtTokenProvider tokenProvider;
    private RateLimitConfig rateLimitConfig;
    private PairingService pairingService;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "4Z03kI3/YpA2W5Q6kFv7eJ8lM9nO0pQ1rS2tU3vW4xY5zA6bC7dE8fG9hI0jK1lM2nO3pQ4rS5tU6vW7xY8z9A==",
                86400000,
                2592000000L
        );
        rateLimitConfig = new RateLimitConfig(10);
        pairingService = new PairingService(
                pairRepository,
                deviceRepository,
                pairingCodeRepository,
                deviceAuthService,
                tokenProvider,
                rateLimitConfig,
                wsSessionManager,
                300,
                5
        );
    }

    @Test
    @DisplayName("Create Pair should generate 6-digit code and save hashed representation")
    void testCreatePair() {
        when(pairRepository.save(any(Pair.class))).thenAnswer(i -> i.getArgument(0));
        when(deviceRepository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));
        when(pairingCodeRepository.save(any(PairingCode.class))).thenAnswer(i -> i.getArgument(0));
        when(deviceAuthService.createSession(any(Device.class), anyString())).thenReturn("mock-refresh-token");

        CreatePairRequest req = new CreatePairRequest("fingerprint-A", "pubkey-A", "Phone A");
        CreatePairResponse res = pairingService.createPair(req, "127.0.0.1");

        assertNotNull(res);
        assertNotNull(res.getPairId());
        assertNotNull(res.getPairingCode());
        assertEquals(6, res.getPairingCode().length());
        assertTrue(res.getPairingCode().matches("^[0-9]{6}$"));
        assertNotNull(res.getAccessToken());
        assertEquals("mock-refresh-token", res.getRefreshToken());

        verify(pairingCodeRepository).save(argThat(pc -> !pc.getCodeHash().equals(res.getPairingCode())));
    }

    @Test
    @DisplayName("Join Pair should successfully link second device and invalidate code")
    void testJoinPairSuccess() {
        UUID pairId = UUID.randomUUID();
        Pair pair = new Pair(pairId);
        Device creator = new Device(UUID.randomUUID(), pair, "fp-A", "pubkey-A", "Phone A");
        String code = "123456";
        String codeHash = tokenProvider.hashToken(code);

        PairingCode pairingCode = new PairingCode(UUID.randomUUID(), codeHash, pair, creator, Instant.now().plusSeconds(300), 5);

        when(pairingCodeRepository.findByCodeHash(codeHash)).thenReturn(Optional.of(pairingCode));
        when(deviceRepository.countByPairId(pairId)).thenReturn(1L);
        when(deviceRepository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));
        when(pairingCodeRepository.save(any(PairingCode.class))).thenAnswer(i -> i.getArgument(0));
        when(deviceAuthService.createSession(any(Device.class), anyString())).thenReturn("mock-refresh-B");

        JoinPairRequest req = new JoinPairRequest(code, "fp-B", "pubkey-B", "Phone B");
        JoinPairResponse res = pairingService.joinPair(req, "127.0.0.2");

        assertNotNull(res);
        assertEquals(pairId, res.getPairId());
        assertEquals(creator.getId(), res.getPartnerDeviceId());
        assertEquals("pubkey-A", res.getPartnerPublicKey());
        assertTrue(pairingCode.isUsed(), "Pairing code must be marked as used immediately");
    }

    @Test
    @DisplayName("Join Pair should reject already used pairing code")
    void testJoinPairReusedCodeRejection() {
        String code = "123456";
        String codeHash = tokenProvider.hashToken(code);
        Pair pair = new Pair();
        Device creator = new Device(UUID.randomUUID(), pair, "fp-A", "pubkey-A", "Phone A");
        PairingCode pairingCode = new PairingCode(UUID.randomUUID(), codeHash, pair, creator, Instant.now().plusSeconds(300), 5);
        pairingCode.markUsed(); // already used!

        when(pairingCodeRepository.findByCodeHash(codeHash)).thenReturn(Optional.of(pairingCode));

        JoinPairRequest req = new JoinPairRequest(code, "fp-B", "pubkey-B", "Phone B");
        assertThrows(InvalidPairingCodeException.class, () -> pairingService.joinPair(req, "127.0.0.3"));
    }

    @Test
    @DisplayName("Join Pair should reject expired pairing code")
    void testJoinPairExpiredCodeRejection() {
        String code = "654321";
        String codeHash = tokenProvider.hashToken(code);
        Pair pair = new Pair();
        Device creator = new Device(UUID.randomUUID(), pair, "fp-A", "pubkey-A", "Phone A");
        // Expired 10 seconds ago
        PairingCode pairingCode = new PairingCode(UUID.randomUUID(), codeHash, pair, creator, Instant.now().minusSeconds(10), 5);

        when(pairingCodeRepository.findByCodeHash(codeHash)).thenReturn(Optional.of(pairingCode));

        JoinPairRequest req = new JoinPairRequest(code, "fp-B", "pubkey-B", "Phone B");
        assertThrows(PairingCodeExpiredException.class, () -> pairingService.joinPair(req, "127.0.0.4"));
    }

    @Test
    @DisplayName("Join Pair should reject third device when 2 devices are already paired")
    void testThirdDeviceRejection() {
        String code = "888999";
        String codeHash = tokenProvider.hashToken(code);
        UUID pairId = UUID.randomUUID();
        Pair pair = new Pair(pairId);
        Device creator = new Device(UUID.randomUUID(), pair, "fp-A", "pubkey-A", "Phone A");
        PairingCode pairingCode = new PairingCode(UUID.randomUUID(), codeHash, pair, creator, Instant.now().plusSeconds(300), 5);

        when(pairingCodeRepository.findByCodeHash(codeHash)).thenReturn(Optional.of(pairingCode));
        when(deviceRepository.countByPairId(pairId)).thenReturn(2L); // Already 2 devices!

        JoinPairRequest req = new JoinPairRequest(code, "fp-C", "pubkey-C", "Phone C");
        assertThrows(PairLimitExceededException.class, () -> pairingService.joinPair(req, "127.0.0.5"));
    }

    @Test
    @DisplayName("Pairing should enforce rate limits per client IP")
    void testRateLimitEnforcement() {
        RateLimitConfig strictLimiter = new RateLimitConfig(2);
        PairingService serviceWithStrictLimiter = new PairingService(
                pairRepository, deviceRepository, pairingCodeRepository, deviceAuthService,
                tokenProvider, strictLimiter, wsSessionManager, 300, 5
        );

        when(pairRepository.save(any(Pair.class))).thenReturn(new Pair());
        when(deviceRepository.save(any(Device.class))).thenReturn(new Device());
        when(pairingCodeRepository.save(any(PairingCode.class))).thenReturn(new PairingCode());
        when(deviceAuthService.createSession(any(Device.class), anyString())).thenReturn("token");

        CreatePairRequest req = new CreatePairRequest("fp", "key", "Phone");
        // 1st request ok
        serviceWithStrictLimiter.createPair(req, "192.168.1.100");
        // 2nd request ok
        serviceWithStrictLimiter.createPair(req, "192.168.1.100");
        // 3rd request exceeds limit of 2!
        assertThrows(RateLimitExceededException.class, () -> serviceWithStrictLimiter.createPair(req, "192.168.1.100"));
    }
}
