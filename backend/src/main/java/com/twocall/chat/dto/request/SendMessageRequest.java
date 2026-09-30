package com.twocall.chat.dto.request;

import com.twocall.chat.domain.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class SendMessageRequest {

    @NotBlank(message = "clientMessageId cannot be blank")
    private String clientMessageId;

    @NotBlank(message = "ciphertextPayload cannot be blank")
    private String ciphertextPayload;

    @NotBlank(message = "iv cannot be blank")
    private String iv;

    private String ephemeralPublicKey;

    @NotNull(message = "messageType cannot be null")
    private MessageType messageType;

    private UUID replyToMessageId;

    private UUID mediaAttachmentId;

    public SendMessageRequest() {}

    public SendMessageRequest(String clientMessageId, String ciphertextPayload, String iv,
                              String ephemeralPublicKey, MessageType messageType,
                              UUID replyToMessageId, UUID mediaAttachmentId) {
        this.clientMessageId = clientMessageId;
        this.ciphertextPayload = ciphertextPayload;
        this.iv = iv;
        this.ephemeralPublicKey = ephemeralPublicKey;
        this.messageType = messageType;
        this.replyToMessageId = replyToMessageId;
        this.mediaAttachmentId = mediaAttachmentId;
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
}
