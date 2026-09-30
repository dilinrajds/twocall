package com.twocall.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twocall.chat.audio.VoicePlayer
import com.twocall.chat.audio.VoiceRecorder
import com.twocall.chat.crypto.CryptoEngine
import com.twocall.chat.crypto.KeyStoreManager
import com.twocall.chat.data.local.entity.ConversationEntity
import com.twocall.chat.data.local.entity.MessageEntity
import com.twocall.chat.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class ChatViewModel(
    private val repository: ChatRepository,
    private val keyStoreManager: KeyStoreManager,
    val voiceRecorder: VoiceRecorder,
    val voicePlayer: VoicePlayer
) : ViewModel() {

    val messages = repository.allMessagesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val conversation = repository.conversationFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val loveAnimationEvents = repository.loveAnimationEvents

    private val _replyingTo = MutableStateFlow<MessageEntity?>(null)
    val replyingTo = _replyingTo.asStateFlow()

    private var typingJob: Job? = null

    init {
        viewModelScope.launch {
            repository.syncMessages()
        }
    }

    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val replyId = _replyingTo.value?.id
        _replyingTo.value = null

        viewModelScope.launch {
            repository.sendTextMessage(trimmed, replyId)
        }
    }

    fun onTypingChanged(text: String) {
        typingJob?.cancel()
        if (text.isNotEmpty()) {
            repository.sendTyping(true)
            typingJob = viewModelScope.launch {
                delay(3000)
                repository.sendTyping(false)
            }
        } else {
            repository.sendTyping(false)
        }
    }

    fun startVoiceRecording(): File? {
        return voiceRecorder.startRecording()
    }

    fun stopAndSendVoiceRecording() {
        val file = voiceRecorder.stopRecording() ?: return
        val replyId = _replyingTo.value?.id
        _replyingTo.value = null

        viewModelScope.launch {
            repository.sendMediaMessage(file, "audio/mp4", "AUDIO", replyId)
        }
    }

    fun cancelVoiceRecording() {
        voiceRecorder.cancelRecording()
    }

    fun playVoiceMessage(filePath: String) {
        voicePlayer.play(filePath)
    }

    fun sendAttachment(file: File, contentType: String, messageType: String) {
        val replyId = _replyingTo.value?.id
        _replyingTo.value = null

        viewModelScope.launch {
            repository.sendMediaMessage(file, contentType, messageType, replyId)
        }
    }

    fun setReplyingTo(message: MessageEntity?) {
        _replyingTo.value = message
    }

    fun addReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            repository.sendReaction(messageId, emoji)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun getSafetyFingerprint(): String {
        val partnerKey = keyStoreManager.getPartnerPublicKey() ?: return "Awaiting Partner Key"
        return CryptoEngine.computeKeyFingerprint(partnerKey)
    }

    fun getMyFingerprint(): String {
        val myKey = keyStoreManager.getMyPublicKeyBase64()
        return CryptoEngine.computeKeyFingerprint(myKey)
    }

    override fun onCleared() {
        super.onCleared()
        voicePlayer.release()
    }
}
