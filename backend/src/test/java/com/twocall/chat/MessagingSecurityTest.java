package com.twocall.chat;

import com.twocall.chat.domain.entity.Device;
import com.twocall.chat.domain.entity.Message;
import com.twocall.chat.domain.entity.Pair;
import com.twocall.chat.domain.enums.MessageType;
import com.twocall.chat.dto.request.SendMessageRequest;
import com.twocall.chat.dto.response.MessageResponse;
import com.twocall.chat.exception.UnauthorizedPairAccessException;
import com.twocall.chat.repository.*;
import com.twocall.chat.security.DevicePrincipal;
import com.twocall.chat.security.PairAccessValidator;
import com.twocall.chat.service.MessagingService;
import com.twocall.chat.service.PushNotificationService;
import com.twocall.chat.service.WsSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagingSecurityTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private PairRepository pairRepository;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private MessageReactionRepository reactionRepository;
    @Mock
    private DeliveryReceiptRepository receiptRepository;
    @Mock
    private WsSessionManager wsSessionManager;
    @Mock
    private PushNotificationService pushNotificationService;

    private MessagingService messagingService;
    private PairAccessValidator accessValidator;

    @BeforeEach
    void setUp() {
        messagingService = new MessagingService(
                messageRepository,
                deviceRepository,
                pairRepository,
                attachmentRepository,
                reactionRepository,
                receiptRepository,
                wsSessionManager,
                pushNotificationService
        );
        accessValidator = new PairAccessValidator();
    }

    @Test
    @DisplayName("Should save and send encrypted message with ciphertext and IV")
    void testSaveAndSendMessage() {
        UUID pairId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UUID partnerDeviceId = UUID.randomUUID();

        Pair pair = new Pair(pairId);
        Device sender = new Device(deviceId, pair, "fp-1", "key-1", "Phone 1");
        Device partner = new Device(partnerDeviceId, pair, "fp-2", "key-2", "Phone 2");

        when(messageRepository.findByPairIdAndClientMessageId(any(), any())).thenReturn(Optional.empty());
        when(pairRepository.findById(pairId)).thenReturn(Optional.of(pair));
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(sender));
        when(deviceRepository.findAllByPairId(pairId)).thenReturn(List.of(sender, partner));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));

        SendMessageRequest req = new SendMessageRequest(
                UUID.randomUUID().toString(),
                "ENCRYPTED_BASE64_CIPHERTEXT_ABC123==",
                "IV_NONCE_XYZ987==",
                null,
                MessageType.TEXT,
                null,
                null
        );

        MessageResponse response = messagingService.saveAndSendMessage(pairId, deviceId, req);

        assertNotNull(response);
        assertEquals("ENCRYPTED_BASE64_CIPHERTEXT_ABC123==", response.getCiphertextPayload());
        assertEquals("IV_NONCE_XYZ987==", response.getIv());
        assertEquals(pairId, response.getPairId());
        assertEquals(deviceId, response.getSenderDeviceId());

        verify(messageRepository).save(any(Message.class));
    }

    @Test
    @DisplayName("PairAccessValidator must reject access when device attempts to access another pair (IDOR)")
    void testCrossPairAccessRejection() {
        UUID actualPairId = UUID.randomUUID();
        UUID attackerDeviceId = UUID.randomUUID();
        UUID targetVictimPairId = UUID.randomUUID();

        DevicePrincipal principal = new DevicePrincipal(attackerDeviceId, actualPairId, "attacker-fingerprint");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        // Accessing caller's own pair should succeed
        assertDoesNotThrow(() -> accessValidator.validatePairAccess(actualPairId));

        // Accessing victim's pair must throw UnauthorizedPairAccessException!
        assertThrows(UnauthorizedPairAccessException.class, () -> accessValidator.validatePairAccess(targetVictimPairId));
    }
}
