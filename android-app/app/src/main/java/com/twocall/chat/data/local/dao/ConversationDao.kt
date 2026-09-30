package com.twocall.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.twocall.chat.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations LIMIT 1")
    fun getConversationFlow(): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations LIMIT 1")
    suspend fun getConversation(): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: ConversationEntity)

    @Query("UPDATE conversations SET partnerOnline = :online, lastActiveTimestamp = :timestamp WHERE pairId = :pairId")
    suspend fun updatePresence(pairId: String, online: Boolean, timestamp: Long)

    @Query("UPDATE conversations SET partnerTyping = :typing WHERE pairId = :pairId")
    suspend fun updateTyping(pairId: String, typing: Boolean)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}
