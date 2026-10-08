package com.twocall.chat.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.twocall.chat.crypto.CryptoEngine
import com.twocall.chat.crypto.KeyStoreManager
import com.twocall.chat.data.local.dao.CallLogDao
import com.twocall.chat.data.local.dao.ConversationDao
import com.twocall.chat.data.local.dao.MessageDao
import com.twocall.chat.data.local.entity.CallLogEntity
import com.twocall.chat.data.local.entity.ConversationEntity
import com.twocall.chat.data.local.entity.MessageEntity
import com.twocall.chat.data.remote.api.ChatApiService
import com.twocall.chat.data.remote.dto.*
import com.twocall.chat.data.remote.ws.WebSocketClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ChatRepository(
    private val context: Context,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val callLogDao: CallLogDao,
    private val apiService: ChatApiService,
    private val webSocketClient: WebSocketClient,
    private val keyStoreManager: KeyStoreManager,
    private val gson: Gson = Gson()
) {

    private val tag = "ChatRepository"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Flows ────────────────────────────────────────────────────────────────

    /** All conversations for home screen */
    val allConversationsFlow = conversationDao.getAllConversationsFlow()

    /** Messages for the ACTIVE pair */
    val allMessagesFlow: Flow<List<MessageEntity>>
        get() {
            val pairId = keyStoreManager.getActivePairId() ?: ""
            return if (pairId.isNotBlank()) messageDao.getMessagesFlowForPair(pairId)
            else messageDao.getAllMessagesFlow()
        }

    /** Conversation for the ACTIVE pair */
    val conversationFlow: Flow<ConversationEntity?>
        get() {
            val pairId = keyStoreManager.getActivePairId() ?: ""
            return if (pairId.isNotBlank()) conversationDao.getConversationFlowByPairId(pairId)
            else conversationDao.getConversationFlow()
        }

    var onPairTerminated: (() -> Unit)? = null

    private val _loveAnimationEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val loveAnimationEvents = _loveAnimationEvents.asSharedFlow()

    // ── Scoped per-pair helpers ──────────────────────────────────────────────

    fun getMessagesFlowForPair(pairId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesFlowForPair(pairId)

    fun getConversationFlowForPair(pairId: String): Flow<ConversationEntity?> =
        conversationDao.getConversationFlowByPairId(pairId)

    fun getCallLogsFlowForPair(pairId: String): Flow<List<CallLogEntity>> =
        callLogDao.getCallLogsForPair(pairId)

    suspend fun clearUnread(pairId: String) {
        conversationDao.clearUnread(pairId)
    }

    suspend fun syncProfile(pairId: String) {
        val response = apiService.getProfiles(pairId)
        if (!response.isSuccessful) return
        val profiles = response.body() ?: return
        val localName = keyStoreManager.getMyProfileName()
        if (localName.isNotBlank() && (profiles.mine.name != localName ||
                profiles.mine.imageBase64.orEmpty() != keyStoreManager.getMyProfileImage().orEmpty())) {
            apiService.updateProfile(ProfileDto(localName, keyStoreManager.getMyProfileImage()), pairId)
        }
        conversationDao.updateProfile(pairId, profiles.partner.name, profiles.partner.imageBase64)
    }

    suspend fun updateMyProfile(name: String, image: String?): Boolean {
        require(name.isNotBlank() && name.trim().length <= 64)
        keyStoreManager.saveMyProfile(name.trim(), image)
        var success = true
        for (pairId in keyStoreManager.getAllPairIds()) {
            try {
                if (!apiService.updateProfile(ProfileDto(name.trim(), image), pairId).isSuccessful) success = false
            } catch (e: Exception) { success = false }
        }
        return success
    }

    // ── Initialization ───────────────────────────────────────────────────────

    init {
        // Observe WebSocket incoming events
        scope.launch {
            webSocketClient.incomingEvents.collect { event ->
                handleIncomingWsEvent(event)
            }
        }

        // Auto-sync whenever WebSocket reconnects
        scope.launch {
            webSocketClient.connectionState.collect { state ->
                if (state == com.twocall.chat.data.remote.ws.WsConnectionState.CONNECTED) {
                    try { syncMessagesForAllPairs() } catch (ignored: Exception) {}
                }
            }
        }
    }

    // ── Sync ─────────────────────────────────────────────────────────────────

    /** Sync active pair messages */
    suspend fun syncMessages() {
        val pairId = keyStoreManager.getActivePairId() ?: return
        syncMessagesForPair(pairId)
    }

    /** Sync messages for all known pairs */
    suspend fun syncMessagesForAllPairs(strict: Boolean = false) {
        var failure: Exception? = null
        for (pairId in keyStoreManager.getAllPairIds()) {
            try { syncProfile(pairId) } catch (e: Exception) { failure = e }
            try { syncMessagesForPair(pairId, strict) } catch (e: Exception) { failure = e }
        }
        if (strict) failure?.let { throw it }
    }

    suspend fun syncMessagesForPair(pairId: String, strict: Boolean = false) {
        val aesKey = keyStoreManager.getSharedAesKey(pairId) ?: return
        val myDeviceId = keyStoreManager.getDeviceId(pairId) ?: return

        try {
            val res = apiService.syncMessages(null, pairId)
            if (!res.isSuccessful) throw java.io.IOException("Message sync HTTP ${res.code()}")
            if (res.isSuccessful && res.body() != null) {
                val list = res.body()!!
                var latestPreview: String? = null
                var latestTimestamp = 0L
                for (dto in list) {
                    if (dto.pairId != pairId) continue // skip other pairs
                    val isOutgoing = dto.senderDeviceId == myDeviceId
                    val existingMsg = messageDao.getMessageById(dto.id)

                    val decryptedText = if (dto.isDeleted) {
                        ""
                    } else {
                        try {
                            CryptoEngine.decrypt(dto.ciphertextPayload, dto.iv, aesKey)
                        } catch (e: Exception) {
                            "[Decryption Error]"
                        }
                    }

                    val entity = MessageEntity(
                        id = dto.id,
                        pairId = pairId,
                        senderDeviceId = dto.senderDeviceId,
                        clientMessageId = dto.clientMessageId,
                        plaintext = decryptedText,
                        ciphertext = dto.ciphertextPayload,
                        iv = dto.iv,
                        messageType = dto.messageType,
                        replyToMessageId = dto.replyToMessageId,
                        attachmentRemoteId = dto.mediaAttachmentId,
                        attachmentLocalPath = existingMsg?.attachmentLocalPath,
                        status = dto.status,
                        isOutgoing = isOutgoing,
                        timestamp = parseIsoTimestamp(dto.createdAt),
                        isDeleted = dto.isDeleted
                    )
                    messageDao.insertOrUpdate(entity)

                    // Track latest for conversation preview
                    val ts = parseIsoTimestamp(dto.createdAt)
                    if (ts > latestTimestamp) {
                        latestTimestamp = ts
                        latestPreview = buildPreview(decryptedText, dto.messageType, isOutgoing)
                    }

                    if (!dto.mediaAttachmentId.isNullOrBlank() && existingMsg?.attachmentLocalPath == null) {
                        downloadAttachmentIfNeeded(dto.id, dto.mediaAttachmentId, pairId)
                    }

                    if (!isOutgoing && dto.status != "READ") {
                        sendReceiptForPair(pairId, dto.id, "DELIVERED")
                    }
                }

                // Update conversation preview
                if (latestPreview != null && latestTimestamp > 0) {
                    conversationDao.updateLastMessage(pairId, latestPreview, latestTimestamp)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Sync messages failed for pair $pairId: ${e.message}")
            if (strict) throw e
        }
    }

    private fun buildPreview(plaintext: String, messageType: String, isOutgoing: Boolean): String {
        val prefix = if (isOutgoing) "You: " else ""
        return when (messageType.uppercase()) {
            "IMAGE" -> "${prefix}📷 Photo"
            "VIDEO" -> "${prefix}🎥 Video"
            "AUDIO" -> "${prefix}🎤 Voice message"
            "DOCUMENT" -> "${prefix}📄 Document"
            else -> "$prefix${plaintext.take(50)}"
        }
    }

    // ── Send Messages ─────────────────────────────────────────────────────────

    suspend fun sendTextMessage(text: String, replyToMessageId: String? = null) {
        val pairId = keyStoreManager.getActivePairId() ?: return
        val myDeviceId = keyStoreManager.getDeviceId(pairId) ?: return
        val aesKey = keyStoreManager.getSharedAesKey(pairId) ?: run {
            Log.e(tag, "Cannot encrypt: Shared AES key is null for pair $pairId")
            return
        }

        val encrypted = CryptoEngine.encrypt(text, aesKey)
        val clientMsgId = UUID.randomUUID().toString()
        val tempId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val localMessage = MessageEntity(
            id = tempId,
            pairId = pairId,
            senderDeviceId = myDeviceId,
            clientMessageId = clientMsgId,
            plaintext = text,
            ciphertext = encrypted.ciphertext,
            iv = encrypted.iv,
            messageType = "TEXT",
            replyToMessageId = replyToMessageId,
            status = "PENDING",
            isOutgoing = true,
            timestamp = now
        )
        messageDao.insertOrUpdate(localMessage)
        conversationDao.updateLastMessage(pairId, "You: ${text.take(50)}", now)

        try {
            val req = SendMessageRequestDto(
                clientMessageId = clientMsgId,
                ciphertextPayload = encrypted.ciphertext,
                iv = encrypted.iv,
                ephemeralPublicKey = null,
                messageType = "TEXT",
                replyToMessageId = replyToMessageId,
                mediaAttachmentId = null
            )
            val response = apiService.sendMessage(req)
            if (response.isSuccessful && response.body() != null) {
                val serverMsg = response.body()!!
                messageDao.deleteById(tempId)
                messageDao.insertOrUpdate(localMessage.copy(id = serverMsg.id, status = serverMsg.status))
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to send message: ${e.message}")
        }
    }

    suspend fun sendMediaMessage(file: File, contentType: String, messageType: String, replyToMessageId: String? = null) {
        val pairId = keyStoreManager.getActivePairId() ?: return
        val myDeviceId = keyStoreManager.getDeviceId(pairId) ?: return
        val aesKey = keyStoreManager.getSharedAesKey(pairId) ?: return

        val requestFile = file.asRequestBody(contentType.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val uploadRes = apiService.uploadMedia(body, null)
        if (!uploadRes.isSuccessful || uploadRes.body() == null) {
            Log.e(tag, "Media upload failed: ${uploadRes.code()}")
            return
        }

        val attachment = uploadRes.body()!!
        val encryptedMeta = CryptoEngine.encrypt(file.name, aesKey)
        val clientMsgId = UUID.randomUUID().toString()
        val tempId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val localMessage = MessageEntity(
            id = tempId,
            pairId = pairId,
            senderDeviceId = myDeviceId,
            clientMessageId = clientMsgId,
            plaintext = "[${messageType.lowercase()}]",
            ciphertext = encryptedMeta.ciphertext,
            iv = encryptedMeta.iv,
            messageType = messageType,
            replyToMessageId = replyToMessageId,
            attachmentLocalPath = file.absolutePath,
            attachmentRemoteId = attachment.attachmentId,
            attachmentFileName = file.name,
            status = "PENDING",
            isOutgoing = true,
            timestamp = now
        )
        messageDao.insertOrUpdate(localMessage)
        conversationDao.updateLastMessage(pairId, buildPreview("[${messageType.lowercase()}]", messageType, true), now)

        val req = SendMessageRequestDto(
            clientMessageId = clientMsgId,
            ciphertextPayload = encryptedMeta.ciphertext,
            iv = encryptedMeta.iv,
            ephemeralPublicKey = null,
            messageType = messageType,
            replyToMessageId = replyToMessageId,
            mediaAttachmentId = attachment.attachmentId
        )

        val sendRes = apiService.sendMessage(req)
        if (sendRes.isSuccessful && sendRes.body() != null) {
            val serverMsg = sendRes.body()!!
            messageDao.deleteById(tempId)
            messageDao.insertOrUpdate(localMessage.copy(id = serverMsg.id, status = serverMsg.status))
        }
    }

    // ── Receipt / Reaction / Delete ─────────────────────────────────────────

    suspend fun sendReceipt(messageId: String, status: String) {
        val pairId = keyStoreManager.getActivePairId() ?: return
        sendReceiptForPair(pairId, messageId, status)
    }

    suspend fun sendReceiptForPair(pairId: String, messageId: String, status: String) {
        try {
            val response = apiService.updateReceipt(ReceiptUpdateRequestDto(messageId, status), pairId)
            if (!response.isSuccessful) return
            messageDao.updateStatus(messageId, status)
            if (status == "READ") {
                conversationDao.clearUnread(pairId)
            }
        } catch (e: Exception) {
            Log.w(tag, "Update receipt failed: ${e.message}")
        }
    }

    suspend fun sendReaction(messageId: String, emoji: String) {
        try {
            apiService.sendReaction(ReactionRequestDto(messageId, emoji))
            messageDao.updateReaction(messageId, emoji)
        } catch (e: Exception) {
            Log.w(tag, "Send reaction failed: ${e.message}")
        }
    }

    suspend fun deleteMessage(messageId: String) {
        messageDao.markDeleted(messageId)
        val msg = messageDao.getMessageById(messageId)
        val targetServerId = msg?.id ?: messageId
        try {
            val response = apiService.deleteMessage(targetServerId)
            if (!response.isSuccessful) {
                Log.w(tag, "Delete message server responded with code ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(tag, "Delete message API call failed: ${e.message}")
        }
    }

    fun sendTyping(isTyping: Boolean) {
        webSocketClient.sendEvent(if (isTyping) "TYPING_START" else "TYPING_STOP", mapOf("typing" to isTyping))
    }

    // ── Call Log ─────────────────────────────────────────────────────────────

    suspend fun saveCallLog(callLog: CallLogEntity) {
        try {
            callLogDao.insertOrUpdate(callLog)
        } catch (e: Exception) {
            Log.w(tag, "Failed to save call log: ${e.message}")
        }
    }

    suspend fun updateLastMessagePreview(pairId: String, preview: String, timestamp: Long) {
        try {
            conversationDao.updateLastMessage(pairId, preview, timestamp)
        } catch (e: Exception) {
            Log.w(tag, "Failed to update last message preview: ${e.message}")
        }
    }

    // ── Media Download ───────────────────────────────────────────────────────

    private fun downloadAttachmentIfNeeded(messageId: String, attachmentRemoteId: String, pairId: String? = null) {
        scope.launch(Dispatchers.IO) {
            try {
                val response = apiService.downloadMedia(attachmentRemoteId, pairId)
                if (response.isSuccessful && response.body() != null) {
                    val mediaDir = File(context.cacheDir, "attachments").apply { mkdirs() }
                    val localFile = File(mediaDir, "att_$attachmentRemoteId")
                    response.body()!!.byteStream().use { input ->
                        FileOutputStream(localFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    messageDao.updateAttachmentLocalPath(messageId, localFile.absolutePath)
                }
            } catch (e: Exception) {
                Log.w(tag, "Error downloading attachment $attachmentRemoteId: ${e.message}")
            }
        }
    }

    // ── WebSocket Event Handler ───────────────────────────────────────────────

    private suspend fun handleIncomingWsEvent(event: WsEventDto<Any>) {
        val eventPairId = event.pairId
        val aesKey = keyStoreManager.getSharedAesKey(eventPairId)

        // Any event from partner implies partner is currently online for that pair
        if (event.eventType != "PRESENCE") {
            conversationDao.updatePresence(eventPairId, true, System.currentTimeMillis())
        }

        when (event.eventType) {
            "MESSAGE_SENT" -> {
                val payloadJson = gson.toJson(event.payload)
                val msgDto = gson.fromJson(payloadJson, MessageResponseDto::class.java)

                val decrypted = if (aesKey != null) {
                    try {
                        CryptoEngine.decrypt(msgDto.ciphertextPayload, msgDto.iv, aesKey)
                    } catch (e: Exception) {
                        "[Encrypted Message]"
                    }
                } else "[Encrypted Message]"

                val entity = MessageEntity(
                    id = msgDto.id,
                    pairId = eventPairId,
                    senderDeviceId = msgDto.senderDeviceId,
                    clientMessageId = msgDto.clientMessageId,
                    plaintext = decrypted,
                    ciphertext = msgDto.ciphertextPayload,
                    iv = msgDto.iv,
                    messageType = msgDto.messageType,
                    replyToMessageId = msgDto.replyToMessageId,
                    attachmentRemoteId = msgDto.mediaAttachmentId,
                    status = "DELIVERED",
                    isOutgoing = false,
                    timestamp = parseIsoTimestamp(msgDto.createdAt)
                )
                messageDao.insertOrUpdate(entity)

                // Update conversation preview and unread for this pair
                val preview = buildPreview(decrypted, msgDto.messageType, false)
                conversationDao.updateLastMessage(eventPairId, preview, parseIsoTimestamp(msgDto.createdAt))

                // Increment unread only if this is NOT the currently active pair
                if (eventPairId != keyStoreManager.getActivePairId()) {
                    conversationDao.incrementUnread(eventPairId)
                }

                if (!msgDto.mediaAttachmentId.isNullOrBlank()) {
                    downloadAttachmentIfNeeded(msgDto.id, msgDto.mediaAttachmentId)
                }

                if (com.twocall.chat.ui.components.containsLoveEmoji(decrypted)) {
                    _loveAnimationEvents.tryEmit(Unit)
                }

                sendReceiptForPair(eventPairId, msgDto.id, "DELIVERED")
            }

            "MESSAGE_DELIVERED" -> {
                val payloadJson = gson.toJson(event.payload)
                val data = gson.fromJson(payloadJson, Map::class.java)
                val msgId = data["messageId"] as? String ?: return
                messageDao.updateStatus(msgId, "DELIVERED")
            }

            "MESSAGE_READ" -> {
                val payloadJson = gson.toJson(event.payload)
                val data = gson.fromJson(payloadJson, Map::class.java)
                val msgId = data["messageId"] as? String ?: return
                messageDao.updateStatus(msgId, "READ")
            }

            "MESSAGE_REACTION" -> {
                val payloadJson = gson.toJson(event.payload)
                val data = gson.fromJson(payloadJson, Map::class.java)
                val msgId = data["messageId"] as? String ?: return
                val emoji = data["emoji"] as? String
                messageDao.updateReaction(msgId, emoji)
                if (emoji != null && com.twocall.chat.ui.components.containsLoveEmoji(emoji)) {
                    _loveAnimationEvents.tryEmit(Unit)
                }
            }

            "MESSAGE_DELETED" -> {
                val payloadJson = gson.toJson(event.payload)
                val data = gson.fromJson(payloadJson, Map::class.java)
                val msgId = data["messageId"] as? String ?: return
                messageDao.markDeleted(msgId)
            }

            "TYPING_START" -> conversationDao.updateTyping(eventPairId, true)
            "TYPING_STOP" -> conversationDao.updateTyping(eventPairId, false)

            "PRESENCE" -> {
                val payloadJson = gson.toJson(event.payload)
                val data = gson.fromJson(payloadJson, Map::class.java)
                val eventName = data["event"] as? String

                if (eventName == "PROFILE_UPDATED") {
                    syncProfile(eventPairId)
                    return
                }

                if (eventName == "PAIR_DELETED" || eventName == "PARTNER_DISCONNECTED") {
                    Log.i(tag, "Pair terminated by partner: $eventName for pairId=$eventPairId")
                    onPairTerminated?.invoke()
                    return
                }

                val online = data["online"] as? Boolean ?: false
                conversationDao.updatePresence(eventPairId, online, System.currentTimeMillis())

                if (eventName == "PAIRING_COMPLETE") {
                    val partnerDevId = data["partnerDeviceId"] as? String
                    val partnerKey = data["partnerPublicKey"] as? String
                    if (partnerDevId != null && partnerKey != null) {
                        keyStoreManager.savePartnerInfo(eventPairId, partnerDevId, partnerKey)
                    }
                }
            }
        }
    }

    private fun parseIsoTimestamp(isoString: String?): Long {
        if (isoString == null) return System.currentTimeMillis()
        return try {
            java.time.Instant.parse(isoString).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
