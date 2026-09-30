package com.twocall.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val pairId: String,
    val partnerDeviceId: String? = null,
    val partnerOnline: Boolean = false,
    val partnerTyping: Boolean = false,
    val lastActiveTimestamp: Long = 0L
)
