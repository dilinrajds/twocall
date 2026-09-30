package com.twocall.chat.domain.enums;

public enum WsEventType {
    MESSAGE_SENT,
    MESSAGE_DELIVERED,
    MESSAGE_READ,
    MESSAGE_REACTION,
    MESSAGE_DELETED,
    TYPING_START,
    TYPING_STOP,
    PRESENCE,
    CALL_OFFER,
    CALL_ANSWER,
    ICE_CANDIDATE,
    CALL_END,
    CALL_REJECT
}
