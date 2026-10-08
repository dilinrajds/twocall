package com.twocall.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class WsSessionManager {

    private static final Logger log = LoggerFactory.getLogger(WsSessionManager.class);

    private final ObjectMapper objectMapper;
    private final Map<UUID, WebSocketSession> deviceSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> pairDevices = new ConcurrentHashMap<>();

    public WsSessionManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void registerSession(UUID pairId, UUID deviceId, WebSocketSession session) {
        deviceSessions.put(deviceId, session);
        pairDevices.computeIfAbsent(pairId, k -> new CopyOnWriteArraySet<>()).add(deviceId);
        log.info("WebSocket connected: deviceId={} pairId={}", deviceId, pairId);
    }

    public void removeSession(UUID pairId, UUID deviceId) {
        deviceSessions.remove(deviceId);
        Set<UUID> devices = pairDevices.get(pairId);
        if (devices != null) {
            devices.remove(deviceId);
            if (devices.isEmpty()) {
                pairDevices.remove(pairId);
            }
        }
        log.info("WebSocket disconnected: deviceId={} pairId={}", deviceId, pairId);
    }

    public boolean isDeviceOnline(UUID deviceId) {
        WebSocketSession session = deviceSessions.get(deviceId);
        return session != null && session.isOpen();
    }

    public boolean sendToDevice(UUID deviceId, Object payload) {
        WebSocketSession session = deviceSessions.get(deviceId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(payload);
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
                return true;
            } catch (IOException e) {
                log.error("Failed to send WebSocket message to device {}", deviceId, e);
            }
        }
        return false;
    }

    public boolean sendToPartner(UUID pairId, UUID senderDeviceId, Object payload) {
        Set<UUID> devices = pairDevices.get(pairId);
        if (devices != null) {
            for (UUID targetId : devices) {
                if (!targetId.equals(senderDeviceId)) {
                    return sendToDevice(targetId, payload);
                }
            }
        }
        return false;
    }

    public UUID getPartnerDeviceId(UUID pairId, UUID myDeviceId) {
        Set<UUID> devices = pairDevices.get(pairId);
        if (devices != null) {
            for (UUID id : devices) {
                if (!id.equals(myDeviceId)) {
                    return id;
                }
            }
        }
        return null;
    }

    public void closeDeviceSession(UUID deviceId) {
        WebSocketSession session = deviceSessions.remove(deviceId);
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException ignored) {}
        }
        // Clean up pairDevices map to prevent phantom partner entries
        pairDevices.forEach((pairId, devices) -> {
            devices.remove(deviceId);
            if (devices.isEmpty()) {
                pairDevices.remove(pairId);
            }
        });
    }
}

