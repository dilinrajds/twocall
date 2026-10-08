package com.twocall.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twocall.chat.audio.VoicePlayer
import com.twocall.chat.audio.VoiceRecorder
import com.twocall.chat.crypto.CryptoEngine
import com.twocall.chat.crypto.KeyStoreManager
import com.twocall.chat.data.local.entity.CallLogEntity
import com.twocall.chat.data.local.entity.ConversationEntity
import com.twocall.chat.data.local.entity.MessageEntity
import com.twocall.chat.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class ChatViewModel(
    private val repository: ChatRepository,
    private val keyStoreManager: KeyStoreManager,
    val voiceRecorder: VoiceRecorder,
    val voicePlayer: VoicePlayer,
    private val onConversationSelected: () -> Unit = {}
) : ViewModel() {

    private val _activePairId = MutableStateFlow(keyStoreManager.getActivePairId() ?: "")
    val activePairId = _activePairId.asStateFlow()

    val allConversations: StateFlow<List<ConversationEntity>> = repository.allConversationsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<MessageEntity>> = _activePairId.flatMapLatest { pairId ->
        if (pairId.isNotBlank()) repository.getMessagesFlowForPair(pairId)
        else repository.allMessagesFlow
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val conversation: StateFlow<ConversationEntity?> = _activePairId.flatMapLatest { pairId ->
        if (pairId.isNotBlank()) repository.getConversationFlowForPair(pairId)
        else repository.conversationFlow
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val callLogs: StateFlow<List<CallLogEntity>> = _activePairId.flatMapLatest { pairId ->
        if (pairId.isNotBlank()) repository.getCallLogsFlowForPair(pairId)
        else flowOf(emptyList())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
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

    fun selectConversation(pairId: String) {
        keyStoreManager.setActivePairId(pairId)
        _activePairId.value = pairId
        onConversationSelected()
        viewModelScope.launch {
            repository.clearUnread(pairId)
            try { repository.syncProfile(pairId) } catch (ignored: Exception) {}
            repository.syncMessagesForPair(pairId)
        }
    }

    fun clearUnread(pairId: String) {
        viewModelScope.launch {
            repository.clearUnread(pairId)
        }
    }

    suspend fun markVisibleMessagesRead(visibleMessages: List<MessageEntity>) {
        val pairId = _activePairId.value
        for (message in visibleMessages) {
            if (message.pairId == pairId && !message.isOutgoing && message.status != "READ") {
                repository.sendReceiptForPair(pairId, message.id, "READ")
            }
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
        val activePair = _activePairId.value
        val partnerKey = if (activePair.isNotBlank()) keyStoreManager.getPartnerPublicKey(activePair)
                         else keyStoreManager.getPartnerPublicKey()
        return if (partnerKey != null) CryptoEngine.computeKeyFingerprint(partnerKey) else "Awaiting Partner Key"
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
