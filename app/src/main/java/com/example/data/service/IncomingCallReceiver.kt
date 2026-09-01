package com.aistudio.jarvis.voiceagent.data.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Listens for incoming phone call state changes.
 *
 * Registered in AndroidManifest.xml for:
 *   - android.intent.action.PHONE_STATE
 *
 * Requires permission: READ_PHONE_STATE
 *
 * On RINGING state → starts [SmartCallService] as a foreground service.
 * On IDLE/OFFHOOK → notifies [SmartCallService] that the call ended.
 *
 * NOTE: goAsync() is used so that Android gives the receiver extra time
 * (10 seconds instead of the default 5 seconds) to start the foreground service.
 * This is critical on Funtouch OS / iQOO devices with aggressive background restrictions.
 */
class IncomingCallReceiver : BroadcastReceiver() {

    private val tag = "IncomingCallReceiver"

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        // Check READ_PHONE_STATE permission before accessing phone state
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(tag, "READ_PHONE_STATE permission not granted — cannot handle phone state broadcast")
            return
        }

        // Use goAsync() to get more time for startForegroundService on strict OS variants
        val pendingResult = goAsync()

        try {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: run {
                pendingResult.finish()
                return
            }

            // On Android 10+ EXTRA_INCOMING_NUMBER may be null unless READ_CALL_LOG is granted.
            @Suppress("DEPRECATION")
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""

            Log.d(tag, "Phone state: $state | Number: ${if (incomingNumber.isBlank()) "(hidden)" else incomingNumber}")

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    Log.i(tag, "📞 Incoming call — starting SmartCallService")
                    startSmartCallService(context, SmartCallService.ACTION_INCOMING_CALL, incomingNumber)
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    Log.d(tag, "Call picked up (OFFHOOK)")
                    startSmartCallService(context, SmartCallService.ACTION_CALL_ANSWERED, incomingNumber)
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    Log.d(tag, "Call ended (IDLE)")
                    startSmartCallService(context, SmartCallService.ACTION_CALL_ENDED, incomingNumber)
                }
            }
        } catch (e: Throwable) {
            Log.e(tag, "IncomingCallReceiver error", e)
        } finally {
            pendingResult.finish()
        }
    }

    private fun startSmartCallService(context: Context, action: String, number: String) {
        try {
            val serviceIntent = Intent(context, SmartCallService::class.java).apply {
                this.action = action
                putExtra(SmartCallService.EXTRA_CALLER_NUMBER, number)
            }
            // startForegroundService so the service must call startForeground() within 5 seconds
            ContextCompat.startForegroundService(context, serviceIntent)
            Log.i(tag, "✅ SmartCallService start requested: $action")
        } catch (e: Throwable) {
            Log.e(tag, "❌ Could not start SmartCallService: ${e.message}", e)
        }
    }
}
