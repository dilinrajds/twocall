package com.twocall.chat.worker

import android.content.Context
import androidx.work.*
import com.twocall.chat.ChatApplication

class MessageSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as ChatApplication
        return try {
            app.chatRepository.syncMessagesForAllPairs(strict = true)
            Result.success()
        } catch (e: Exception) { Result.retry() }
    }
    companion object {
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<MessageSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()
            WorkManager.getInstance(context).enqueueUniqueWork("incoming-message-sync", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
