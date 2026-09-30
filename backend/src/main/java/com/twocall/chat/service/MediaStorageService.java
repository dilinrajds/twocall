package com.twocall.chat.service;

import com.twocall.chat.domain.entity.Attachment;
import com.twocall.chat.domain.entity.Device;
import com.twocall.chat.domain.entity.Pair;
import com.twocall.chat.dto.response.AttachmentUploadResponse;
import com.twocall.chat.exception.ResourceNotFoundException;
import com.twocall.chat.repository.AttachmentRepository;
import com.twocall.chat.repository.DeviceRepository;
import com.twocall.chat.repository.PairRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class MediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(MediaStorageService.class);

    private final AttachmentRepository attachmentRepository;
    private final PairRepository pairRepository;
    private final DeviceRepository deviceRepository;
    private final Path rootStorageLocation;
    private final long maxFileSizeBytes;

    public MediaStorageService(
            AttachmentRepository attachmentRepository,
            PairRepository pairRepository,
            DeviceRepository deviceRepository,
            @Value("${app.media.storage-path:./uploads/media}") String storagePath,
            @Value("${app.media.max-file-size-bytes:52428800}") long maxFileSizeBytes) {
        this.attachmentRepository = attachmentRepository;
        this.pairRepository = pairRepository;
        this.deviceRepository = deviceRepository;
        this.rootStorageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;

        try {
            Files.createDirectories(this.rootStorageLocation);
        } catch (IOException e) {
            log.error("Could not create media storage directory at {}", rootStorageLocation, e);
        }
    }

    @Transactional
    public AttachmentUploadResponse storeFile(UUID pairId, UUID uploaderDeviceId,
                                              MultipartFile file, String encryptedKeyMaterial) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("File size exceeds limit of " + (maxFileSizeBytes / 1024 / 1024) + "MB");
        }

        Pair pair = pairRepository.findById(pairId)
                .orElseThrow(() -> new ResourceNotFoundException("Pair not found"));
        Device device = deviceRepository.findById(uploaderDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        // Isolated pair subfolder
        Path pairDirectory = this.rootStorageLocation.resolve(pairId.toString()).normalize();
        Files.createDirectories(pairDirectory);

        UUID attachmentId = UUID.randomUUID();
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment";
        String cleanFileName = attachmentId + "_" + Paths.get(originalFilename).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        Path destination = pairDirectory.resolve(cleanFileName).normalize();

        // Prevent directory traversal
        if (!destination.startsWith(pairDirectory)) {
            throw new SecurityException("Cannot store file outside target directory");
        }

        // Calculate SHA-256 during copy
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }

        try (InputStream in = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }

        // Store file
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        String sha256 = HexFormat.of().formatHex(digest.digest());

        Attachment attachment = new Attachment(
                attachmentId,
                pair,
                device,
                originalFilename,
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                file.getSize(),
                destination.toString(),
                sha256,
                encryptedKeyMaterial
        );

        attachment = attachmentRepository.save(attachment);
        log.info("Media stored: id={} size={} sha256={}", attachmentId, file.getSize(), sha256);

        return new AttachmentUploadResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getFileSizeBytes(),
                attachment.getSha256Hash()
        );
    }

    @Transactional(readOnly = true)
    public Resource loadFileAsResource(UUID pairId, UUID attachmentId) {
        Attachment attachment = attachmentRepository.findByIdAndPairId(attachmentId, pairId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));

        Path filePath = Paths.get(attachment.getStoragePath());
        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("File not found on disk");
        }
        return resource;
    }

    @Transactional(readOnly = true)
    public Attachment getAttachmentMetadata(UUID pairId, UUID attachmentId) {
        return attachmentRepository.findByIdAndPairId(attachmentId, pairId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
    }
}
