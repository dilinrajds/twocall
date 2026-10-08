package com.twocall.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twocall.chat.crypto.KeyStoreManager
import com.twocall.chat.data.local.entity.CallLogEntity
import com.twocall.chat.data.repository.ChatRepository
import com.twocall.chat.webrtc.CallState
import com.twocall.chat.webrtc.WebRtcManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer

class CallViewModel(
    val webRtcManager: WebRtcManager,
    private val chatRepository: ChatRepository,
    private val keyStoreManager: KeyStoreManager
) : ViewModel() {

    val callState = webRtcManager.callState
    val currentSession = webRtcManager.currentSession
    val isMicMuted = webRtcManager.isMicMuted
    val isSpeakerOn = webRtcManager.isSpeakerOn
    val isVideoEnabled = webRtcManager.isVideoEnabled
    val hasRemoteVideoFrame = webRtcManager.hasRemoteVideoFrame

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val partnerProfile = currentSession.flatMapLatest { session ->
        chatRepository.getConversationFlowForPair(session?.pairId ?: keyStoreManager.getActivePairId().orEmpty())
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    private val _autoAccept = MutableStateFlow(false)
    val autoAccept = _autoAccept.asStateFlow()
    fun requestAutoAccept() { _autoAccept.value = true }

    private val _callDurationSeconds = MutableStateFlow(0)
    val callDurationSeconds = _callDurationSeconds.asStateFlow()

    /** Emitted when the REMOTE SIDE ends the call or connection fails. UI should navigate to chat immediately. */
    private val _remoteCallEndedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val remoteCallEndedEvent = _remoteCallEndedEvent.asSharedFlow()

    private var timerJob: Job? = null

    // Call logging tracking state
    private var callStartedAt: Long = 0L
    private var callAnsweredAt: Long? = null
    private var lastLoggedCallId: String? = null
    private var trackedPairId: String? = null
    private var trackedPartnerDeviceId: String? = null
    private var trackedCallType: String = "AUDIO"
    private var trackedDirection: String = "OUTGOING"

    init {
        // Track session metadata when call starts
        viewModelScope.launch {
            currentSession.collect { session ->
                if (session != null) {
                    val callId = session.callId
                    if (callId != lastLoggedCallId) {
                        val pId = session.pairId ?: keyStoreManager.getActivePairId() ?: ""
                        trackedPairId = pId
                        trackedPartnerDeviceId = session.partnerDeviceId ?: keyStoreManager.getPartnerDeviceId(pId)
                        trackedCallType = if (session.isVideo) "VIDEO" else "AUDIO"
                        trackedDirection = if (session.isOutgoing) "OUTGOING" else "INCOMING"
                        callStartedAt = System.currentTimeMillis()
                        callAnsweredAt = null
                    }
                }
            }
        }

        // Track call state transitions
        viewModelScope.launch {
            callState.collect { state ->
                when (state) {
                    CallState.CONNECTED -> {
                        if (callAnsweredAt == null) {
                            callAnsweredAt = System.currentTimeMillis()
                        }
                        startTimer()
                    }
                    CallState.ENDED, CallState.DECLINED, CallState.FAILED -> {
                        stopTimer()
                        recordCallLog(state)
                    }
                    CallState.IDLE -> {
                        stopTimer()
                    }
                    else -> {}
                }
            }
        }

        // Wire WebRtcManager callback -> ViewModel event so UI can react immediately
        webRtcManager.onCallEndedRemotely = {
            _remoteCallEndedEvent.tryEmit(Unit)
        }
    }

    private fun recordCallLog(finalState: CallState) {
        val session = currentSession.value
        val callId = session?.callId
        if (callId.isNullOrBlank() || callId == lastLoggedCallId) return
        lastLoggedCallId = callId

        val pairId = session.pairId ?: trackedPairId ?: keyStoreManager.getActivePairId() ?: ""
        val partnerDeviceId = session.partnerDeviceId ?: trackedPartnerDeviceId ?: keyStoreManager.getPartnerDeviceId(pairId)
        val callType = if (session.isVideo) "VIDEO" else trackedCallType
        val direction = if (session.isOutgoing) "OUTGOING" else trackedDirection
        val endedAt = System.currentTimeMillis()

        // Duration is strictly from answeredAt to endedAt; 0 if never answered
        val durationSeconds = if (callAnsweredAt != null) {
            ((endedAt - callAnsweredAt!!) / 1000).coerceAtLeast(0L)
        } else {
            0L
        }

        val status = when {
            callAnsweredAt != null -> "ANSWERED"
            finalState == CallState.DECLINED -> "DECLINED"
            finalState == CallState.FAILED -> "FAILED"
            direction == "OUTGOING" -> "CANCELLED"
            else -> "MISSED"
        }

        val entity = CallLogEntity(
            callId = callId,
            pairId = pairId,
            partnerDeviceId = partnerDeviceId,
            callType = callType,
            direction = direction,
            status = status,
            startedAt = if (callStartedAt > 0L) callStartedAt else endedAt,
            answeredAt = callAnsweredAt,
            endedAt = endedAt,
            durationSeconds = durationSeconds
        )

        viewModelScope.launch {
            chatRepository.saveCallLog(entity)
            if (pairId.isNotBlank()) {
                val icon = if (callType == "VIDEO") "📹 Video call" else "📞 Voice call"
                val desc = if (status == "ANSWERED") formatDurationShort(durationSeconds) else status.lowercase().replaceFirstChar { it.uppercase() }
                val preview = "$icon • $desc"
                chatRepository.updateLastMessagePreview(pairId, preview, endedAt)
            }
        }
    }

    private fun formatDurationShort(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return when {
            mins > 0 && secs > 0 -> "${mins}m ${secs}s"
            mins > 0 -> "${mins}m"
            else -> "${secs}s"
        }
    }

    fun startAudioCall() {
        webRtcManager.startOutgoingCall(isVideo = false)
    }

    fun startVideoCall(localRenderer: SurfaceViewRenderer? = null, remoteRenderer: SurfaceViewRenderer? = null) {
        webRtcManager.startOutgoingCall(isVideo = true, localRenderer = localRenderer, remoteRenderer = remoteRenderer)
    }

    fun acceptCall(localRenderer: SurfaceViewRenderer? = null, remoteRenderer: SurfaceViewRenderer? = null) {
        _autoAccept.value = false
        webRtcManager.acceptIncomingCall(localRenderer, remoteRenderer)
    }

    fun rejectCall() {
        webRtcManager.rejectIncomingCall()
    }

    fun endCall() {
        webRtcManager.endCall()
    }

    fun toggleMic() = webRtcManager.toggleMic()
    fun toggleSpeaker() = webRtcManager.toggleSpeaker()
    fun toggleVideo() = webRtcManager.toggleVideo()
    fun switchCamera() = webRtcManager.switchCamera()

    private fun startTimer() {
        timerJob?.cancel()
        _callDurationSeconds.value = 0
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        _callDurationSeconds.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        webRtcManager.onCallEndedRemotely = null
    }
}
