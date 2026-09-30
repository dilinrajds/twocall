package com.twocall.chat.dto.ws;

import java.time.Instant;

public class WsPresencePayload {
    private boolean online;
    private Instant lastActiveAt;

    public WsPresencePayload() {}

    public WsPresencePayload(boolean online, Instant lastActiveAt) {
        this.online = online;
        this.lastActiveAt = lastActiveAt;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
}
