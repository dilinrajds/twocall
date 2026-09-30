package com.twocall.chat.dto.ws;

import java.util.UUID;

public class WsSignalingPayload {
    private UUID callId;
    private String callType; // AUDIO, VIDEO
    private String sdp;
    private String sdpType; // offer, answer
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;
    private String reason; // for CALL_REJECT / CALL_END

    public WsSignalingPayload() {}

    public static WsSignalingPayload offer(UUID callId, String callType, String sdp) {
        WsSignalingPayload p = new WsSignalingPayload();
        p.callId = callId;
        p.callType = callType;
        p.sdp = sdp;
        p.sdpType = "offer";
        return p;
    }

    public static WsSignalingPayload answer(UUID callId, String sdp) {
        WsSignalingPayload p = new WsSignalingPayload();
        p.callId = callId;
        p.sdp = sdp;
        p.sdpType = "answer";
        return p;
    }

    public static WsSignalingPayload iceCandidate(UUID callId, String candidate, String sdpMid, int sdpMLineIndex) {
        WsSignalingPayload p = new WsSignalingPayload();
        p.callId = callId;
        p.candidate = candidate;
        p.sdpMid = sdpMid;
        p.sdpMLineIndex = sdpMLineIndex;
        return p;
    }

    public static WsSignalingPayload end(UUID callId, String reason) {
        WsSignalingPayload p = new WsSignalingPayload();
        p.callId = callId;
        p.reason = reason;
        return p;
    }

    public UUID getCallId() {
        return callId;
    }

    public void setCallId(UUID callId) {
        this.callId = callId;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getSdp() {
        return sdp;
    }

    public void setSdp(String sdp) {
        this.sdp = sdp;
    }

    public String getSdpType() {
        return sdpType;
    }

    public void setSdpType(String sdpType) {
        this.sdpType = sdpType;
    }

    public String getCandidate() {
        return candidate;
    }

    public void setCandidate(String candidate) {
        this.candidate = candidate;
    }

    public String getSdpMid() {
        return sdpMid;
    }

    public void setSdpMid(String sdpMid) {
        this.sdpMid = sdpMid;
    }

    public Integer getSdpMLineIndex() {
        return sdpMLineIndex;
    }

    public void setSdpMLineIndex(Integer sdpMLineIndex) {
        this.sdpMLineIndex = sdpMLineIndex;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
