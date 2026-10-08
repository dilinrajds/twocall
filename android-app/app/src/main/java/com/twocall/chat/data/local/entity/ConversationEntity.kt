package com.twocall.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val pairId: String,
    val partnerDeviceId: String? = null,
    val partnerDisplayName: String? = null,
    val partnerImageBase64: String? = null,
    val partnerOnline: Boolean = false,
    val partnerTyping: Boolean = false,
    val lastActiveTimestamp: Long = 0L,
    val lastMessagePreview: String? = null,
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    /** Encrypted access credentials (stored in EncryptedSharedPreferences separately,
     * but pairId links them). This field is informational only. */
    val isActive: Boolean = true
)
