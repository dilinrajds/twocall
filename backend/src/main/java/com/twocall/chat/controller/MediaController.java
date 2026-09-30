package com.twocall.chat.controller;

import com.twocall.chat.domain.entity.Attachment;
import com.twocall.chat.dto.response.AttachmentUploadResponse;
import com.twocall.chat.security.DevicePrincipal;
import com.twocall.chat.security.PairAccessValidator;
import com.twocall.chat.service.MediaStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaStorageService mediaStorageService;
    private final PairAccessValidator accessValidator;

    public MediaController(MediaStorageService mediaStorageService, PairAccessValidator accessValidator) {
        this.mediaStorageService = mediaStorageService;
        this.accessValidator = accessValidator;
    }

    @PostMapping("/upload")
    public ResponseEntity<AttachmentUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "encryptedKey", required = false) String encryptedKey) throws IOException {

        DevicePrincipal principal = accessValidator.getAuthenticatedPrincipal();
        AttachmentUploadResponse response = mediaStorageService.storeFile(
                principal.getPairId(),
                principal.getDeviceId(),
                file,
                encryptedKey
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID attachmentId) {
        DevicePrincipal principal = accessValidator.getAuthenticatedPrincipal();
        Attachment metadata = mediaStorageService.getAttachmentMetadata(principal.getPairId(), attachmentId);
        Resource file = mediaStorageService.loadFileAsResource(principal.getPairId(), attachmentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.getFileName() + "\"")
                .body(file);
    }
}
