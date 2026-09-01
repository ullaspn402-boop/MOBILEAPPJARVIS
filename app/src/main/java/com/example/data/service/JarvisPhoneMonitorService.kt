package com.aistudio.jarvis.voiceagent.data.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aistudio.jarvis.voiceagent.MainActivity
import com.aistudio.jarvis.voiceagent.R
import com.aistudio.jarvis.voiceagent.data.call.CallerIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * JarvisPhoneMonitorService — Persistent foreground service for call detection.
 *
 * WHY THIS EXISTS:
 * On Funtouch OS (iQOO/vivo) and many custom Android ROMs, the system
 * SILENTLY BLOCKS BroadcastReceiver for PHONE_STATE even when:
 *   - Autostart is ON
 *   - READ_PHONE_STATE is granted
 *   - android:exported="true" is set
 *
 * The ONLY reliable solution is a persistent foreground service that registers
 * TelephonyCallback (Android 12+) or PhoneStateListener (older) DIRECTLY.
 * Since this service is already running when the call arrives, the OS cannot
 * block it from detecting the call state change.
 *
 * Started from JarvisApplication.onCreate() so it is always running.
 */
class JarvisPhoneMonitorService : Service() {

    companion object {
        private const val CHANNEL_ID = "jarvis_monitor_channel"
        private const val NOTIF_ID = 9002
        private const val TAG = "JarvisPhoneMonitor"

        var isRunning = false
            private set

        fun start(context: Context) {
            if (isRunning) return
            try {
                val intent = Intent(context, JarvisPhoneMonitorService::class.java)
                ContextCompat.startForegroundService(context, intent)
                Log.i(TAG, "✅ JarvisPhoneMonitorService start requested")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to start JarvisPhoneMonitorService", e)
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var telephonyManager: TelephonyManager? = null
    private var voiceEngine: JarvisVoiceEngine? = null
    private var callerIdentifier: CallerIdentifier? = null
    private var announceJob: Job? = null

    // Track last call state to avoid duplicate announcements
    private var lastCallState = TelephonyManager.CALL_STATE_IDLE
    private var lastIncomingNumber = ""

    // TelephonyCallback for Android 12+
    private var telephonyCallback: Any? = null

    // PhoneStateListener for Android < 12
    @Suppress("DEPRECATION")
    private val legacyPhoneStateListener = object : PhoneStateListener() {
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            handleCallStateChange(state, phoneNumber ?: "")
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIF_ID,
                    buildNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIF_ID, buildNotification())
            }
            Log.i(TAG, "✅ JarvisPhoneMonitorService started foreground successfully")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed startForeground in JarvisPhoneMonitorService", e)
        }

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        voiceEngine = JarvisVoiceEngine(applicationContext, scope)
        callerIdentifier = CallerIdentifier(applicationContext)

        registerPhoneListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Restart if killed by OS
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        unregisterPhoneListener()
        voiceEngine?.destroy()
        scope.cancel()
        Log.i(TAG, "JarvisPhoneMonitorService destroyed")
    }

    // ─── Phone State Registration ─────────────────────────────────────────────

    private fun registerPhoneListener() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "READ_PHONE_STATE not granted — call detection disabled")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ — use TelephonyCallback (modern, reliable)
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        // TelephonyCallback on Android 12+ doesn't provide the number
                        // for privacy reasons — we use CallerIdentifier to look it up
                        handleCallStateChange(state, "")
                    }
                }
                telephonyCallback = callback
                telephonyManager?.registerTelephonyCallback(
                    mainExecutor,
                    callback
                )
                Log.i(TAG, "✅ TelephonyCallback registered (Android 12+)")
            } else {
                // Android < 12 — use PhoneStateListener
                @Suppress("DEPRECATION")
                telephonyManager?.listen(
                    legacyPhoneStateListener,
                    PhoneStateListener.LISTEN_CALL_STATE
                )
                Log.i(TAG, "✅ PhoneStateListener registered (Android < 12)")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to register phone listener", e)
        }
    }

    private fun unregisterPhoneListener() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val cb = telephonyCallback
                if (cb != null && cb is TelephonyCallback) {
                    telephonyManager?.unregisterTelephonyCallback(cb)
                }
            } else {
                @Suppress("DEPRECATION")
                telephonyManager?.listen(legacyPhoneStateListener, PhoneStateListener.LISTEN_NONE)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error unregistering phone listener", e)
        }
    }

    // ─── Call State Handling ──────────────────────────────────────────────────

    private fun handleCallStateChange(state: Int, incomingNumber: String) {
        Log.d(TAG, "📞 Call state changed: $state | number: ${incomingNumber.ifBlank { "(hidden)" }}")

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                if (lastCallState != TelephonyManager.CALL_STATE_RINGING) {
                    lastCallState = state
                    lastIncomingNumber = incomingNumber
                    Log.i(TAG, "📞 RINGING detected — delegating to SmartCallService")

                    // Start SmartCallService for full caller identification, announcement & voice command handling
                    startSmartCallService(SmartCallService.ACTION_INCOMING_CALL, incomingNumber)
                }
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (lastCallState != TelephonyManager.CALL_STATE_OFFHOOK) {
                    lastCallState = state
                    announceJob?.cancel()
                    voiceEngine?.stopSpeaking()
                    startSmartCallService(SmartCallService.ACTION_CALL_ANSWERED, lastIncomingNumber)
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (lastCallState != TelephonyManager.CALL_STATE_IDLE) {
                    lastCallState = state
                    announceJob?.cancel()
                    voiceEngine?.stopSpeaking()
                    lastIncomingNumber = ""
                    startSmartCallService(SmartCallService.ACTION_CALL_ENDED, "")
                }
            }
        }
    }

    private fun announceCaller(number: String) {
        announceJob?.cancel()
        announceJob = scope.launch {
            try {
                // Identify caller name from contacts
                val callerInfo = withContext(Dispatchers.IO) {
                    callerIdentifier?.identify(number)
                }
                val who = callerInfo?.announcementLabel() ?: "an unknown number"
                val announcement = "Ullas, $who is calling you."

                Log.i(TAG, "🔊 Announcing: $announcement")

                // Wait for TTS to be ready (max 3 seconds)
                var waited = 0
                while (voiceEngine?.isTtsReady?.value != true && waited < 3000) {
                    delay(100)
                    waited += 100
                }

                voiceEngine?.speak(announcement)
            } catch (e: Throwable) {
                Log.e(TAG, "Error announcing caller", e)
            }
        }
    }

    private fun startSmartCallService(action: String, number: String) {
        try {
            val intent = Intent(this, SmartCallService::class.java).apply {
                this.action = action
                putExtra(SmartCallService.EXTRA_CALLER_NUMBER, number)
            }
            ContextCompat.startForegroundService(this, intent)
        } catch (e: Throwable) {
            Log.e(TAG, "Could not start SmartCallService: ${e.message}")
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.i(TAG, "Task removed (app swiped away) — scheduling auto-restart for background operation")
        try {
            val restartIntent = Intent(applicationContext, JarvisPhoneMonitorService::class.java)
            val pendingIntent = android.app.PendingIntent.getService(
                applicationContext, 1, restartIntent,
                android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pendingIntent)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed scheduling auto-restart on task removed", e)
        }
    }

    // ─── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jarvis Background Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Jarvis active background call assistant"
                setShowBadge(false)
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("🤖 Jarvis Assistant Active")
            .setContentText("Listening for incoming calls in background")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
