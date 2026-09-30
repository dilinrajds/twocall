package com.twocall.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ReceiptUpdateRequest {

    @NotNull(message = "messageId cannot be null")
    private UUID messageId;

    @NotBlank(message = "status cannot be blank")
    private String status; // DELIVERED, READ

    public ReceiptUpdateRequest() {}

    public ReceiptUpdateRequest(UUID messageId, String status) {
        this.messageId = messageId;
        this.status = status;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
