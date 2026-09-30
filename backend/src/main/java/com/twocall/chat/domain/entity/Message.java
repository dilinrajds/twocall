package com.twocall.chat.domain.entity;

import com.twocall.chat.domain.enums.MessageStatus;
import com.twocall.chat.domain.enums.MessageType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "messages", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"pair_id", "client_message_id"}, name = "uq_message_client_id_pair")
})
public class Message {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pair_id", nullable = false)
    private Pair pair;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_device_id", nullable = false)
    private Device senderDevice;

    @Column(name = "client_message_id", nullable = false, length = 64)
    private String clientMessageId;

    @Column(name = "ciphertext_payload", nullable = false, columnDefinition = "TEXT")
    private String ciphertextPayload;

    @Column(name = "iv", nullable = false, length = 64)
    private String iv;

    @Column(name = "ephemeral_public_key", columnDefinition = "TEXT")
    private String ephemeralPublicKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 32)
    private MessageType messageType;

    @Column(name = "reply_to_message_id")
    private UUID replyToMessageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_attachment_id")
    private Attachment mediaAttachment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status = MessageStatus.SENT;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MessageReaction> reactions = new ArrayList<>();

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DeliveryReceipt> deliveryReceipts = new ArrayList<>();

    public Message() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    public Message(UUID id, Pair pair, Device senderDevice, String clientMessageId,
                   String ciphertextPayload, String iv, String ephemeralPublicKey,
                   MessageType messageType, UUID replyToMessageId, Attachment mediaAttachment) {
        this.id = id != null ? id : UUID.randomUUID();
        this.pair = pair;
        this.senderDevice = senderDevice;
        this.clientMessageId = clientMessageId;
        this.ciphertextPayload = ciphertextPayload;
        this.iv = iv;
        this.ephemeralPublicKey = ephemeralPublicKey;
        this.messageType = messageType;
        this.replyToMessageId = replyToMessageId;
        this.mediaAttachment = mediaAttachment;
        this.status = MessageStatus.SENT;
        this.isDeleted = false;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Pair getPair() {
        return pair;
    }

    public void setPair(Pair pair) {
        this.pair = pair;
    }

    public Device getSenderDevice() {
        return senderDevice;
    }

    public void setSenderDevice(Device senderDevice) {
        this.senderDevice = senderDevice;
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

    public Attachment getMediaAttachment() {
        return mediaAttachment;
    }

    public void setMediaAttachment(Attachment mediaAttachment) {
        this.mediaAttachment = mediaAttachment;
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

    public List<MessageReaction> getReactions() {
        return reactions;
    }

    public void setReactions(List<MessageReaction> reactions) {
        this.reactions = reactions;
    }

    public List<DeliveryReceipt> getDeliveryReceipts() {
        return deliveryReceipts;
    }

    public void setDeliveryReceipts(List<DeliveryReceipt> deliveryReceipts) {
        this.deliveryReceipts = deliveryReceipts;
    }
}
