package com.aistudio.jarvis.voiceagent.data.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * JarvisWatchdogWorker — WorkManager periodic task.
 *
 * WHY THIS EXISTS:
 * On Funtouch OS (iQOO/vivo), Android aggressively kills foreground services
 * after a few minutes of inactivity. Even with START_STICKY, AlarmManager restart,
 * and battery optimization disabled, the OS sometimes prevents restart.
 *
 * WorkManager is specifically designed by Google to survive these restrictions.
 * It runs even when the app is killed, even after battery saver kicks in,
 * and it persists across reboots automatically.
 *
 * This worker fires every 15 minutes (the minimum WorkManager interval),
 * checks if JarvisPhoneMonitorService is alive, and restarts it if not.
 */
class JarvisWatchdogWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "JarvisWatchdog"
        private const val WORK_NAME = "jarvis_watchdog_work"

        /**
         * Schedule or replace the periodic watchdog.
         * Safe to call multiple times — idempotent.
         */
        fun schedule(context: Context) {
            try {
                val request = PeriodicWorkRequestBuilder<JarvisWatchdogWorker>(
                    15, TimeUnit.MINUTES
                ).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,  // Don't reset if already scheduled
                    request
                )
                Log.i(TAG, "✅ Watchdog scheduled (15 min interval)")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to schedule watchdog", e)
            }
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "⏰ Watchdog triggered — checking JarvisPhoneMonitorService status")

            if (!JarvisPhoneMonitorService.isRunning) {
                Log.w(TAG, "⚠️ JarvisPhoneMonitorService is NOT running — restarting now")
                JarvisPhoneMonitorService.start(context)
            } else {
                Log.d(TAG, "✅ JarvisPhoneMonitorService is running — no action needed")
            }

            Result.success()
        } catch (e: Throwable) {
            Log.e(TAG, "Watchdog encountered an error", e)
            Result.retry()
        }
    }
}
