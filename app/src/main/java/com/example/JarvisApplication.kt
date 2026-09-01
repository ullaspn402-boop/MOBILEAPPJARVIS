package com.aistudio.jarvis.voiceagent

import android.app.Application
import android.os.Looper
import android.util.Log
import com.aistudio.jarvis.voiceagent.data.service.JarvisPhoneMonitorService

class JarvisApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            setupGlobalExceptionHandler()
        } catch (t: Throwable) {
            Log.e("JarvisApplication", "Could not install exception handler", t)
        }

        // Start the persistent phone monitor service immediately.
        // This is the ONLY reliable way to detect calls on Funtouch OS (iQOO/vivo)
        // because the OS silently blocks PHONE_STATE broadcasts from background receivers.
        try {
            JarvisPhoneMonitorService.start(this)
            com.aistudio.jarvis.voiceagent.data.service.JarvisWatchdogWorker.schedule(this)
            Log.i("JarvisApplication", "✅ JarvisPhoneMonitorService started & Watchdog scheduled")
        } catch (t: Throwable) {
            Log.e("JarvisApplication", "Could not start JarvisPhoneMonitorService", t)
        }
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e(
                    "JarvisApplication",
                    "Uncaught exception on thread '${thread.name}': ${throwable.message}",
                    throwable
                )
            } catch (_: Throwable) {
            }

            val isMainThread = try {
                thread == Looper.getMainLooper().thread
            } catch (_: Throwable) {
                true
            }

            // Always keep Error / main-thread crashes fatal so the process does not hang.
            // Background worker crashes should not close the app.
            if (throwable is Error || isMainThread) {
                try {
                    defaultHandler?.uncaughtException(thread, throwable)
                } catch (e: Throwable) {
                    Log.e("JarvisApplication", "Default exception handler failed", e)
                }
            } else {
                Log.w(
                    "JarvisApplication",
                    "Absorbed non-fatal background exception: ${throwable.javaClass.simpleName}"
                )
            }
        }
    }
}

