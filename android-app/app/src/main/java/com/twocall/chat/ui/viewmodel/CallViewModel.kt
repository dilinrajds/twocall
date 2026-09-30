package com.twocall.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twocall.chat.webrtc.CallState
import com.twocall.chat.webrtc.WebRtcManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer

class CallViewModel(
    val webRtcManager: WebRtcManager
) : ViewModel() {

    val callState = webRtcManager.callState
    val currentSession = webRtcManager.currentSession
    val isMicMuted = webRtcManager.isMicMuted
    val isSpeakerOn = webRtcManager.isSpeakerOn
    val isVideoEnabled = webRtcManager.isVideoEnabled

    private val _callDurationSeconds = MutableStateFlow(0)
    val callDurationSeconds = _callDurationSeconds.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            callState.collect { state ->
                if (state == CallState.CONNECTED) {
                    startTimer()
                } else if (state == CallState.IDLE || state == CallState.ENDED) {
                    stopTimer()
                }
            }
        }
    }

    fun startAudioCall() {
        webRtcManager.startOutgoingCall(isVideo = false)
    }

    fun startVideoCall(localRenderer: SurfaceViewRenderer) {
        webRtcManager.startOutgoingCall(isVideo = true, localRenderer = localRenderer)
    }

    fun acceptCall(localRenderer: SurfaceViewRenderer? = null) {
        webRtcManager.acceptIncomingCall(localRenderer)
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
}
