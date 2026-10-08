package com.twocall.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.twocall.chat.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    /** All messages ordered by time (legacy: returns all pairs) */
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>

    /** Messages for a specific pair (conversation) */
    @Query("SELECT * FROM messages WHERE pairId = :pairId ORDER BY timestamp ASC")
    fun getMessagesFlowForPair(pairId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: String)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateStatus(messageId: String, status: String)

    @Query("UPDATE messages SET attachmentLocalPath = :path WHERE id = :messageId")
    suspend fun updateAttachmentLocalPath(messageId: String, path: String)

    @Query("UPDATE messages SET reactionEmoji = :emoji WHERE id = :messageId")
    suspend fun updateReaction(messageId: String, emoji: String?)

    @Query("UPDATE messages SET isDeleted = 1, plaintext = '' WHERE id = :messageId OR clientMessageId = :messageId")
    suspend fun markDeleted(messageId: String)

    @Query("SELECT * FROM messages WHERE status = 'PENDING' AND isOutgoing = 1 ORDER BY timestamp ASC")
    suspend fun getPendingOutgoingMessages(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :messageId OR clientMessageId = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("SELECT MAX(timestamp) FROM messages WHERE pairId = :pairId")
    suspend fun getLatestTimestampForPair(pairId: String): Long?

    @Query("SELECT MAX(timestamp) FROM messages")
    suspend fun getLatestMessageTimestamp(): Long?

    @Query("DELETE FROM messages WHERE pairId = :pairId")
    suspend fun deleteForPair(pairId: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()
}
