package com.aistudio.jarvis.voiceagent.data.service

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

/**
 * JarvisCallScreeningService — Official Android 10+ CallScreeningService API.
 *
 * WHY THIS IS CRITICAL:
 * On Android 10+ (API 29+), Android hides the caller phone number in standard PHONE_STATE broadcasts.
 * CallScreeningService is Google's official API for Call Assistants.
 * When a call arrives, Android OS calls onScreenCall() and passes the exact incoming phone number
 * in callDetails.handle.schemeSpecificPart BEFORE the first ring!
 *
 * Registered in AndroidManifest.xml under BIND_SCREENING_SERVICE.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class JarvisCallScreeningService : CallScreeningService() {

    private val tag = "JarvisCallScreening"

    override fun onScreenCall(callDetails: Call.Details) {
        try {
            val handle = callDetails.handle
            val rawNumber = handle?.schemeSpecificPart ?: ""
            Log.i(tag, "📞 Incoming call via CallScreeningService! Number: ${rawNumber.ifBlank { "(hidden)" }}")

            // CRITICAL: Store the real number BEFORE starting SmartCallService.
            // TelephonyCallback may fire simultaneously with "" (Android 12+ privacy).
            // By storing it here first, handleIncomingCall's dedup logic will always
            // use the real number even if the empty-number event arrives first.
            if (rawNumber.isNotBlank()) {
                SmartCallService.lastKnownCallerNumber = rawNumber
                Log.i(tag, "✅ Stored real caller number in lastKnownCallerNumber")
            }

            // Send exact caller number to SmartCallService for caller identification & announcement
            val serviceIntent = Intent(this, SmartCallService::class.java).apply {
                action = SmartCallService.ACTION_INCOMING_CALL
                putExtra(SmartCallService.EXTRA_CALLER_NUMBER, rawNumber)
            }
            ContextCompat.startForegroundService(this, serviceIntent)

        } catch (e: Throwable) {
            Log.e(tag, "Error in CallScreeningService", e)
        } finally {
            // Allow the call to ring normally without blocking
            try {
                val response = CallResponse.Builder().build()
                respondToCall(callDetails, response)
            } catch (e: Throwable) {
                Log.e(tag, "Error responding to call in CallScreeningService", e)
            }
        }
    }
}
