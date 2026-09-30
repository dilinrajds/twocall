package com.twocall.chat.dto.response;

import java.util.UUID;

public class AttachmentUploadResponse {
    private UUID attachmentId;
    private String fileName;
    private String contentType;
    private long fileSizeBytes;
    private String sha256Hash;

    public AttachmentUploadResponse() {}

    public AttachmentUploadResponse(UUID attachmentId, String fileName, String contentType, long fileSizeBytes, String sha256Hash) {
        this.attachmentId = attachmentId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.sha256Hash = sha256Hash;
    }

    public UUID getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(UUID attachmentId) {
        this.attachmentId = attachmentId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }
}
