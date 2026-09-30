package com.twocall.chat.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.twocall.chat.crypto.CryptoEngine
import com.twocall.chat.crypto.KeyStoreManager
import com.twocall.chat.data.local.dao.ConversationDao
import com.twocall.chat.data.local.dao.MessageDao
import com.twocall.chat.data.local.entity.ConversationEntity
import com.twocall.chat.data.local.entity.MessageEntity
import com.twocall.chat.data.remote.api.ChatApiService
import com.twocall.chat.data.remote.dto.*
import com.twocall.chat.data.remote.ws.WebSocketClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
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
    private val apiService: ChatApiService,
    private val webSocketClient: WebSocketClient,
    private val keyStoreManager: KeyStoreManager,
    private val gson: Gson = Gson()
) {

    private val tag = "ChatRepository"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val allMessagesFlow: Flow<List<MessageEntity>> = messageDao.getAllMessagesFlow()
    val conversationFlow: Flow<ConversationEntity?> = conversationDao.getConversationFlow()

    var onPairTerminated: (() -> Unit)? = null

    private val _loveAnimationEvents = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val loveAnimationEvents = _loveAnimationEvents.asSharedFlow()

    init {
        // Observe WebSocket incoming events
        scope.launch {
            webSocketClient.incomingEvents.collect { event ->
                handleIncomingWsEvent(event)
            }
        }
    }

    suspend fun sendTextMessage(text: String, replyToMessageId: String? = null) {
        val pairId = keyStoreManager.getPairId() ?: return
        val myDeviceId = keyStoreManager.getDeviceId() ?: return
        val aesKey = keyStoreManager.getSharedSessionAesKey() ?: run {
            Log.e(tag, "Cannot encrypt message: Shared AES key is null (partner not paired yet)")
            return
        }

        // 1. E2EE Encrypt
        val encrypted = CryptoEngine.encrypt(text, aesKey)
        val clientMsgId = UUID.randomUUID().toString()
        val tempId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        // 2. Persist locally as PENDING immediately (optimistic UI)
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

        // 3. Send to backend via REST
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
                messageDao.insertOrUpdate(
                    localMessage.copy(
                        id = serverMsg.id,
                        status = serverMsg.status
                    )
                )
            } else {
                Log.w(tag, "Message send returned error code ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to send message over network, will retry offline queue: ${e.message}")
        }
    }

    suspend fun sendMediaMessage(file: File, contentType: String, messageType: String, replyToMessageId: String? = null) {
        val pairId = keyStoreManager.getPairId() ?: return
        val myDeviceId = keyStoreManager.getDeviceId() ?: return
        val aesKey = keyStoreManager.getSharedSessionAesKey() ?: return

        val requestFile = file.asRequestBody(contentType.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val uploadRes = apiService.uploadMedia(body, null)
        if (!uploadRes.isSuccessful || uploadRes.body() == null) {
            Log.e(tag, "Media upload failed: ${uploadRes.code()}")
            return
        }

        val attachment = uploadRes.body()!!

        // Encrypt the metadata / filename with E2EE
        val encryptedMeta = CryptoEngine.encrypt(file.name, aesKey)
        val clientMsgId = UUID.randomUUID().toString()
        val tempId = UUID.randomUUID().toString()

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
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertOrUpdate(localMessage)

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

    private fun downloadAttachmentIfNeeded(messageId: String, attachmentRemoteId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val response = apiService.downloadMedia(attachmentRemoteId)
                if (response.isSuccessful && response.body() != null) {
                    val mediaDir = File(context.cacheDir, "attachments").apply { mkdirs() }
                    val localFile = File(mediaDir, "att_$attachmentRemoteId")
                    response.body()!!.byteStream().use { input ->
                        FileOutputStream(localFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    messageDao.updateAttachmentLocalPath(messageId, localFile.absolutePath)
                    Log.i(tag, "Downloaded attachment $attachmentRemoteId for message $messageId -> ${localFile.absolutePath}")
                } else {
                    Log.w(tag, "Failed to download media attachment $attachmentRemoteId: code ${response.code()}")
                }
            } catch (e: Exception) {
                Log.w(tag, "Error downloading media attachment $attachmentRemoteId: ${e.message}")
            }
        }
    }

    suspend fun syncMessages() {
        val pairId = keyStoreManager.getPairId() ?: return
        val aesKey = keyStoreManager.getSharedSessionAesKey() ?: return
        val myDeviceId = keyStoreManager.getDeviceId() ?: return

        try {
            val res = apiService.syncMessages(null)
            if (res.isSuccessful && res.body() != null) {
                val list = res.body()!!
                for (dto in list) {
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

                    // Auto-download attachment if received media message and not saved locally yet
                    if (!dto.mediaAttachmentId.isNullOrBlank() && existingMsg?.attachmentLocalPath == null) {
                        downloadAttachmentIfNeeded(dto.id, dto.mediaAttachmentId)
                    }

                    // If incoming and status is DELIVERED, send READ receipt
                    if (!isOutgoing && dto.status != "READ") {
                        sendReceipt(dto.id, "READ")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Sync messages failed: ${e.message}")
        }
    }

    suspend fun sendReceipt(messageId: String, status: String) {
        try {
            apiService.updateReceipt(ReceiptUpdateRequestDto(messageId, status))
            messageDao.updateStatus(messageId, status)
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
        // Mark deleted locally immediately for smooth UI feedback
        messageDao.markDeleted(messageId)

        // Resolve target server ID if messageId is local tempId or clientMessageId
        val msg = messageDao.getMessageById(messageId)
        val targetServerId = msg?.id ?: messageId

        try {
            val response = apiService.deleteMessage(targetServerId)
            if (response.isSuccessful) {
                Log.i(tag, "Successfully deleted message $targetServerId on server")
            } else {
                Log.w(tag, "Delete message server responded with code ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(tag, "Delete message API call failed: ${e.message}")
        }
    }

    fun sendTyping(isTyping: Boolean) {
        webSocketClient.sendEvent(if (isTyping) "TYPING_START" else "TYPING_STOP", mapOf("typing" to isTyping))
    }

    private suspend fun handleIncomingWsEvent(event: WsEventDto<Any>) {
        val pairId = keyStoreManager.getPairId() ?: return
        val aesKey = keyStoreManager.getSharedSessionAesKey()

        // Any event from partner implies partner is currently online
        if (event.eventType != "PRESENCE") {
            conversationDao.updatePresence(pairId, true, System.currentTimeMillis())
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
                    pairId = pairId,
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

                // Auto download media attachment if received
                if (!msgDto.mediaAttachmentId.isNullOrBlank()) {
                    downloadAttachmentIfNeeded(msgDto.id, msgDto.mediaAttachmentId)
                }

                // Check for love emoji in incoming message
                if (com.twocall.chat.ui.components.containsLoveEmoji(decrypted)) {
                    _loveAnimationEvents.tryEmit(Unit)
                }

                // Send DELIVERED receipt
                sendReceipt(msgDto.id, "DELIVERED")
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

            "TYPING_START" -> conversationDao.updateTyping(pairId, true)
            "TYPING_STOP" -> conversationDao.updateTyping(pairId, false)

            "PRESENCE" -> {
                val payloadJson = gson.toJson(event.payload)
                val data = gson.fromJson(payloadJson, Map::class.java)
                val eventName = data["event"] as? String

                if (eventName == "PAIR_DELETED" || eventName == "PARTNER_DISCONNECTED") {
                    Log.i(tag, "Pair terminated by partner: $eventName")
                    onPairTerminated?.invoke()
                    return
                }

                val online = data["online"] as? Boolean ?: false
                conversationDao.updatePresence(pairId, online, System.currentTimeMillis())

                if (eventName == "PAIRING_COMPLETE") {
                    val partnerDevId = data["partnerDeviceId"] as? String
                    val partnerKey = data["partnerPublicKey"] as? String
                    if (partnerDevId != null && partnerKey != null) {
                        keyStoreManager.savePartnerInfo(partnerDevId, partnerKey)
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
