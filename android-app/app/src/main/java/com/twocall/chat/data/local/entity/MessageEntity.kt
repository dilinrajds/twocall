package com.twocall.chat.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["clientMessageId"], unique = true),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val pairId: String,
    val senderDeviceId: String,
    val clientMessageId: String,
    val plaintext: String,
    val ciphertext: String,
    val iv: String,
    val messageType: String,
    val replyToMessageId: String? = null,
    val attachmentLocalPath: String? = null,
    val attachmentRemoteId: String? = null,
    val attachmentFileName: String? = null,
    val status: String, // PENDING, SENT, DELIVERED, READ, FAILED
    val isOutgoing: Boolean,
    val timestamp: Long,
    val reactionEmoji: String? = null,
    val isDeleted: Boolean = false
)
