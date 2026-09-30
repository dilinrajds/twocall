package com.twocall.chat.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.twocall.chat.ChatApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class RenderKeepAliveWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val tag = "RenderKeepAliveWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val app = applicationContext as? ChatApplication ?: return@withContext Result.success()
            val response = app.apiClient.apiService.getPairInfo()
            Log.i(tag, "Render backend keep-alive ping sent, status=${response.code()}")
            Result.success()
        } catch (e: Exception) {
            Log.w(tag, "Render backend keep-alive ping exception: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "twocall_render_keepalive"

        fun schedulePeriodicKeepAlive(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<RenderKeepAliveWorker>(12, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.i("RenderKeepAliveWorker", "Scheduled 12-minute periodic keep-alive worker to prevent Render inactivity")
        }
    }
}
