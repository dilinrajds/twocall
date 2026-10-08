package com.twocall.chat;

import com.twocall.chat.domain.entity.Device;
import com.twocall.chat.domain.entity.Pair;
import com.twocall.chat.dto.request.UpdateProfileRequest;
import com.twocall.chat.exception.ResourceNotFoundException;
import com.twocall.chat.repository.DeviceRepository;
import com.twocall.chat.service.ProfileService;
import com.twocall.chat.service.WsSessionManager;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileServiceTest {
    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final WsSessionManager sessions = mock(WsSessionManager.class);
    private final ProfileService service = new ProfileService(devices, sessions);

    @Test void returnsOnlyAuthenticatedPairProfiles() {
        Pair pair = new Pair();
        Device mine = new Device(UUID.randomUUID(), pair, "a", "key", "Alice");
        Device partner = new Device(UUID.randomUUID(), pair, "b", "key", "Bob");
        partner.setProfileImage("photo");
        when(devices.findAllByPairId(pair.getId())).thenReturn(List.of(mine, partner));
        var result = service.get(pair.getId(), mine.getId());
        assertEquals("Alice", result.get("mine").get("name"));
        assertEquals("Bob", result.get("partner").get("name"));
        assertEquals("photo", result.get("partner").get("imageBase64"));
        assertThrows(ResourceNotFoundException.class, () -> service.get(pair.getId(), UUID.randomUUID()));
    }

    @Test void updatePersistsAndNotifiesOnlyAfterCommit() {
        Pair pair = new Pair();
        Device mine = new Device(UUID.randomUUID(), pair, "a", "key", "Before");
        mine.setProfileImage("old");
        when(devices.findAllByPairId(pair.getId())).thenReturn(List.of(mine));
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.update(pair.getId(), mine.getId(), new UpdateProfileRequest(" Alice ", null));
            assertEquals("Alice", mine.getDeviceLabel());
            assertNull(mine.getProfileImage());
            verify(devices).save(mine);
            verifyNoInteractions(sessions);
            TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
            verify(sessions).sendToPartner(eq(pair.getId()), eq(mine.getId()), any());
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }

    @Test void rejectsAnotherDevicesProfileUpdate() {
        Pair pair = new Pair();
        Device mine = new Device(UUID.randomUUID(), pair, "a", "key", "Alice");
        when(devices.findAllByPairId(pair.getId())).thenReturn(List.of(mine));
        assertThrows(ResourceNotFoundException.class, () -> service.update(pair.getId(), UUID.randomUUID(),
                new UpdateProfileRequest("Intruder", null)));
        verify(devices, never()).save(any());
    }

    @Test void rejectsNonJpegImage() {
        Pair pair = new Pair();
        Device mine = new Device(UUID.randomUUID(), pair, "a", "key", "Alice");
        when(devices.findAllByPairId(pair.getId())).thenReturn(List.of(mine));
        assertThrows(IllegalArgumentException.class, () -> service.update(pair.getId(), mine.getId(),
                new UpdateProfileRequest("Alice", "aGVsbG8=")));
        verify(devices, never()).save(any());
    }
}
