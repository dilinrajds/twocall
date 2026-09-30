package com.twocall.chat.dto.ws;

import com.twocall.chat.domain.enums.WsEventType;
import java.time.Instant;
import java.util.UUID;

public class WsEvent<T> {
    private WsEventType eventType;
    private UUID pairId;
    private UUID senderDeviceId;
    private UUID recipientDeviceId;
    private Instant timestamp;
    private T payload;

    public WsEvent() {
        this.timestamp = Instant.now();
    }

    public WsEvent(WsEventType eventType, UUID pairId, UUID senderDeviceId, UUID recipientDeviceId, T payload) {
        this.eventType = eventType;
        this.pairId = pairId;
        this.senderDeviceId = senderDeviceId;
        this.recipientDeviceId = recipientDeviceId;
        this.timestamp = Instant.now();
        this.payload = payload;
    }

    public WsEventType getEventType() {
        return eventType;
    }

    public void setEventType(WsEventType eventType) {
        this.eventType = eventType;
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

    public UUID getRecipientDeviceId() {
        return recipientDeviceId;
    }

    public void setRecipientDeviceId(UUID recipientDeviceId) {
        this.recipientDeviceId = recipientDeviceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
