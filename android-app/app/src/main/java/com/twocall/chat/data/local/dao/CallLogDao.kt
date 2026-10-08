package com.twocall.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.twocall.chat.data.local.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(callLog: CallLogEntity)

    @Query("SELECT * FROM call_logs WHERE pairId = :pairId ORDER BY startedAt ASC")
    fun getCallLogsForPair(pairId: String): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE pairId = :pairId ORDER BY startedAt DESC LIMIT 50")
    suspend fun getRecentCallLogs(pairId: String): List<CallLogEntity>

    @Query("DELETE FROM call_logs WHERE pairId = :pairId")
    suspend fun deleteForPair(pairId: String)

    @Query("DELETE FROM call_logs")
    suspend fun deleteAll()
}
