package com.twocall.chat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.twocall.chat.data.local.dao.CallLogDao
import com.twocall.chat.data.local.dao.ConversationDao
import com.twocall.chat.data.local.dao.MessageDao
import com.twocall.chat.data.local.entity.CallLogEntity
import com.twocall.chat.data.local.entity.ConversationEntity
import com.twocall.chat.data.local.entity.MessageEntity

@Database(
    entities = [MessageEntity::class, ConversationEntity::class, CallLogEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun callLogDao(): CallLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "twocall_chat.db"
                ).addMigrations(object : androidx.room.migration.Migration(1, 2) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE conversations_new (pairId TEXT NOT NULL PRIMARY KEY, partnerDeviceId TEXT, partnerDisplayName TEXT, partnerOnline INTEGER NOT NULL, partnerTyping INTEGER NOT NULL, lastActiveTimestamp INTEGER NOT NULL, lastMessagePreview TEXT, lastMessageTimestamp INTEGER NOT NULL, unreadCount INTEGER NOT NULL, isActive INTEGER NOT NULL)")
                        db.execSQL("INSERT INTO conversations_new SELECT pairId, partnerDeviceId, NULL, partnerOnline, partnerTyping, lastActiveTimestamp, NULL, 0, 0, 1 FROM conversations")
                        db.execSQL("DROP TABLE conversations")
                        db.execSQL("ALTER TABLE conversations_new RENAME TO conversations")
                        db.execSQL("CREATE TABLE IF NOT EXISTS call_logs (callId TEXT NOT NULL PRIMARY KEY, pairId TEXT NOT NULL, partnerDeviceId TEXT, callType TEXT NOT NULL, direction TEXT NOT NULL, status TEXT NOT NULL, startedAt INTEGER NOT NULL, answeredAt INTEGER, endedAt INTEGER, durationSeconds INTEGER NOT NULL)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_call_logs_pairId ON call_logs (pairId)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_call_logs_startedAt ON call_logs (startedAt)")
                    }
                }, object : androidx.room.migration.Migration(2, 3) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE conversations ADD COLUMN partnerImageBase64 TEXT")
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
