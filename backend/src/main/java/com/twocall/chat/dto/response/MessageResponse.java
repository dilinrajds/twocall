package com.twocall.chat.dto.response;

import com.twocall.chat.domain.enums.MessageStatus;
import com.twocall.chat.domain.enums.MessageType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class MessageResponse {
    private UUID id;
    private UUID pairId;
    private UUID senderDeviceId;
    private String clientMessageId;
    private String ciphertextPayload;
    private String iv;
    private String ephemeralPublicKey;
    private MessageType messageType;
    private UUID replyToMessageId;
    private UUID mediaAttachmentId;
    private MessageStatus status;
    private boolean isDeleted;
    private Instant createdAt;
    private Instant editedAt;
    private Map<String, String> reactions; // deviceId -> emoji

    public MessageResponse() {}

    public MessageResponse(UUID id, UUID pairId, UUID senderDeviceId, String clientMessageId,
                           String ciphertextPayload, String iv, String ephemeralPublicKey,
                           MessageType messageType, UUID replyToMessageId, UUID mediaAttachmentId,
                           MessageStatus status, boolean isDeleted, Instant createdAt, Instant editedAt,
                           Map<String, String> reactions) {
        this.id = id;
        this.pairId = pairId;
        this.senderDeviceId = senderDeviceId;
        this.clientMessageId = clientMessageId;
        this.ciphertextPayload = ciphertextPayload;
        this.iv = iv;
        this.ephemeralPublicKey = ephemeralPublicKey;
        this.messageType = messageType;
        this.replyToMessageId = replyToMessageId;
        this.mediaAttachmentId = mediaAttachmentId;
        this.status = status;
        this.isDeleted = isDeleted;
        this.createdAt = createdAt;
        this.editedAt = editedAt;
        this.reactions = reactions;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPairId() {
        return pairId;
    }

    public void setPairId(UUID pairId) {
        this.pairId = pairId;
    }

    public UUID getSenderDeviceId() {
        return senderDeviceId;
    }

    public void setSenderDeviceId(UUID senderDeviceId) {
        this.senderDeviceId = senderDeviceId;
    }

    public String getClientMessageId() {
        return clientMessageId;
    }

    public void setClientMessageId(String clientMessageId) {
        this.clientMessageId = clientMessageId;
    }

    public String getCiphertextPayload() {
        return ciphertextPayload;
    }

    public void setCiphertextPayload(String ciphertextPayload) {
        this.ciphertextPayload = ciphertextPayload;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getEphemeralPublicKey() {
        return ephemeralPublicKey;
    }

    public void setEphemeralPublicKey(String ephemeralPublicKey) {
        this.ephemeralPublicKey = ephemeralPublicKey;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public UUID getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(UUID replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public UUID getMediaAttachmentId() {
        return mediaAttachmentId;
    }

    public void setMediaAttachmentId(UUID mediaAttachmentId) {
        this.mediaAttachmentId = mediaAttachmentId;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Instant editedAt) {
        this.editedAt = editedAt;
    }

    public Map<String, String> getReactions() {
        return reactions;
    }

    public void setReactions(Map<String, String> reactions) {
        this.reactions = reactions;
    }
}
