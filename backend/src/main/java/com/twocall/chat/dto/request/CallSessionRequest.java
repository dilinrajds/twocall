package com.twocall.chat.dto.request;

import com.twocall.chat.domain.enums.CallType;
import jakarta.validation.constraints.NotNull;

public class CallSessionRequest {

    @NotNull(message = "callType cannot be null")
    private CallType callType;

    public CallSessionRequest() {}

    public CallSessionRequest(CallType callType) {
        this.callType = callType;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(CallType callType) {
        this.callType = callType;
    }
}
