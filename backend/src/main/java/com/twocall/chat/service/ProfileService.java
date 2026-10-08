package com.twocall.chat.service;

import com.twocall.chat.domain.entity.Device;
import com.twocall.chat.domain.enums.WsEventType;
import com.twocall.chat.dto.request.UpdateProfileRequest;
import com.twocall.chat.dto.ws.WsEvent;
import com.twocall.chat.exception.ResourceNotFoundException;
import com.twocall.chat.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;

@Service
public class ProfileService {
    private final DeviceRepository devices;
    private final WsSessionManager sessions;
    public ProfileService(DeviceRepository devices, WsSessionManager sessions) {
        this.devices = devices;
        this.sessions = sessions;
    }
    private Map<String, String> profile(Device device) {
        return Map.of("name", device.getDeviceLabel() == null ? "Partner" : device.getDeviceLabel(),
                "imageBase64", device.getProfileImage() == null ? "" : device.getProfileImage());
    }
    @Transactional(readOnly = true)
    public Map<String, Map<String, String>> get(UUID pairId, UUID deviceId) {
        var members = devices.findAllByPairId(pairId);
        Device mine = members.stream().filter(d -> d.getId().equals(deviceId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
        var partner = members.stream().filter(d -> !d.getId().equals(deviceId)).findFirst();
        return Map.of("mine", profile(mine), "partner", partner.map(this::profile)
                .orElse(Map.of("name", "Partner", "imageBase64", "")));
    }
    @Transactional
    public void update(UUID pairId, UUID deviceId, UpdateProfileRequest request) {
        Device mine = devices.findAllByPairId(pairId).stream().filter(d -> d.getId().equals(deviceId))
                .findFirst().orElseThrow(() -> new ResourceNotFoundException("Device not found"));
        if (request.imageBase64() != null && !request.imageBase64().isEmpty()) {
            byte[] bytes = java.util.Base64.getDecoder().decode(request.imageBase64());
            if (bytes.length < 3 || bytes[0] != (byte) 0xff || bytes[1] != (byte) 0xd8) {
                throw new IllegalArgumentException("Profile image must be a JPEG");
            }
        }
        mine.setDeviceLabel(request.name().trim());
        mine.setProfileImage(request.imageBase64());
        devices.save(mine);
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() {
                        sessions.sendToPartner(pairId, deviceId, new WsEvent<>(WsEventType.PRESENCE, pairId,
                                deviceId, null, Map.of("event", "PROFILE_UPDATED")));
                    }
                });
    }
}
