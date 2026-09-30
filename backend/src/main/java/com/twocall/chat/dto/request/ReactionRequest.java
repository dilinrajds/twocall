package com.twocall.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ReactionRequest {

    @NotNull(message = "messageId cannot be null")
    private UUID messageId;

    @NotBlank(message = "emoji cannot be blank")
    private String emoji;

    public ReactionRequest() {}

    public ReactionRequest(UUID messageId, String emoji) {
        this.messageId = messageId;
        this.emoji = emoji;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
}
