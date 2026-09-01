package com.aistudio.jarvis.voiceagent.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Restarts JarvisPhoneMonitorService after device reboot or app update.
 * Without this, the persistent service would not survive a phone restart
 * on Funtouch OS / iQOO devices.
 */
class JarvisBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i("JarvisBootReceiver", "Boot/update detected — restarting JarvisPhoneMonitorService & scheduling Watchdog")
            JarvisPhoneMonitorService.start(context)
            JarvisWatchdogWorker.schedule(context)
        }
    }
}
