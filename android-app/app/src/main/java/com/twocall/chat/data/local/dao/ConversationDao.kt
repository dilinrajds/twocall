package com.twocall.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.twocall.chat.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("UPDATE conversations SET partnerDisplayName = :name, partnerImageBase64 = :image WHERE pairId = :pairId")
    suspend fun updateProfile(pairId: String, name: String, image: String?)

    /** All conversations ordered by most recent message / activity */
    @Query("SELECT * FROM conversations WHERE isActive = 1 ORDER BY lastMessageTimestamp DESC, lastActiveTimestamp DESC")
    fun getAllConversationsFlow(): Flow<List<ConversationEntity>>

    /** Single conversation by pairId */
    @Query("SELECT * FROM conversations WHERE pairId = :pairId LIMIT 1")
    fun getConversationFlowByPairId(pairId: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE pairId = :pairId LIMIT 1")
    suspend fun getConversationByPairId(pairId: String): ConversationEntity?

    /** Legacy compat: returns the most recently active conversation */
    @Query("SELECT * FROM conversations WHERE isActive = 1 ORDER BY lastMessageTimestamp DESC LIMIT 1")
    fun getConversationFlow(): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE isActive = 1 ORDER BY lastMessageTimestamp DESC LIMIT 1")
    suspend fun getConversation(): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: ConversationEntity)

    @Query("UPDATE conversations SET partnerOnline = :online, lastActiveTimestamp = :timestamp WHERE pairId = :pairId")
    suspend fun updatePresence(pairId: String, online: Boolean, timestamp: Long)

    @Query("UPDATE conversations SET partnerTyping = :typing WHERE pairId = :pairId")
    suspend fun updateTyping(pairId: String, typing: Boolean)

    @Query("UPDATE conversations SET lastMessagePreview = :preview, lastMessageTimestamp = :timestamp WHERE pairId = :pairId")
    suspend fun updateLastMessage(pairId: String, preview: String, timestamp: Long)

    @Query("UPDATE conversations SET unreadCount = unreadCount + 1 WHERE pairId = :pairId")
    suspend fun incrementUnread(pairId: String)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE pairId = :pairId")
    suspend fun clearUnread(pairId: String)

    @Query("UPDATE conversations SET partnerDisplayName = :name WHERE pairId = :pairId")
    suspend fun updateDisplayName(pairId: String, name: String)

    @Query("DELETE FROM conversations WHERE pairId = :pairId")
    suspend fun deleteByPairId(pairId: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}
