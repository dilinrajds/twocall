package com.twocall.chat.webrtc

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.google.gson.Gson
import com.twocall.chat.data.remote.api.ChatApiService
import com.twocall.chat.data.remote.dto.WsEventDto
import com.twocall.chat.data.remote.ws.WebSocketClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*
import java.util.UUID

enum class CallState {
    IDLE,
    OUTGOING_RINGING,
    INCOMING_RINGING,
    CONNECTING,
    CONNECTED,
    ENDED
}

data class CallSessionData(
    val callId: String,
    val isVideo: Boolean,
    val isOutgoing: Boolean,
    val partnerDeviceId: String? = null
)

class WebRtcManager(
    private val context: Context,
    private val apiService: ChatApiService,
    private val webSocketClient: WebSocketClient,
    private val gson: Gson = Gson()
) {

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

    private var remoteRenderer: SurfaceViewRenderer? = null
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
        this.remoteRenderer = remoteRenderer
        val eglContext = rootEglBase?.eglBaseContext ?: return
        try {
            localRenderer.init(eglContext, null)
            localRenderer.setEnableHardwareScaler(true)
            localRenderer.setMirror(true)
        } catch (e: Exception) {
            Log.w(tag, "localRenderer already initialized: ${e.message}")
        }

        try {
            remoteRenderer.init(eglContext, null)
            remoteRenderer.setEnableHardwareScaler(true)
        } catch (e: Exception) {
            Log.w(tag, "remoteRenderer already initialized: ${e.message}")
        }

        remoteVideoTrack?.let { track ->
            try {
                track.addSink(remoteRenderer)
            } catch (e: Exception) {
                Log.w(tag, "Error adding remote sink: ${e.message}")
            }
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

                _currentSession.value = CallSessionData(callId, isVideo, false, event.senderDeviceId)
                _callState.value = CallState.INCOMING_RINGING

                createPeerConnection(callId, isVideo)
                val sessionDesc = SessionDescription(SessionDescription.Type.OFFER, sdp)
                peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        drainQueuedIceCandidates()
                    }
                }, sessionDesc)
            }

            "CALL_ANSWER" -> {
                val payloadJson = gson.toJson(event.payload)
                val payload = gson.fromJson(payloadJson, Map::class.java)
                val sdp = payload["sdp"] as? String ?: return

                val sessionDesc = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        drainQueuedIceCandidates()
                    }
                }, sessionDesc)
                _callState.value = CallState.CONNECTED
            }

            "ICE_CANDIDATE" -> {
                val payloadJson = gson.toJson(event.payload)
                val payload = gson.fromJson(payloadJson, Map::class.java)
                val candidateStr = payload["candidate"] as? String ?: return
                val sdpMid = payload["sdpMid"] as? String ?: ""
                val sdpMLineIndex = (payload["sdpMLineIndex"] as? Number)?.toInt() ?: 0

                val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidateStr)
                if (peerConnection?.remoteDescription != null) {
                    peerConnection?.addIceCandidate(iceCandidate)
                } else {
                    synchronized(queuedRemoteIceCandidates) {
                        queuedRemoteIceCandidates.add(iceCandidate)
                    }
                }
            }

            "CALL_END", "CALL_REJECT" -> {
                endCallLocally()
            }
        }
    }

    private fun drainQueuedIceCandidates() {
        synchronized(queuedRemoteIceCandidates) {
            for (candidate in queuedRemoteIceCandidates) {
                peerConnection?.addIceCandidate(candidate)
            }
            queuedRemoteIceCandidates.clear()
        }
    }

    fun startOutgoingCall(isVideo: Boolean, localRenderer: SurfaceViewRenderer? = null, remoteRenderer: SurfaceViewRenderer? = null) {
        if (remoteRenderer != null) {
            this.remoteRenderer = remoteRenderer
        }
        val callId = UUID.randomUUID().toString()
        _currentSession.value = CallSessionData(callId, isVideo, true)
        _callState.value = CallState.OUTGOING_RINGING

        scope.launch(Dispatchers.IO) {
            createPeerConnection(callId, isVideo, localRenderer)

            val sdpMediaConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                if (isVideo) {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                }
            }

            peerConnection?.createOffer(object : SimpleSdpObserver() {
                override fun onCreateSuccess(desc: SessionDescription?) {
                    desc?.let { sdp ->
                        peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
                        val payload = mapOf(
                            "callId" to callId,
                            "callType" to (if (isVideo) "VIDEO" else "AUDIO"),
                            "sdp" to sdp.description
                        )
                        webSocketClient.sendEvent("CALL_OFFER", payload)
                    }
                }
            }, sdpMediaConstraints)
        }
    }

    fun acceptIncomingCall(localRenderer: SurfaceViewRenderer? = null, remoteRenderer: SurfaceViewRenderer? = null) {
        if (remoteRenderer != null) {
            this.remoteRenderer = remoteRenderer
        }
        val session = _currentSession.value ?: return
        _callState.value = CallState.CONNECTING

        scope.launch(Dispatchers.IO) {
            setupLocalTracks(session.isVideo, localRenderer)

            val sdpMediaConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                if (session.isVideo) {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                }
            }

            peerConnection?.createAnswer(object : SimpleSdpObserver() {
                override fun onCreateSuccess(desc: SessionDescription?) {
                    desc?.let { sdp ->
                        peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
                        val payload = mapOf(
                            "callId" to session.callId,
                            "sdp" to sdp.description
                        )
                        webSocketClient.sendEvent("CALL_ANSWER", payload)
                        _callState.value = CallState.CONNECTED
                    }
                }
            }, sdpMediaConstraints)
        }
    }

    fun rejectIncomingCall() {
        val session = _currentSession.value ?: return
        webSocketClient.sendEvent("CALL_REJECT", mapOf("callId" to session.callId, "reason" to "REJECTED"))
        endCallLocally()
    }

    fun endCall() {
        val session = _currentSession.value ?: return
        webSocketClient.sendEvent("CALL_END", mapOf("callId" to session.callId, "reason" to "USER_HANGUP"))
        endCallLocally()
    }

    private fun endCallLocally() {
        _callState.value = CallState.ENDED
        _currentSession.value = null

        scope.launch(Dispatchers.IO) {
            synchronized(queuedRemoteIceCandidates) {
                queuedRemoteIceCandidates.clear()
            }
            try {
                peerConnection?.close()
            } catch (ignored: Exception) {}
            peerConnection = null

            try {
                localAudioTrack?.dispose()
            } catch (ignored: Exception) {}
            localAudioTrack = null

            try {
                localVideoTrack?.dispose()
            } catch (ignored: Exception) {}
            localVideoTrack = null
            remoteVideoTrack = null

            try {
                videoCapturer?.stopCapture()
                videoCapturer?.dispose()
            } catch (ignored: Exception) {}
            videoCapturer = null

            withContext(Dispatchers.Main) {
                audioManager.isSpeakerphoneOn = false
                audioManager.mode = AudioManager.MODE_NORMAL
                _callState.value = CallState.IDLE
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
                    val payload = mapOf(
                        "callId" to callId,
                        "candidate" to it.sdp,
                        "sdpMid" to it.sdpMid,
                        "sdpMLineIndex" to it.sdpMLineIndex
                    )
                    webSocketClient.sendEvent("ICE_CANDIDATE", payload)
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                if (state == PeerConnection.IceConnectionState.DISCONNECTED || state == PeerConnection.IceConnectionState.FAILED) {
                    endCallLocally()
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}

            override fun onTrack(transceiver: RtpTransceiver?) {
                val track = transceiver?.receiver?.track()
                if (track is VideoTrack) {
                    Log.i(tag, "Remote VideoTrack received!")
                    remoteVideoTrack = track
                    scope.launch(Dispatchers.Main) {
                        remoteRenderer?.let { renderer ->
                            try {
                                track.addSink(renderer)
                            } catch (e: Exception) {
                                Log.w(tag, "Failed to attach remote track sink: ${e.message}")
                            }
                        }
                    }
                }
            }
        })

        setupLocalTracks(isVideo, localRenderer)
    }

    private fun setupLocalTracks(isVideo: Boolean, localRenderer: SurfaceViewRenderer? = null) {
        val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("local_audio_track", audioSource)
        localAudioTrack?.let { peerConnection?.addTrack(it, listOf("media_stream")) }

        if (isVideo) {
            videoCapturer = createCameraCapturer()
            if (videoCapturer != null) {
                val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase?.eglBaseContext)
                val videoSource = peerConnectionFactory?.createVideoSource(videoCapturer!!.isScreencast)
                videoCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
                videoCapturer?.startCapture(1280, 720, 30)

                localVideoTrack = peerConnectionFactory?.createVideoTrack("local_video_track", videoSource)
                localRenderer?.let { localVideoTrack?.addSink(it) }
                localVideoTrack?.let { peerConnection?.addTrack(it, listOf("media_stream")) }
            }
        }

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = isVideo
        _isSpeakerOn.value = isVideo
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        return try {
            val enumerator = Camera2Enumerator(context)
            for (deviceName in enumerator.deviceNames) {
                if (enumerator.isFrontFacing(deviceName)) {
                    return enumerator.createCapturer(deviceName, null)
                }
            }
            for (deviceName in enumerator.deviceNames) {
                if (!enumerator.isFrontFacing(deviceName)) {
                    return enumerator.createCapturer(deviceName, null)
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
