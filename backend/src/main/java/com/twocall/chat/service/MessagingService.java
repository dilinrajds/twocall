package com.twocall.chat.service;

import com.twocall.chat.domain.entity.*;
import com.twocall.chat.domain.enums.MessageStatus;
import com.twocall.chat.domain.enums.WsEventType;
import com.twocall.chat.dto.request.ReactionRequest;
import com.twocall.chat.dto.request.ReceiptUpdateRequest;
import com.twocall.chat.dto.request.SendMessageRequest;
import com.twocall.chat.dto.response.MessageResponse;
import com.twocall.chat.dto.ws.WsEvent;
import com.twocall.chat.exception.ResourceNotFoundException;
import com.twocall.chat.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessagingService {

    private static final Logger log = LoggerFactory.getLogger(MessagingService.class);

    private final MessageRepository messageRepository;
    private final DeviceRepository deviceRepository;
    private final PairRepository pairRepository;
    private final AttachmentRepository attachmentRepository;
    private final MessageReactionRepository reactionRepository;
    private final DeliveryReceiptRepository receiptRepository;
    private final WsSessionManager wsSessionManager;
    private final PushNotificationService pushNotificationService;

    public MessagingService(
            MessageRepository messageRepository,
            DeviceRepository deviceRepository,
            PairRepository pairRepository,
            AttachmentRepository attachmentRepository,
            MessageReactionRepository reactionRepository,
            DeliveryReceiptRepository receiptRepository,
            WsSessionManager wsSessionManager,
            PushNotificationService pushNotificationService) {
        this.messageRepository = messageRepository;
        this.deviceRepository = deviceRepository;
        this.pairRepository = pairRepository;
        this.attachmentRepository = attachmentRepository;
        this.reactionRepository = reactionRepository;
        this.receiptRepository = receiptRepository;
        this.wsSessionManager = wsSessionManager;
        this.pushNotificationService = pushNotificationService;
    }

    @Transactional
    public MessageResponse saveAndSendMessage(UUID pairId, UUID senderDeviceId, SendMessageRequest req) {
        // Idempotency check: if clientMessageId already exists in this pair, return existing
        Optional<Message> existing = messageRepository.findByPairIdAndClientMessageId(pairId, req.getClientMessageId());
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        Pair pair = pairRepository.findById(pairId)
                .orElseThrow(() -> new ResourceNotFoundException("Pair not found: " + pairId));
        Device senderDevice = deviceRepository.findById(senderDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender device not found"));

        Attachment attachment = null;
        if (req.getMediaAttachmentId() != null) {
            attachment = attachmentRepository.findByIdAndPairId(req.getMediaAttachmentId(), pairId)
                    .orElse(null);
        }

        Message message = new Message(
                UUID.randomUUID(),
                pair,
                senderDevice,
                req.getClientMessageId(),
                req.getCiphertextPayload(),
                req.getIv(),
                req.getEphemeralPublicKey(),
                req.getMessageType(),
                req.getReplyToMessageId(),
                attachment
        );

        message = messageRepository.save(message);

        MessageResponse responseDto = toDto(message);

        // Find partner device
        UUID partnerDeviceId = findPartnerDeviceId(pairId, senderDeviceId);

        // Send via WebSocket if partner is connected
        boolean partnerReceived = false;
        if (partnerDeviceId != null) {
            WsEvent<MessageResponse> wsEvent = new WsEvent<>(
                    WsEventType.MESSAGE_SENT,
                    pairId,
                    senderDeviceId,
                    partnerDeviceId,
                    responseDto
            );
            partnerReceived = wsSessionManager.sendToDevice(partnerDeviceId, wsEvent);

            if (partnerReceived) {
                // Partner received message immediately -> mark DELIVERED
                message.setStatus(MessageStatus.DELIVERED);
                messageRepository.save(message);
                responseDto.setStatus(MessageStatus.DELIVERED);

                // Send delivery confirmation back to sender
                wsSessionManager.sendToDevice(senderDeviceId, new WsEvent<>(
                        WsEventType.MESSAGE_DELIVERED,
                        pairId,
                        partnerDeviceId,
                        senderDeviceId,
                        Map.of("messageId", message.getId().toString(), "status", "DELIVERED")
                ));
            } else {
                // Partner offline -> trigger privacy-preserving FCM push notification
                pushNotificationService.sendNewMessageNotification(partnerDeviceId, message.getId());
            }
        }

        return responseDto;
    }

    @Transactional
    public void updateDeliveryReceipt(UUID pairId, UUID deviceId, ReceiptUpdateRequest req) {
        Message message = messageRepository.findByIdAndPairId(req.getMessageId(), pairId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        DeliveryReceipt receipt = receiptRepository.findByMessageIdAndDeviceId(req.getMessageId(), deviceId)
                .orElse(new DeliveryReceipt(UUID.randomUUID(), message, device, req.getStatus()));

        receipt.setStatus(req.getStatus());
        receipt.setUpdatedAt(Instant.now());
        receiptRepository.save(receipt);

        if ("READ".equalsIgnoreCase(req.getStatus())) {
            message.setStatus(MessageStatus.READ);
        } else if ("DELIVERED".equalsIgnoreCase(req.getStatus()) && message.getStatus() != MessageStatus.READ) {
            message.setStatus(MessageStatus.DELIVERED);
        }
        messageRepository.save(message);

        // Notify original sender via WebSocket
        UUID senderId = message.getSenderDevice().getId();
        WsEventType eventType = "READ".equalsIgnoreCase(req.getStatus()) ? WsEventType.MESSAGE_READ : WsEventType.MESSAGE_DELIVERED;
        wsSessionManager.sendToDevice(senderId, new WsEvent<>(
                eventType,
                pairId,
                deviceId,
                senderId,
                Map.of("messageId", message.getId().toString(), "status", req.getStatus())
        ));
    }

    @Transactional
    public void addOrUpdateReaction(UUID pairId, UUID deviceId, ReactionRequest req) {
        Message message = messageRepository.findByIdAndPairId(req.getMessageId(), pairId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        MessageReaction reaction = reactionRepository.findByMessageIdAndDeviceId(req.getMessageId(), deviceId)
                .orElse(new MessageReaction(UUID.randomUUID(), message, device, req.getEmoji()));

        reaction.setEmoji(req.getEmoji());
        reactionRepository.save(reaction);

        // Notify partner via WebSocket
        UUID partnerId = findPartnerDeviceId(pairId, deviceId);
        if (partnerId != null) {
            wsSessionManager.sendToDevice(partnerId, new WsEvent<>(
                    WsEventType.MESSAGE_REACTION,
                    pairId,
                    deviceId,
                    partnerId,
                    Map.of("messageId", message.getId().toString(), "emoji", req.getEmoji(), "deviceId", deviceId.toString())
            ));
        }
    }

    @Transactional
    public void deleteMessage(UUID pairId, UUID deviceId, UUID messageId) {
        Message message = messageRepository.findByIdAndPairId(messageId, pairId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        message.setDeleted(true);
        message.setCiphertextPayload(""); // Clear encrypted payload on deletion
        messageRepository.save(message);

        UUID partnerId = findPartnerDeviceId(pairId, deviceId);
        if (partnerId != null) {
            wsSessionManager.sendToDevice(partnerId, new WsEvent<>(
                    WsEventType.MESSAGE_DELETED,
                    pairId,
                    deviceId,
                    partnerId,
                    Map.of("messageId", messageId.toString())
            ));
        }
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> syncMessages(UUID pairId, Instant after) {
        List<Message> list;
        if (after != null) {
            list = messageRepository.findAllByPairIdAndCreatedAtAfterOrderByCreatedAtAsc(pairId, after);
        } else {
            list = messageRepository.findTop50ByPairIdOrderByCreatedAtDesc(pairId);
            Collections.reverse(list); // Chronological order
        }
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    private UUID findPartnerDeviceId(UUID pairId, UUID myDeviceId) {
        List<Device> devices = deviceRepository.findAllByPairId(pairId);
        for (Device d : devices) {
            if (!d.getId().equals(myDeviceId)) {
                return d.getId();
            }
        }
        return null;
    }

    private MessageResponse toDto(Message m) {
        Map<String, String> reactionMap = new HashMap<>();
        if (m.getReactions() != null) {
            for (MessageReaction r : m.getReactions()) {
                reactionMap.put(r.getDevice().getId().toString(), r.getEmoji());
            }
        }

        return new MessageResponse(
                m.getId(),
                m.getPair().getId(),
                m.getSenderDevice().getId(),
                m.getClientMessageId(),
                m.getCiphertextPayload(),
                m.getIv(),
                m.getEphemeralPublicKey(),
                m.getMessageType(),
                m.getReplyToMessageId(),
                m.getMediaAttachment() != null ? m.getMediaAttachment().getId() : null,
                m.getStatus(),
                m.isDeleted(),
                m.getCreatedAt(),
                m.getEditedAt(),
                reactionMap
        );
    }
}
