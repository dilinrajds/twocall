package com.twocall.chat;

import com.twocall.chat.config.TurnConfig;
import com.twocall.chat.domain.entity.*;
import com.twocall.chat.domain.enums.*;
import com.twocall.chat.exception.ResourceNotFoundException;
import com.twocall.chat.repository.*;
import com.twocall.chat.service.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CallRecoveryTest {
    private final CallSessionRepository calls = mock(CallSessionRepository.class);
    private final WebRtcSignalingService service = new WebRtcSignalingService(calls, mock(PairRepository.class),
            mock(DeviceRepository.class), mock(WsSessionManager.class), mock(PushNotificationService.class), mock(TurnConfig.class));

    @Test void recoversOfferAndCandidatesForReceiver() {
        Pair pair = new Pair();
        Device caller = new Device(UUID.randomUUID(), pair, "a", "key", "Alice");
        CallSession call = new CallSession(UUID.randomUUID(), pair, caller, CallType.VIDEO);
        call.setOfferSdp("v=0");
        call.setIceCandidates("[{\"candidate\":\"candidate:1\",\"sdpMid\":\"0\",\"sdpMLineIndex\":0}]");
        when(calls.findByIdAndPairId(call.getId(), pair.getId())).thenReturn(Optional.of(call));
        var recovered = service.recoverIncomingCall(pair.getId(), UUID.randomUUID(), call.getId());
        assertEquals("v=0", recovered.get("sdp"));
        assertEquals(CallType.VIDEO, recovered.get("callType"));
        assertEquals(1, ((com.fasterxml.jackson.databind.JsonNode) recovered.get("iceCandidates")).size());
        assertThrows(ResourceNotFoundException.class, () -> service.recoverIncomingCall(pair.getId(), caller.getId(), call.getId()));
    }

    @Test void rejectsEndedExpiredAndCrossPairCalls() {
        Pair pair = new Pair();
        Device caller = new Device(UUID.randomUUID(), pair, "a", "key", "Alice");
        CallSession call = new CallSession(UUID.randomUUID(), pair, caller, CallType.AUDIO);
        when(calls.findByIdAndPairId(call.getId(), pair.getId())).thenReturn(Optional.of(call));
        call.setStatus(CallStatus.ENDED);
        assertThrows(ResourceNotFoundException.class, () -> service.recoverIncomingCall(pair.getId(), UUID.randomUUID(), call.getId()));
        call.setStatus(CallStatus.RINGING);
        call.setStartedAt(java.time.Instant.now().minusSeconds(61));
        assertThrows(ResourceNotFoundException.class, () -> service.recoverIncomingCall(pair.getId(), UUID.randomUUID(), call.getId()));
        UUID otherPair = UUID.randomUUID();
        when(calls.findByIdAndPairId(call.getId(), otherPair)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.recoverIncomingCall(otherPair, UUID.randomUUID(), call.getId()));
    }
}
