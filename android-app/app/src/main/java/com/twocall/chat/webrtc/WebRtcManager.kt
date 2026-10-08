package com.twocall.chat.webrtc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.twocall.chat.data.remote.api.ChatApiService
import com.twocall.chat.data.remote.dto.WsEventDto
import com.twocall.chat.data.remote.ws.WebSocketClient
import com.twocall.chat.fcm.ChatFirebaseMessagingService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*
import java.util.UUID

enum class CallState {
    IDLE,
    OUTGOING_CALLING,
    INCOMING_RINGING,
    ACCEPTING,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DECLINED,
    ENDED,
    FAILED
}

data class CallSessionData(
    val callId: String,
    val isVideo: Boolean,
    val isOutgoing: Boolean,
    val partnerDeviceId: String? = null,
    val pairId: String? = null
)

data class PendingIncomingCall(
    val callId: String,
    val isVideo: Boolean,
    val sdpOffer: String,
    val callerDeviceId: String? = null,
    val pairId: String? = null
)

class WebRtcManager(
    private val context: Context,
    private val apiService: ChatApiService,
    private val webSocketClient: WebSocketClient,
    private val keyStoreManager: com.twocall.chat.crypto.KeyStoreManager,
    private val gson: Gson = Gson()
) {

    /** Invoked when remote side ends the call (or connection fails). UI should navigate back to chat. */
    var onCallEndedRemotely: (() -> Unit)? = null

    private val tag = "WebRtcManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var rootEglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null
    // Track which SurfaceViewRenderer instances have been init()'d so we never double-init.
    private var initializedLocalRenderer: SurfaceViewRenderer? = null
    private var initializedRemoteRenderer: SurfaceViewRenderer? = null
    @Volatile
    private var isRenderersInitialized = false

    private var pendingIncomingCall: PendingIncomingCall? = null
    private val queuedRemoteIceCandidates = mutableListOf<IceCandidate>()

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState = _callState.asStateFlow()

    private val _currentSession = MutableStateFlow<CallSessionData?>(null)
    val currentSession = _currentSession.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted = _isMicMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn = _isSpeakerOn.asStateFlow()

    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled = _isVideoEnabled.asStateFlow()

    private val _hasRemoteVideoFrame = MutableStateFlow(false)
    val hasRemoteVideoFrame = _hasRemoteVideoFrame.asStateFlow()

    init {
        initializePeerConnectionFactory()
        observeWebSocketSignaling()
    }

    private fun initializePeerConnectionFactory() {
        rootEglBase = EglBase.create()

        val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)

        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(
            rootEglBase?.eglBaseContext, true, true
        )
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(rootEglBase?.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    fun initSurfaceViews(localRenderer: SurfaceViewRenderer, remoteRenderer: SurfaceViewRenderer) {
        this.localRenderer = localRenderer
        this.remoteRenderer = remoteRenderer
        val eglContext = rootEglBase?.eglBaseContext ?: return

        // BUG 2 FIX: Guard against double-init of the same renderer instance.
        // LaunchedEffect can fire multiple times (callState changes after renderers set);
        // calling init() twice on the same SurfaceViewRenderer throws IllegalStateException.
        if (localRenderer !== initializedLocalRenderer) {
            try {
                localRenderer.init(eglContext, null)
                localRenderer.setEnableHardwareScaler(true)
                localRenderer.setMirror(true)
                localRenderer.setZOrderMediaOverlay(true)
                localRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                initializedLocalRenderer = localRenderer
                Log.i(tag, "Local SurfaceViewRenderer initialized with shared EGL context")
            } catch (e: Exception) {
                Log.w(tag, "localRenderer init exception: ${e.message}")
            }
        } else {
            Log.d(tag, "Local renderer already initialized, skipping re-init")
        }

        if (remoteRenderer !== initializedRemoteRenderer) {
            try {
                remoteRenderer.init(eglContext, object : RendererCommon.RendererEvents {
                    override fun onFirstFrameRendered() {
                        Log.i(tag, "First remote video frame rendered!")
                        _hasRemoteVideoFrame.value = true
                    }
                    override fun onFrameResolutionChanged(videoWidth: Int, videoHeight: Int, rotation: Int) {
                        Log.i(tag, "Remote video resolution changed: ${videoWidth}x${videoHeight}, rotation=$rotation")
                    }
                })
                remoteRenderer.setEnableHardwareScaler(true)
                remoteRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                initializedRemoteRenderer = remoteRenderer
                Log.i(tag, "Remote SurfaceViewRenderer initialized with shared EGL context")
            } catch (e: Exception) {
                Log.w(tag, "remoteRenderer init exception: ${e.message}")
            }
        } else {
            Log.d(tag, "Remote renderer already initialized, skipping re-init")
        }

        isRenderersInitialized = true
        attachLocalVideoTrackIfReady()
        attachRemoteVideoTrackIfReady()
    }

    private fun attachLocalVideoTrackIfReady() {
        if (!isRenderersInitialized) return
        val renderer = localRenderer ?: return
        val track = localVideoTrack ?: return
        try {
            track.removeSink(renderer)
            track.addSink(renderer)
            Log.i(tag, "Attached localVideoTrack to localRenderer")
        } catch (e: Exception) {
            Log.w(tag, "Error attaching local sink: ${e.message}")
        }
    }

    private fun attachRemoteVideoTrackIfReady() {
        if (!isRenderersInitialized) return
        val renderer = remoteRenderer ?: return
        val track = remoteVideoTrack ?: return
        try {
            track.removeSink(renderer)
            track.addSink(renderer)
            Log.i(tag, "Attached remoteVideoTrack to remoteRenderer")
        } catch (e: Exception) {
            Log.w(tag, "Error attaching remote sink: ${e.message}")
        }
    }

    private fun observeWebSocketSignaling() {
        scope.launch(Dispatchers.IO) {
            webSocketClient.incomingEvents.collect { event ->
                handleSignalingEvent(event)
            }
        }
    }

    private suspend fun handleSignalingEvent(event: WsEventDto<Any>) {
        when (event.eventType) {
            "CALL_OFFER" -> {
                val payloadJson = gson.toJson(event.payload)
                val payload = gson.fromJson(payloadJson, Map::class.java)
                val callId = payload["callId"] as? String ?: return
                val callType = payload["callType"] as? String ?: "AUDIO"
                val sdp = payload["sdp"] as? String ?: return
                val isVideo = "VIDEO".equals(callType, ignoreCase = true)
                if (_currentSession.value?.callId == callId && _callState.value != CallState.IDLE) return
                if (_callState.value !in listOf(CallState.IDLE, CallState.ENDED, CallState.FAILED, CallState.DECLINED)) return

                Log.i(tag, "CALL STATE: INCOMING_RINGING received (callId: $callId, isVideo: $isVideo)")
                _currentSession.value = CallSessionData(callId, isVideo, isOutgoing = false, partnerDeviceId = event.senderDeviceId, pairId = event.pairId)
                pendingIncomingCall = PendingIncomingCall(callId, isVideo, sdp, callerDeviceId = event.senderDeviceId, pairId = event.pairId)
                _hasRemoteVideoFrame.value = false
                _callState.value = CallState.INCOMING_RINGING

                // CRITICAL PRIVACY RULE:
                // DO NOT CREATE PEER CONNECTION YET.
                // DO NOT INITIALIZE MICROPHONE OR CAMERA YET.
                // Wait until the user taps ACCEPT.
            }

            "CALL_ANSWER" -> {
                Log.i(tag, "Received SDP answer from partner")
                val payloadJson = gson.toJson(event.payload)
                val payload = gson.fromJson(payloadJson, Map::class.java)
                val sdp = payload["sdp"] as? String ?: return

                _callState.value = CallState.CONNECTING
                Log.i(tag, "CALL STATE: OUTGOING_CALLING -> CONNECTING")

                val sessionDesc = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        Log.i(tag, "Remote description (answer) set successfully")
                        drainQueuedIceCandidates()
                        peerConnection?.transceivers?.forEach { transceiver ->
                            if (transceiver.mediaType == MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO) {
                                configureVideoBitrate(transceiver)
                            }
                        }
                    }
                    override fun onSetFailure(error: String?) {
                        Log.e(tag, "Failed to setRemoteDescription on answer: $error")
                        _callState.value = CallState.FAILED
                        endCallLocally()
                    }
                }, sessionDesc)
            }

            "ICE_CANDIDATE" -> {
                val payloadJson = gson.toJson(event.payload)
                val payload = gson.fromJson(payloadJson, Map::class.java)
                val candidateStr = payload["candidate"] as? String ?: return
                val sdpMid = payload["sdpMid"] as? String ?: ""
                val sdpMLineIndex = (payload["sdpMLineIndex"] as? Number)?.toInt() ?: 0

                val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidateStr)
                if (peerConnection != null && peerConnection?.remoteDescription != null) {
                    peerConnection?.addIceCandidate(iceCandidate)
                    Log.d(tag, "Added remote ICE candidate directly")
                } else {
                    synchronized(queuedRemoteIceCandidates) {
                        queuedRemoteIceCandidates.add(iceCandidate)
                    }
                    Log.d(tag, "Queued remote ICE candidate (pending remoteDescription)")
                }
            }

            "CALL_REJECT" -> {
                Log.i(tag, "Call was rejected/declined by partner")
                _callState.value = CallState.DECLINED
                scope.launch {
                    delay(1500)
                    endCallLocally()
                }
            }

            "CALL_END" -> {
                Log.i(tag, "Call ended by partner")
                endCallLocally(notifyRemote = true)
            }
        }
    }

    suspend fun recoverIncomingCall(callId: String, pairId: String): Boolean {
        if (_currentSession.value?.callId == callId && _callState.value == CallState.INCOMING_RINGING) return true
        val response = apiService.recoverIncomingCall(callId, pairId)
        val call = response.body()?.takeIf { response.isSuccessful && it.sdp.isNotBlank() } ?: return false
        if (_callState.value !in listOf(CallState.IDLE, CallState.ENDED, CallState.FAILED, CallState.DECLINED)) return false
        pendingIncomingCall = PendingIncomingCall(call.callId, call.callType == "VIDEO", call.sdp, call.callerDeviceId, call.pairId)
        synchronized(queuedRemoteIceCandidates) {
            for (candidate in call.iceCandidates) {
                if (queuedRemoteIceCandidates.none { it.sdp == candidate.candidate }) {
                    queuedRemoteIceCandidates.add(IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.candidate))
                }
            }
        }
        _currentSession.value = CallSessionData(call.callId, call.callType == "VIDEO", false, call.callerDeviceId, call.pairId)
        _hasRemoteVideoFrame.value = false
        _callState.value = CallState.INCOMING_RINGING
        return true
    }

    private fun drainQueuedIceCandidates() {
        synchronized(queuedRemoteIceCandidates) {
            Log.i(tag, "Draining ${queuedRemoteIceCandidates.size} queued ICE candidates")
            for (candidate in queuedRemoteIceCandidates) {
                peerConnection?.addIceCandidate(candidate)
            }
            queuedRemoteIceCandidates.clear()
        }
    }

    fun startOutgoingCall(isVideo: Boolean, localRenderer: SurfaceViewRenderer? = null, remoteRenderer: SurfaceViewRenderer? = null) {
        if (localRenderer != null) this.localRenderer = localRenderer
        if (remoteRenderer != null) this.remoteRenderer = remoteRenderer

        val callId = UUID.randomUUID().toString()
        val pairId = keyStoreManager.getActivePairId()
        val partnerDeviceId = keyStoreManager.getPartnerDeviceId()
        _currentSession.value = CallSessionData(callId, isVideo, isOutgoing = true, partnerDeviceId = partnerDeviceId, pairId = pairId)
        _callState.value = CallState.OUTGOING_CALLING
        _hasRemoteVideoFrame.value = false
        Log.i(tag, "CALL STATE: IDLE -> OUTGOING_CALLING (callId: $callId, isVideo: $isVideo, pairId: $pairId)")

        scope.launch(Dispatchers.IO) {
            createPeerConnection(callId, isVideo, localRenderer)

            peerConnection?.transceivers?.forEach { transceiver ->
                transceiver.direction = RtpTransceiver.RtpTransceiverDirection.SEND_RECV
                if (transceiver.mediaType == MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO) {
                    configureVideoBitrate(transceiver)
                }
            }

            val sdpMediaConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                if (isVideo) {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                }
            }

            Log.i(tag, "Creating SDP offer...")
            peerConnection?.createOffer(object : SimpleSdpObserver() {
                override fun onCreateSuccess(desc: SessionDescription?) {
                    desc?.let { sdp ->
                        Log.i(tag, "Setting local description on offer...")
                        peerConnection?.setLocalDescription(object : SimpleSdpObserver() {
                            override fun onSetSuccess() {
                                val payload = mapOf(
                                    "callId" to callId,
                                    "callType" to (if (isVideo) "VIDEO" else "AUDIO"),
                                    "sdp" to sdp.description
                                )
                                webSocketClient.sendEvent("CALL_OFFER", payload, targetPairId = pairId)
                                Log.i(tag, "Local offer set and CALL_OFFER sent for callId: $callId")
                            }
                            override fun onSetFailure(error: String?) {
                                Log.e(tag, "Failed to setLocalDescription on offer: $error")
                                _callState.value = CallState.FAILED
                                endCallLocally()
                            }
                        }, sdp)
                    }
                }
                override fun onCreateFailure(error: String?) {
                    Log.e(tag, "Failed to createOffer: $error")
                    _callState.value = CallState.FAILED
                    endCallLocally()
                }
            }, sdpMediaConstraints)
        }
    }

    fun acceptIncomingCall(localRenderer: SurfaceViewRenderer? = null, remoteRenderer: SurfaceViewRenderer? = null) {
        if (localRenderer != null) this.localRenderer = localRenderer
        if (remoteRenderer != null) this.remoteRenderer = remoteRenderer

        val pending = pendingIncomingCall
        if (pending == null) {
            Log.w(tag, "acceptIncomingCall called but pendingIncomingCall is null")
            return
        }

        ChatFirebaseMessagingService.cancelIncomingCallNotification(context)

        Log.i(tag, "CALL ACCEPTED by receiver (callId: ${pending.callId})")
        _callState.value = CallState.ACCEPTING
        _hasRemoteVideoFrame.value = false

        // BUG 2 FIX: Initialize renderers NOW (synchronously on Main thread) before
        // createPeerConnection runs on IO. This ensures isRenderersInitialized=true and
        // the EGL context is ready when setupLocalTracks calls attachLocalVideoTrackIfReady.
        // Without this, the LaunchedEffect races against the IO coroutine.
        val localRend = this.localRenderer
        val remoteRend = this.remoteRenderer
        if (localRend != null && remoteRend != null) {
            initSurfaceViews(localRend, remoteRend)
        }

        scope.launch(Dispatchers.IO) {
            _callState.value = CallState.CONNECTING
            Log.i(tag, "CALL STATE: ACCEPTING -> CONNECTING")

            // 1. Create PeerConnection & initialize receiver local tracks
            createPeerConnection(pending.callId, pending.isVideo, localRenderer)

            // 2. Set remote description (caller's offer)
            val sessionDesc = SessionDescription(SessionDescription.Type.OFFER, pending.sdpOffer)
            Log.i(tag, "Setting remote description (offer)...")
            peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
                override fun onSetSuccess() {
                    Log.i(tag, "Remote description (offer) set successfully")
                    drainQueuedIceCandidates()

                    // 3. Create SDP answer
                    val sdpMediaConstraints = MediaConstraints().apply {
                        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                        if (pending.isVideo) {
                            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                        }
                    }

                    Log.i(tag, "Creating SDP answer...")
                    peerConnection?.createAnswer(object : SimpleSdpObserver() {
                        override fun onCreateSuccess(desc: SessionDescription?) {
                            desc?.let { sdp ->
                                Log.i(tag, "Setting local description (answer)...")
                                peerConnection?.setLocalDescription(object : SimpleSdpObserver() {
                                    override fun onSetSuccess() {
                                        val payload = mapOf(
                                            "callId" to pending.callId,
                                            "sdp" to sdp.description
                                        )
                                        webSocketClient.sendEvent("CALL_ANSWER", payload, targetPairId = pending.pairId)
                                        Log.i(tag, "Local answer set and CALL_ANSWER sent for callId: ${pending.callId}")
                                        pendingIncomingCall = null

                                        peerConnection?.transceivers?.forEach { transceiver ->
                                            transceiver.direction = RtpTransceiver.RtpTransceiverDirection.SEND_RECV
                                            if (transceiver.mediaType == MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO) {
                                                configureVideoBitrate(transceiver)
                                            }
                                        }
                                    }
                                    override fun onSetFailure(error: String?) {
                                        Log.e(tag, "Failed to setLocalDescription on answer: $error")
                                        _callState.value = CallState.FAILED
                                        endCallLocally()
                                    }
                                }, sdp)
                            }
                        }
                        override fun onCreateFailure(error: String?) {
                            Log.e(tag, "Failed to createAnswer: $error")
                            _callState.value = CallState.FAILED
                            endCallLocally()
                        }
                    }, sdpMediaConstraints)
                }
                override fun onSetFailure(error: String?) {
                    Log.e(tag, "Failed to setRemoteDescription on offer: $error")
                    _callState.value = CallState.FAILED
                    endCallLocally()
                }
            }, sessionDesc)
        }
    }


    fun rejectIncomingCall() {
        ChatFirebaseMessagingService.cancelIncomingCallNotification(context)
        val callId = pendingIncomingCall?.callId ?: _currentSession.value?.callId
        val targetPairId = pendingIncomingCall?.pairId ?: _currentSession.value?.pairId
        if (callId != null) {
            webSocketClient.sendEvent("CALL_REJECT", mapOf("callId" to callId, "reason" to "REJECTED"), targetPairId = targetPairId)
            Log.i(tag, "Sent CALL_REJECT for callId: $callId, pairId: $targetPairId")
        }
        pendingIncomingCall = null
        _callState.value = CallState.DECLINED
        scope.launch {
            delay(1000)
            endCallLocally()
        }
    }

    fun endCall() {
        ChatFirebaseMessagingService.cancelIncomingCallNotification(context)
        val callId = pendingIncomingCall?.callId ?: _currentSession.value?.callId
        val targetPairId = pendingIncomingCall?.pairId ?: _currentSession.value?.pairId
        if (callId != null) {
            webSocketClient.sendEvent("CALL_END", mapOf("callId" to callId, "reason" to "USER_HANGUP"), targetPairId = targetPairId)
            Log.i(tag, "Sent CALL_END for callId: $callId, pairId: $targetPairId")
        }
        pendingIncomingCall = null
        endCallLocally(notifyRemote = false)
    }

    @Volatile
    private var isTearingDown = false

    private fun endCallLocally(notifyRemote: Boolean = false) {
        if (_callState.value == CallState.ENDED || isTearingDown) {
            return
        }
        isTearingDown = true
        _callState.value = CallState.ENDED
        ChatFirebaseMessagingService.cancelIncomingCallNotification(context)
        _hasRemoteVideoFrame.value = false
        pendingIncomingCall = null

        // 1. Stop foreground call service immediately
        try {
            CallService.stopCallService(context)
        } catch (ignored: Exception) {}

        // 2. Teardown audio manager immediately
        try {
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (ignored: Exception) {}

        // RACE FIX: Capture instance refs locally and null them on THIS thread NOW,
        // before launching the IO coroutine. This means a new call started by the user
        // immediately after endCall() gets clean null instance vars and can safely
        // create fresh objects without racing against the teardown IO coroutine.
        val capturerToStop = videoCapturer
        val localVideoToDispose = localVideoTrack
        val remoteVideoToDispose = remoteVideoTrack
        val localAudioToDispose = localAudioTrack
        val pcToClose = peerConnection
        videoCapturer = null
        localVideoTrack = null
        remoteVideoTrack = null
        localAudioTrack = null
        peerConnection = null
        localRenderer = null
        remoteRenderer = null
        initializedLocalRenderer = null
        initializedRemoteRenderer = null
        isRenderersInitialized = false

        // 3. Clean up WebRTC peer connection and tracks on IO
        scope.launch(Dispatchers.IO) {
            try {
                synchronized(queuedRemoteIceCandidates) {
                    queuedRemoteIceCandidates.clear()
                }

                // Stop video capturer FIRST to prevent native deadlock
                try { capturerToStop?.stopCapture() } catch (ignored: Exception) {}
                try { capturerToStop?.dispose() } catch (ignored: Exception) {}

                try {
                    localVideoToDispose?.setEnabled(false)
                    localVideoToDispose?.dispose()
                } catch (ignored: Exception) {}

                try { remoteVideoToDispose?.setEnabled(false) } catch (ignored: Exception) {}

                try {
                    localAudioToDispose?.setEnabled(false)
                    localAudioToDispose?.dispose()
                } catch (ignored: Exception) {}

                try {
                    pcToClose?.close()
                    pcToClose?.dispose()
                } catch (ignored: Exception) {}

            } catch (e: Exception) {
                Log.w(tag, "Exception during WebRTC teardown: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    delay(400)
                    isTearingDown = false
                    // RACE FIX: Only reset to IDLE if still in ENDED state.
                    // If the user already started a new call during teardown, the state
                    // will be OUTGOING_CALLING or INCOMING_RINGING — don't overwrite it.
                    if (_callState.value == CallState.ENDED) {
                        _currentSession.value = null
                        _callState.value = CallState.IDLE
                        Log.i(tag, "CALL STATE: Session teardown complete -> IDLE")
                    } else {
                        Log.i(tag, "CALL STATE: Teardown done, new call already in state ${_callState.value} — not resetting to IDLE")
                    }
                    // Notify UI to navigate back when remote ends the call or connection fails
                    if (notifyRemote) {
                        onCallEndedRemotely?.invoke()
                    }
                }
            }
        }
    }


    private suspend fun createPeerConnection(callId: String, isVideo: Boolean, localRenderer: SurfaceViewRenderer? = null) {
        val iceServers = mutableListOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun.services.mozilla.com").createIceServer()
        )

        try {
            val turnRes = apiService.getTurnCredentials()
            if (turnRes.isSuccessful && turnRes.body() != null) {
                val creds = turnRes.body()!!
                for (url in creds.urls) {
                    iceServers.add(
                        PeerConnection.IceServer.builder(url)
                            .setUsername(creds.username)
                            .setPassword(creds.password)
                            .createIceServer()
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Using fallback STUN servers: ${e.message}")
        }

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    Log.d(tag, "Local ICE candidate generated: sdpMid=${it.sdpMid}, index=${it.sdpMLineIndex}")
                    val payload = mapOf(
                        "callId" to callId,
                        "candidate" to it.sdp,
                        "sdpMid" to it.sdpMid,
                        "sdpMLineIndex" to it.sdpMLineIndex
                    )
                    val targetPairId = _currentSession.value?.pairId ?: pendingIncomingCall?.pairId
                    webSocketClient.sendEvent("ICE_CANDIDATE", payload, targetPairId = targetPairId)
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(tag, "Signaling state changed: $state")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.i(tag, "ICE connection state changed: $state")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> {
                        onMediaConnected()
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        if (_callState.value == CallState.CONNECTED) {
                            _callState.value = CallState.RECONNECTING
                        }
                    }
                    PeerConnection.IceConnectionState.FAILED -> {
                        Log.e(tag, "ICE connection failed")
                        _callState.value = CallState.FAILED
                        endCallLocally(notifyRemote = true)
                    }
                    else -> {}
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.i(tag, "PeerConnection state changed: $newState")
                when (newState) {
                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        onMediaConnected()
                    }
                    PeerConnection.PeerConnectionState.DISCONNECTED -> {
                        if (_callState.value == CallState.CONNECTED) {
                            _callState.value = CallState.RECONNECTING
                        }
                    }
                    PeerConnection.PeerConnectionState.FAILED -> {
                        Log.e(tag, "PeerConnection failed")
                        _callState.value = CallState.FAILED
                        endCallLocally(notifyRemote = true)
                    }
                    PeerConnection.PeerConnectionState.CLOSED -> {
                        endCallLocally(notifyRemote = true)
                    }
                    else -> {}
                }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(tag, "ICE gathering state: $state")
            }

            override fun onAddStream(stream: MediaStream?) {
                Log.i(tag, "onAddStream called: ${stream?.id}")
                val track = stream?.videoTracks?.firstOrNull()
                if (track != null) {
                    Log.i(tag, "Remote VideoTrack received via onAddStream: ${track.id()}")
                    track.setEnabled(true)
                    remoteVideoTrack = track
                    scope.launch(Dispatchers.Main) {
                        attachRemoteVideoTrackIfReady()
                    }
                }
            }

            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}

            override fun onTrack(transceiver: RtpTransceiver?) {
                val track = transceiver?.receiver?.track()
                Log.i(tag, "Remote track received: id=${track?.id()}, kind=${track?.kind()}")
                if (track is VideoTrack) {
                    Log.i(tag, "Remote VideoTrack received via onTrack: ${track.id()}")
                    track.setEnabled(true)
                    remoteVideoTrack = track
                    scope.launch(Dispatchers.Main) {
                        attachRemoteVideoTrackIfReady()
                    }
                }
            }
        })

        setupLocalTracks(isVideo, localRenderer)
    }

    private fun onMediaConnected() {
        if (_callState.value != CallState.CONNECTED &&
            _callState.value != CallState.ENDED &&
            _callState.value != CallState.DECLINED
        ) {
            Log.i(tag, "CALL STATE: CONNECTED — WebRTC media established successfully!")
            _callState.value = CallState.CONNECTED
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            val isVideo = _currentSession.value?.isVideo == true
            audioManager.isSpeakerphoneOn = isVideo
            _isSpeakerOn.value = isVideo
            CallService.startCallService(context, isVideo)
        }
    }

    private fun setupLocalTracks(isVideo: Boolean, localRenderer: SurfaceViewRenderer? = null) {
        if (localAudioTrack == null) {
            val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
            localAudioTrack = peerConnectionFactory?.createAudioTrack("local_audio_track", audioSource)
            localAudioTrack?.setEnabled(true)
            localAudioTrack?.let {
                peerConnection?.addTrack(it, listOf("media_stream"))
                Log.i(tag, "Local audio track created, enabled, and added to PeerConnection")
            }
        }

        if (isVideo && localVideoTrack == null) {
            val hasCameraPerm = ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasCameraPerm) {
                Log.e(tag, "CAMERA permission not granted, cannot create video track")
            } else {
                videoCapturer = createCameraCapturer()
                if (videoCapturer != null) {
                    val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase?.eglBaseContext)
                    val videoSource = peerConnectionFactory?.createVideoSource(videoCapturer!!.isScreencast)
                    videoCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
                    try {
                        videoCapturer?.startCapture(1920, 1080, 30)
                        Log.i(tag, "Camera capture started at Full HD (1920x1080@30fps)")
                    } catch (e: Exception) {
                        Log.w(tag, "1080p capture failed, trying 720p: ${e.message}")
                        try {
                            videoCapturer?.startCapture(1280, 720, 30)
                            Log.i(tag, "Camera capture started at HD (1280x720@30fps)")
                        } catch (e2: Exception) {
                            Log.w(tag, "720p capture failed, trying 640x480: ${e2.message}")
                            try {
                                videoCapturer?.startCapture(640, 480, 30)
                            } catch (e3: Exception) {
                                Log.e(tag, "Fallback startCapture failed: ${e3.message}")
                            }
                        }
                    }

                    localVideoTrack = peerConnectionFactory?.createVideoTrack("local_video_track", videoSource)
                    localVideoTrack?.setEnabled(true)
                    localVideoTrack?.let {
                        peerConnection?.addTrack(it, listOf("media_stream"))
                        Log.i(tag, "Local video track created, enabled, and added to PeerConnection")
                    }
                }
            }
        }

        localRenderer?.let { renderer ->
            this.localRenderer = renderer
            attachLocalVideoTrackIfReady()
        }
    }

    private fun configureVideoBitrate(transceiver: RtpTransceiver) {
        try {
            val sender = transceiver.sender
            val parameters = sender.parameters
            if (parameters.encodings != null && parameters.encodings.isNotEmpty()) {
                for (encoding in parameters.encodings) {
                    encoding.maxBitrateBps = 4_000_000 // 4.0 Mbps max for crisp Full HD 1080p
                    encoding.minBitrateBps = 800_000   // 800 kbps min
                    encoding.maxFramerate = 30
                }
                sender.parameters = parameters
                Log.i(tag, "Configured video sender bitrate parameters (max: 4Mbps, min: 800kbps, 30fps)")
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not set video sender bitrate parameters: ${e.message}")
        }
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        return try {
            val enumerator = if (Camera2Enumerator.isSupported(context)) {
                Camera2Enumerator(context)
            } else {
                Camera1Enumerator(true)
            }
            for (deviceName in enumerator.deviceNames) {
                if (enumerator.isFrontFacing(deviceName)) {
                    val capturer = enumerator.createCapturer(deviceName, null)
                    if (capturer != null) {
                        Log.i(tag, "Selected front camera: $deviceName")
                        return capturer
                    }
                }
            }
            for (deviceName in enumerator.deviceNames) {
                if (!enumerator.isFrontFacing(deviceName)) {
                    val capturer = enumerator.createCapturer(deviceName, null)
                    if (capturer != null) {
                        Log.i(tag, "Selected rear camera: $deviceName")
                        return capturer
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(tag, "Error creating camera capturer: ${e.message}")
            null
        }
    }

    fun toggleMic(): Boolean {
        val muted = !_isMicMuted.value
        localAudioTrack?.setEnabled(!muted)
        _isMicMuted.value = muted
        return muted
    }

    fun toggleSpeaker(): Boolean {
        val speaker = !_isSpeakerOn.value
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = speaker
        _isSpeakerOn.value = speaker
        return speaker
    }

    fun toggleVideo(): Boolean {
        val video = !_isVideoEnabled.value
        localVideoTrack?.setEnabled(video)
        _isVideoEnabled.value = video
        return video
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) { Log.e("SimpleSdpObserver", "onCreateFailure: $error") }
        override fun onSetFailure(error: String?) { Log.e("SimpleSdpObserver", "onSetFailure: $error") }
    }
}
