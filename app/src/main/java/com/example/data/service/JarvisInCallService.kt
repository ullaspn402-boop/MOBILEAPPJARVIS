package com.aistudio.jarvis.voiceagent.data.service

import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

/**
 * Jarvis InCallService — only active when Jarvis is the default phone/dialer application.
 *
 * Provides full programmatic call control:
 *   - Accept a ringing call
 *   - Disconnect an active or ringing call
 *   - Hold a call
 *
 * IMPORTANT:
 *   - This service must be declared in AndroidManifest.xml with
 *     android.permission.BIND_INCALL_SERVICE.
 *   - It is ONLY bound by the system when Jarvis has the ROLE_DIALER role.
 *   - When Jarvis is NOT the default dialer, [instance] will be null and
 *     [SmartCallService] falls back to TelecomManager.acceptRingingCall().
 *
 * Android version requirement: API 23+ (InCallService), full control API 28+.
 */
class JarvisInCallService : InCallService() {

    companion object {
        private val tag = "JarvisInCallService"

        /** Non-null only when the system has bound this service (i.e. default dialer role active). */
        var instance: JarvisInCallService? = null
            private set
    }

    private var currentCall: Call? = null

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        Log.i(tag, "Call added: state=${call.state}")

        // Register a state callback so we know when the call transitions
        call.registerCallback(callCallback)

        // Notify SmartCallService (it will decide whether to handle this call)
        // The IncomingCallReceiver already fired, so SmartCallService is likely running.
        // The service will call acceptCurrentCall() / disconnectCurrentCall() on us.
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.i(tag, "Call removed")
        call.unregisterCallback(callCallback)
        if (currentCall == call) currentCall = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(tag, "JarvisInCallService created — Jarvis is the default dialer")
    }

    override fun onDestroy() {
        instance = null
        currentCall = null
        super.onDestroy()
        Log.i(tag, "JarvisInCallService destroyed")
    }

    // ─── Public API used by SmartCallService ──────────────────────────────────

    /**
     * Accept the current ringing call.
     * No-op if there is no ringing call.
     */
    fun acceptCurrentCall() {
        val call = currentCall ?: run {
            Log.w(tag, "acceptCurrentCall: no current call")
            return
        }
        try {
            if (call.state == Call.STATE_RINGING) {
                call.answer(0)
                Log.i(tag, "Call answered via InCallService")
            }
        } catch (e: Throwable) {
            Log.e(tag, "Error answering call", e)
        }
    }

    /**
     * Disconnect the current call (decline if ringing, end if active).
     */
    fun disconnectCurrentCall() {
        val call = currentCall ?: run {
            Log.w(tag, "disconnectCurrentCall: no current call")
            return
        }
        try {
            call.disconnect()
            Log.i(tag, "Call disconnected via InCallService")
        } catch (e: Throwable) {
            Log.e(tag, "Error disconnecting call", e)
        }
    }

    // ─── Call state callback ──────────────────────────────────────────────────

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            Log.d(tag, "Call state changed: $state")
            // SmartCallService observes phone state via IncomingCallReceiver,
            // so no additional action needed here for the prototype.
        }
    }
}
