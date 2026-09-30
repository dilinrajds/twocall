package com.twocall.chat.dto.ws;

public class WsTypingPayload {
    private boolean typing;

    public WsTypingPayload() {}

    public WsTypingPayload(boolean typing) {
        this.typing = typing;
    }

    public boolean isTyping() {
        return typing;
    }

    public void setTyping(boolean typing) {
        this.typing = typing;
    }
}
