package com.twocall.chat.domain.entity;

import com.twocall.chat.domain.enums.CallStatus;
import com.twocall.chat.domain.enums.CallType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "call_sessions")
public class CallSession {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pair_id", nullable = false)
    private Pair pair;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caller_device_id", nullable = false)
    private Device callerDevice;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false, length = 10)
    private CallType callType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    public CallSession() {
        this.id = UUID.randomUUID();
        this.startedAt = Instant.now();
        this.status = CallStatus.RINGING;
    }

    public CallSession(UUID id, Pair pair, Device callerDevice, CallType callType) {
        this.id = id != null ? id : UUID.randomUUID();
        this.pair = pair;
        this.callerDevice = callerDevice;
        this.callType = callType;
        this.status = CallStatus.RINGING;
        this.startedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Pair getPair() {
        return pair;
    }

    public void setPair(Pair pair) {
        this.pair = pair;
    }

    public Device getCallerDevice() {
        return callerDevice;
    }

    public void setCallerDevice(Device callerDevice) {
        this.callerDevice = callerDevice;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(CallType callType) {
        this.callType = callType;
    }

    public CallStatus getStatus() {
        return status;
    }

    public void setStatus(CallStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
}
