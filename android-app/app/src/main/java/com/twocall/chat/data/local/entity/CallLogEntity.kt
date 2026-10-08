package com.twocall.chat.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted call log entry per conversation.
 *
 * direction: INCOMING | OUTGOING
 * callType:  AUDIO | VIDEO
 * status:    ANSWERED | MISSED | DECLINED | CANCELLED | FAILED
 * durationSeconds: measured from answeredAt → endedAt (0 if not answered)
 */
@Entity(
    tableName = "call_logs",
    indices = [Index(value = ["pairId"]), Index(value = ["startedAt"])]
)
data class CallLogEntity(
    @PrimaryKey
    val callId: String,
    val pairId: String,
    val partnerDeviceId: String?,
    val callType: String,       // AUDIO | VIDEO
    val direction: String,      // INCOMING | OUTGOING
    val status: String,         // ANSWERED | MISSED | DECLINED | CANCELLED | FAILED
    val startedAt: Long,
    val answeredAt: Long?,
    val endedAt: Long?,
    val durationSeconds: Long   // 0 if not answered
)
