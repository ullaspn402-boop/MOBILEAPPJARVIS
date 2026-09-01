package com.aistudio.jarvis.voiceagent.data.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CapturedNotification(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postTime: Long = System.currentTimeMillis()
)

class JarvisNotificationListenerService : NotificationListenerService() {

    companion object {
        private val _recentNotifications = MutableStateFlow<List<CapturedNotification>>(emptyList())
        val recentNotifications: StateFlow<List<CapturedNotification>> = _recentNotifications.asStateFlow()

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        fun isNotificationAccessGranted(context: Context): Boolean {
            return try {
                val packageName = context.packageName
                val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                flat != null && flat.contains(packageName)
            } catch (e: Throwable) {
                false
            }
        }

        fun openNotificationAccessSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Throwable) {
                // Ignore settings intent failure
            }
        }
    }

    override fun onListenerConnected() {
        try {
            super.onListenerConnected()
            _isServiceConnected.value = true
            fetchActiveNotifications()
        } catch (e: Throwable) {
            // Ignore connection errors
        }
    }

    override fun onListenerDisconnected() {
        try {
            super.onListenerDisconnected()
            _isServiceConnected.value = false
        } catch (e: Throwable) {
            // Ignore disconnection errors
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            super.onNotificationPosted(sbn)
            sbn?.let { addNotification(it) }
        } catch (e: Throwable) {
            // Ignore notification posting errors
        }
    }

    private fun addNotification(sbn: StatusBarNotification) {
        try {
            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            
            if (title.isBlank() && text.isBlank()) return
            // Ignore self notifications
            if (sbn.packageName == packageName) return

            val appName = try {
                val pm = packageManager
                val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Throwable) {
                sbn.packageName.substringAfterLast('.')
            }

            val captured = CapturedNotification(
                id = "${sbn.id}_${sbn.postTime}",
                packageName = sbn.packageName,
                appName = appName,
                title = title,
                text = text,
                postTime = sbn.postTime
            )

            val current = _recentNotifications.value.toMutableList()
            current.removeAll { it.id == captured.id }
            current.add(0, captured)
            if (current.size > 30) {
                _recentNotifications.value = current.take(30)
            } else {
                _recentNotifications.value = current
            }
        } catch (e: Throwable) {
            // Ignore notification processing errors caused by custom parcelables or dead objects
        }
    }

    private fun fetchActiveNotifications() {
        try {
            val active = activeNotifications ?: return
            val list = mutableListOf<CapturedNotification>()
            for (sbn in active) {
                val extras = sbn.notification?.extras ?: continue
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                if (title.isBlank() && text.isBlank()) continue
                if (sbn.packageName == packageName) continue

                val appName = try {
                    val pm = packageManager
                    val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Throwable) {
                    sbn.packageName.substringAfterLast('.')
                }

                list.add(
                    CapturedNotification(
                        id = "${sbn.id}_${sbn.postTime}",
                        packageName = sbn.packageName,
                        appName = appName,
                        title = title,
                        text = text,
                        postTime = sbn.postTime
                    )
                )
            }
            _recentNotifications.value = list.sortedByDescending { it.postTime }.take(30)
        } catch (e: Throwable) {
            // Ignore
        }
    }
}
