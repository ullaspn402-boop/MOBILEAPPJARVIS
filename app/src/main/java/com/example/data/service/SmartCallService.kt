package com.aistudio.jarvis.voiceagent.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aistudio.jarvis.voiceagent.MainActivity
import com.aistudio.jarvis.voiceagent.R
import com.aistudio.jarvis.voiceagent.data.call.CallAssistantSettings
import com.aistudio.jarvis.voiceagent.data.call.CallAssistantSettingsRepository
import com.aistudio.jarvis.voiceagent.data.call.CallCommand
import com.aistudio.jarvis.voiceagent.data.call.CallConversationAgent
import com.aistudio.jarvis.voiceagent.data.call.CallSummaryEntity
import com.aistudio.jarvis.voiceagent.data.call.CallSummaryManager
import com.aistudio.jarvis.voiceagent.data.call.CallerIdentifier
import com.aistudio.jarvis.voiceagent.data.call.CallerInfo
import com.aistudio.jarvis.voiceagent.data.call.CallState
import com.aistudio.jarvis.voiceagent.data.call.CallUrgency
import com.aistudio.jarvis.voiceagent.data.call.DeclineReason
import com.aistudio.jarvis.voiceagent.data.call.LanguageDetector
import com.aistudio.jarvis.voiceagent.data.call.UrgencyClassifier
import com.aistudio.jarvis.voiceagent.data.call.UrgencyResult
import com.aistudio.jarvis.voiceagent.data.call.VoiceCommandManager
import com.aistudio.jarvis.voiceagent.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Core foreground service that orchestrates the Jarvis Smart Call Assistant.
 *
 * Lifecycle:
 *   Started by [IncomingCallReceiver] when Android reports PHONE_STATE_RINGING.
 *   Manages the full call state machine from IDLE → SUMMARY → IDLE.
 *   Stops itself when the call ends and the summary is saved.
 *
 * IMPORTANT: This service runs in the background and must be declared in AndroidManifest.xml
 * as a foreground service with type="phoneCall".
 */
class SmartCallService : Service() {

    companion object {
        // Actions sent via Intent
        const val ACTION_INCOMING_CALL = "com.aistudio.jarvis.ACTION_INCOMING_CALL"
        const val ACTION_CALL_ANSWERED = "com.aistudio.jarvis.ACTION_CALL_ANSWERED"
        const val ACTION_CALL_ENDED = "com.aistudio.jarvis.ACTION_CALL_ENDED"
        const val ACTION_USER_COMMAND = "com.aistudio.jarvis.ACTION_USER_COMMAND"

        // Extras
        const val EXTRA_CALLER_NUMBER = "extra_caller_number"
        const val EXTRA_USER_COMMAND = "extra_user_command"

        // Broadcast sent to CallOverlayActivity
        const val BROADCAST_CALL_STATE = "com.aistudio.jarvis.CALL_STATE_CHANGED"
        const val BROADCAST_EXTRA_STATE = "broadcast_state"
        const val BROADCAST_EXTRA_CALLER_NAME = "broadcast_caller_name"
        const val BROADCAST_EXTRA_CALLER_NUMBER = "broadcast_caller_number"

        // Notification
        private const val CHANNEL_ID = "jarvis_call_channel"
        private const val NOTIF_ID = 9001

        // Singleton state observable — used by UI
        private val _callState = MutableStateFlow<CallState>(CallState.Idle)
        val callState: StateFlow<CallState> = _callState.asStateFlow()

        /**
         * The best known caller number for the current call session.
         * JarvisCallScreeningService writes the exact number here BEFORE
         * JarvisPhoneMonitorService fires with an empty string — so the
         * real number always wins over the empty fallback.
         */
        @Volatile var lastKnownCallerNumber: String = ""
    }

    private val tag = "SmartCallService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var voiceEngine: JarvisVoiceEngine
    private lateinit var callerIdentifier: CallerIdentifier
    private lateinit var settingsRepo: CallAssistantSettingsRepository
    private lateinit var database: AppDatabase

    private var currentCallerInfo: CallerInfo? = null
    private var settings: CallAssistantSettings = CallAssistantSettings()
    private var conversationAgent: CallConversationAgent? = null
    private var autoAnswerJob: Job? = null
    private var callStartTimeMs: Long = 0L
    private var isJarvisHandlingCall = false
    private var originalRingerVolume: Int = -1

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        Log.i(tag, "SmartCallService created")

        voiceEngine = JarvisVoiceEngine(applicationContext, serviceScope)
        callerIdentifier = CallerIdentifier(applicationContext)
        settingsRepo = CallAssistantSettingsRepository(applicationContext)
        database = AppDatabase.getDatabase(applicationContext)

        createNotificationChannel()

        // Load latest settings
        serviceScope.launch {
            settingsRepo.settings.collect { s ->
                settings = s
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ⚠️ CRITICAL: startForeground() MUST be called within 5 seconds on Android 8+
        // and within the SAME call stack as onStartCommand — do NOT put it inside a coroutine.
        // On Android 14 (API 34), failing to call this immediately causes ForegroundServiceDidNotStartInTimeException.
        try {
            val notification = buildNotification("Jarvis is listening for your calls")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIF_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIF_ID, notification)
            }
            Log.i(tag, "✅ Foreground service started successfully")
        } catch (e: Throwable) {
            Log.e(tag, "❌ Could not start foreground — service will be killed by OS", e)
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_INCOMING_CALL -> {
                val number = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: ""
                handleIncomingCall(number)
            }
            ACTION_CALL_ANSWERED -> {
                // User or system manually answered — cancel any pending Jarvis action
                if (!isJarvisHandlingCall) {
                    Log.d(tag, "Call answered manually — Jarvis stepping back")
                    cancelAutoAnswer()
                    restoreRingerVolume()
                    transitionTo(CallState.CallActive(currentCallerInfo ?: unknownCaller()))
                }
            }
            ACTION_CALL_ENDED -> {
                restoreRingerVolume()
                handleCallEnded()
            }
            ACTION_USER_COMMAND -> {
                val cmd = intent.getStringExtra(EXTRA_USER_COMMAND) ?: return START_NOT_STICKY
                handleUserCommand(cmd)
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            restoreRingerVolume()
            conversationAgent = null
            voiceEngine.destroy()
            serviceScope.cancel()
            Log.i(tag, "SmartCallService destroyed")
        } catch (e: Throwable) {
            Log.e(tag, "Error in onDestroy", e)
        }
    }

    // ─── State machine handlers ───────────────────────────────────────────────

    private fun handleIncomingCall(number: String) {
        if (!settings.smartCallAssistantEnabled) {
            Log.d(tag, "Smart Call Assistant disabled — not handling call")
            stopSelf()
            return
        }

        // Deduplication: if CallScreeningService already stored the real number,
        // and TelephonyCallback fires with "" (Android 12+ privacy), use the real number.
        val resolvedNumber = when {
            number.isNotBlank() -> {
                lastKnownCallerNumber = number  // Store the real number
                number
            }
            lastKnownCallerNumber.isNotBlank() -> {
                Log.d(tag, "Using lastKnownCallerNumber: ${lastKnownCallerNumber.takeLast(4).padStart(lastKnownCallerNumber.length, '*')}")
                lastKnownCallerNumber
            }
            else -> ""
        }

        // Prevent duplicate handling if we're already announcing this call
        if (_callState.value !is CallState.Idle && _callState.value !is CallState.CallEnded) {
            Log.d(tag, "Already handling a call — ignoring duplicate ACTION_INCOMING_CALL")
            return
        }

        serviceScope.launch {
            val callerInfo = withContext(Dispatchers.IO) {
                callerIdentifier.identify(resolvedNumber)
            }
            currentCallerInfo = callerInfo
            callStartTimeMs = System.currentTimeMillis()

            // Launch CallOverlayActivity HUD
            launchCallOverlay(callerInfo)

            transitionTo(CallState.IncomingCall(
                callerNumber = callerInfo.number,
                callerName = callerInfo.displayName,
                isKnownContact = callerInfo.isKnownContact
            ))

            // Duck ringer volume so SpeechRecognizer can hear user voice commands clearly
            duckRingerVolume()

            // Announce the caller
            announceCaller(callerInfo)
        }
    }

    private fun launchCallOverlay(callerInfo: CallerInfo) {
        try {
            val overlayIntent = Intent(applicationContext, com.aistudio.jarvis.voiceagent.ui.call.CallOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(BROADCAST_EXTRA_CALLER_NAME, callerInfo.displayName ?: callerInfo.number)
                putExtra(BROADCAST_EXTRA_CALLER_NUMBER, callerInfo.number)
            }
            applicationContext.startActivity(overlayIntent)
            Log.i(tag, "✅ Launched CallOverlayActivity HUD for incoming call")
        } catch (e: Throwable) {
            Log.e(tag, "Failed launching CallOverlayActivity", e)
        }
    }

    private fun duckRingerVolume() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null && originalRingerVolume == -1) {
                originalRingerVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                val targetVol = (maxVol * 0.25f).toInt().coerceAtLeast(1)
                audioManager.setStreamVolume(AudioManager.STREAM_RING, targetVol, 0)
                Log.i(tag, "🔉 Ducked ringer volume from $originalRingerVolume to $targetVol for voice recognition")
            }
        } catch (e: Throwable) {
            Log.w(tag, "Could not duck ringer volume", e)
        }
    }

    private fun restoreRingerVolume() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null && originalRingerVolume != -1) {
                audioManager.setStreamVolume(AudioManager.STREAM_RING, originalRingerVolume, 0)
                Log.i(tag, "🔊 Restored ringer volume to $originalRingerVolume")
                originalRingerVolume = -1
            }
        } catch (e: Throwable) {
            Log.w(tag, "Could not restore ringer volume", e)
        }
    }

    private fun announceCaller(callerInfo: CallerInfo) {
        transitionTo(CallState.AnnouncingCaller(callerInfo))
        broadcastCallState("ANNOUNCING", callerInfo)

        val announcement = buildAnnouncement(callerInfo)

        serviceScope.launch {
            // 1. Wait up to 3 seconds for TTS engine initialization
            var waitedMs = 0
            while (!voiceEngine.isTtsReady.value && waitedMs < 3000) {
                delay(100)
                waitedMs += 100
            }

            // 2. Speak announcement out loud
            voiceEngine.speak(announcement)

            // 3. Wait for TTS to start speaking
            delay(400)

            // 4. Wait until TTS finishes speaking completely
            while (voiceEngine.isSpeaking.value) {
                delay(200)
            }

            val autoDeadlineMs = if (settings.autoAnswerEnabled) {
                System.currentTimeMillis() + (settings.autoAnswerDelaySeconds * 1000L)
            } else null

            transitionTo(CallState.WaitingForCommand(callerInfo, autoDeadlineMs))
            broadcastCallState("WAITING", callerInfo)
            startListeningForUserCommand(callerInfo, autoDeadlineMs)
        }
    }

    private fun startListeningForUserCommand(callerInfo: CallerInfo, autoDeadlineMs: Long?) {
        // Configure voice engine for command mode
        voiceEngine.onSpeechRecognized = { text ->
            val command = VoiceCommandManager.parse(text)
            Log.d(tag, "User command recognized: '$text' → $command")
            executeCommand(command, callerInfo, text)
        }

        // Automatic re-listen loop during call ringing if silence or timeout occurs
        voiceEngine.onListeningCancelled = {
            if (_callState.value is CallState.WaitingForCommand) {
                serviceScope.launch {
                    delay(500)
                    if (_callState.value is CallState.WaitingForCommand) {
                        Log.d(tag, "Re-listening for user command during call ringing...")
                        voiceEngine.startListening()
                    }
                }
            }
        }

        voiceEngine.startListening()

        // Auto-answer countdown
        if (autoDeadlineMs != null) {
            autoAnswerJob = serviceScope.launch {
                val remaining = autoDeadlineMs - System.currentTimeMillis()
                if (remaining > 0) delay(remaining)
                // Only auto-answer if still waiting
                if (_callState.value is CallState.WaitingForCommand) {
                    Log.i(tag, "Auto-answer timer expired — Jarvis answering the call")
                    voiceEngine.stopListening()
                    executeCommand(CallCommand.JarvisAnswer, callerInfo, "auto-answer")
                }
            }
        }
    }

    private fun executeCommand(command: CallCommand, callerInfo: CallerInfo, rawText: String) {
        cancelAutoAnswer()
        restoreRingerVolume()
        voiceEngine.stopListening()

        when (command) {
            is CallCommand.PickUp -> userPickUp(callerInfo)
            is CallCommand.JarvisAnswer -> jarvisAnswerCall(callerInfo)
            is CallCommand.DoNotPickUp -> declineCall(callerInfo, DeclineReason.USER_COMMAND)
            is CallCommand.Ambiguous -> handleAmbiguousCommand(command, callerInfo)
            is CallCommand.Unknown -> {
                // Re-listen once before giving up
                voiceEngine.speak("Sorry, I didn't catch that. Pick up, answer them, or don't pick up?")
                serviceScope.launch {
                    delay(500)
                    while (voiceEngine.isSpeaking.value) delay(300)
                    voiceEngine.startListening()
                }
            }
        }
    }

    private fun userPickUp(callerInfo: CallerInfo) {
        Log.i(tag, "User says: pick up")
        transitionTo(CallState.CallActive(callerInfo))
        broadcastCallState("CALL_ACTIVE", callerInfo)

        // Accept the call via TelecomManager
        acceptCallProgrammatically()

        // Jarvis stays silent during the user's call
        voiceEngine.stopListening()
        voiceEngine.stopSpeaking()
        isJarvisHandlingCall = false
    }

    private fun jarvisAnswerCall(callerInfo: CallerInfo) {
        Log.i(tag, "Jarvis answering the call")
        isJarvisHandlingCall = true

        val speakerMode = settings.useSpeakerForJarvisAnswer
        transitionTo(CallState.JarvisAnswering(callerInfo, speakerMode))
        broadcastCallState("JARVIS_ANSWERING", callerInfo)

        acceptCallProgrammatically()

        // AUDIO ROUTING FIX: Use MODE_IN_COMMUNICATION (VoIP mode) so TTS audio
        // goes through the phone speaker AND is captured by the mic, which sends
        // it to the caller over the cellular uplink. This is the correct approach
        // for non-default-dialer apps to route audio to the caller.
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = true  // Speaker ON so mic captures TTS
            Log.i(tag, "🔊 Audio mode set to IN_COMMUNICATION + speakerphone ON for caller-side audio")
        } catch (e: Throwable) {
            Log.w(tag, "Failed setting audio mode for Jarvis answer", e)
        }

        // Give a moment for the call audio stream to stabilize
        serviceScope.launch {
            delay(1000)
            startJarvisConversation(callerInfo)
        }
    }

    private fun startJarvisConversation(callerInfo: CallerInfo) {
        // Configure voice engine for active call audio stream (USAGE_VOICE_COMMUNICATION + speakerphone)
        voiceEngine.setInCallAudioAttributes(true)

        val agent = CallConversationAgent(
            voiceEngine = voiceEngine,
            scope = serviceScope,
            userName = "Ullas", // Primary user name
            callerInfo = callerInfo,
            languageTag = "en-US",
            informCaller = settings.informCallerOfAi
        )
        conversationAgent = agent

        transitionTo(CallState.JarvisConversing(callerInfo))

        // Set up caller speech listening loop
        voiceEngine.onSpeechRecognized = { callerText ->
            agent.onCallerSpeechReceived(callerText)
            // Update language detection
            val langResult = LanguageDetector.detect(callerText)
            voiceEngine.setLanguage(langResult.languageTag)

            val state = _callState.value
            if (state is CallState.JarvisConversing) {
                transitionTo(state.copy(
                    turnCount = state.turnCount + 1,
                    transcriptSoFar = agent.getFullTranscript().joinToString("\n")
                ))
            }

            // After Jarvis speaks, listen again (if conversation not ended)
            serviceScope.launch {
                delay(500)
                while (voiceEngine.isSpeaking.value) delay(300)
                if (!agent.conversationEnded.value) {
                    voiceEngine.startListening()
                }
            }
        }

        agent.startConversation(onReadyToListen = {
            serviceScope.launch {
                if (!agent.conversationEnded.value) {
                    voiceEngine.startListening()
                }
            }
        })

        // Monitor conversation end
        serviceScope.launch {
            agent.conversationEnded.collect { ended ->
                if (ended) {
                    Log.i(tag, "Jarvis conversation ended")
                    handleCallEnded()
                }
            }
        }
    }

    private fun declineCall(callerInfo: CallerInfo, reason: DeclineReason) {
        Log.i(tag, "Declining call: $reason")
        transitionTo(CallState.Declining(callerInfo, reason))
        broadcastCallState("DECLINING", callerInfo)

        // Mute ringer volume instantly so ringing stops disturbing the user
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
        } catch (_: Throwable) {}

        // Cut/disconnect the call line programmatically
        endCallProgrammatically()

        // Send automatic predefined busy response to the caller if user said "don't pick up"
        if (reason == DeclineReason.USER_COMMAND && callerInfo.number.isNotBlank()) {
            sendPredefinedSmsResponse(callerInfo.number)
        }

        serviceScope.launch {
            delay(1000)
            transitionTo(CallState.CallDeclined(callerInfo))

            // Save a minimal summary for declined calls if user wants summaries
            if (settings.saveSummaries) {
                val summary = CallSummaryManager.buildDeclinedSummary(callerInfo)
                saveSummary(summary)
            }

            delay(1000)
            transitionTo(CallState.Idle)
            broadcastCallState("IDLE", callerInfo)
            stopSelf()
        }
    }

    private fun sendPredefinedSmsResponse(number: String) {
        if (number.isBlank()) return
        try {
            val message = "The user is currently busy. Please call again later."
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val smsManager = applicationContext.getSystemService(android.telephony.SmsManager::class.java)
                smsManager?.sendTextMessage(number, null, message, null, null)
            } else {
                @Suppress("DEPRECATION")
                val smsManager = android.telephony.SmsManager.getDefault()
                smsManager.sendTextMessage(number, null, message, null, null)
            }
            Log.i(tag, "Predefined busy SMS response sent to $number")
        } catch (e: SecurityException) {
            Log.w(tag, "SEND_SMS permission not granted — skipping automatic SMS response")
        } catch (e: Throwable) {
            Log.e(tag, "Error sending predefined SMS response", e)
        }
    }

    private fun handleAmbiguousCommand(command: CallCommand.Ambiguous, callerInfo: CallerInfo) {
        voiceEngine.speak("Did you want me to pick up the call, or not?")
        serviceScope.launch {
            delay(500)
            while (voiceEngine.isSpeaking.value) delay(300)

            // Listen for confirmation
            voiceEngine.onSpeechRecognized = { confirmText ->
                val resolved = VoiceCommandManager.parseConfirmation(confirmText, command)
                if (resolved != null) {
                    executeCommand(resolved, callerInfo, confirmText)
                } else {
                    // Still unclear — default to not picking up (safe default)
                    Log.w(tag, "Confirmation still ambiguous — defaulting to DO_NOT_PICK_UP")
                    executeCommand(CallCommand.DoNotPickUp, callerInfo, "unclear")
                }
            }
            voiceEngine.startListening()
        }
    }

    private fun handleCallEnded() {
        val callerInfo = currentCallerInfo ?: unknownCaller()
        val duration = ((System.currentTimeMillis() - callStartTimeMs) / 1000).toInt()
        val agent = conversationAgent

        // Reset stored caller number so next call starts fresh
        lastKnownCallerNumber = ""

        voiceEngine.stopListening()
        voiceEngine.stopSpeaking()
        voiceEngine.setInCallAudioAttributes(false)
        setSpeakerPhone(false)

        if (isJarvisHandlingCall && agent != null && settings.saveSummaries) {
            // Generate summary from Jarvis-handled call
            val transcriptLines = agent.getFullTranscript()
            val combinedCallerText = transcriptLines
                .filter { it.startsWith("CALLER:") }
                .joinToString(" ")

            val urgencyResult = UrgencyClassifier.classify(combinedCallerText, agent.getCallerTurnCount())
            val langResult = LanguageDetector.detect(combinedCallerText)

            val summaryState = CallState.Summary(
                callerInfo = callerInfo,
                durationSeconds = duration,
                handledByJarvis = true,
                transcriptLines = transcriptLines
            )
            transitionTo(summaryState)
            broadcastCallState("SUMMARY", callerInfo)

            serviceScope.launch {
                val summary = CallSummaryManager.buildSummary(
                    callerInfo = callerInfo,
                    transcriptLines = transcriptLines,
                    urgencyResult = urgencyResult,
                    languageResult = langResult,
                    durationSeconds = duration,
                    wasAutoAnswered = false,
                    saveTranscripts = settings.saveTranscripts
                )
                saveSummary(summary)

                // Notify user if important
                if (urgencyResult.category != CallUrgency.NORMAL) {
                    sendUrgencyNotification(callerInfo, urgencyResult, summary)
                }

                delay(1000)
                transitionTo(CallState.Idle)
                broadcastCallState("IDLE", callerInfo)
                stopSelf()
            }
        } else {
            transitionTo(CallState.CallEnded(callerInfo, duration, false))
            serviceScope.launch {
                delay(1000)
                transitionTo(CallState.Idle)
                broadcastCallState("IDLE", callerInfo)
                stopSelf()
            }
        }

        isJarvisHandlingCall = false
        conversationAgent = null
    }

    // ─── Telecom helpers ──────────────────────────────────────────────────────

    /**
     * Accept the ringing call.
     *
     * Multi-tiered fallback strategy:
     * 1. [JarvisInCallService] if active (default dialer role).
     * 2. [TelecomManager.acceptRingingCall] (requires ANSWER_PHONE_CALLS).
     * 3. MediaKey HEADSETHOOK dispatch via [AudioManager] (universal fallback on Android 10+).
     * 4. [Intent.ACTION_ANSWER] intent launch.
     */
    private fun acceptCallProgrammatically() {
        // Priority 1: InCallService (if default dialer)
        try {
            val inCallService = JarvisInCallService.instance
            if (inCallService != null) {
                inCallService.acceptCurrentCall()
                Log.i(tag, "✅ Call accepted via InCallService")
                return
            }
        } catch (e: Throwable) {
            Log.w(tag, "InCallService accept failed: ${e.message}")
        }

        // Priority 2: TelecomManager (ANSWER_PHONE_CALLS)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                @Suppress("MissingPermission")
                telecomManager?.acceptRingingCall()
                Log.i(tag, "✅ Call accepted via TelecomManager.acceptRingingCall()")
                return
            }
        } catch (e: Throwable) {
            Log.w(tag, "TelecomManager.acceptRingingCall failed: ${e.message} — trying MediaKey fallback")
        }

        // Priority 3: Dispatch MediaKey HEADSETHOOK key event (works like a headset button click)
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_HEADSETHOOK)
            val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_HEADSETHOOK)
            audioManager?.dispatchMediaKeyEvent(downEvent)
            audioManager?.dispatchMediaKeyEvent(upEvent)
            Log.i(tag, "✅ Call accepted via AudioManager HEADSETHOOK media key event")
            return
        } catch (e: Throwable) {
            Log.w(tag, "Media key answer failed: ${e.message} — trying ACTION_ANSWER intent")
        }

        // Priority 4: Action Answer Intent
        try {
            val answerIntent = Intent(Intent.ACTION_ANSWER).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(answerIntent)
            Log.i(tag, "✅ Call accept intent launched")
        } catch (e: Throwable) {
            Log.e(tag, "All call accept methods failed", e)
        }
    }

    /**
     * Decline / end the ringing or active call.
     */
    private fun endCallProgrammatically() {
        // Priority 1: InCallService
        try {
            val inCallService = JarvisInCallService.instance
            if (inCallService != null) {
                inCallService.disconnectCurrentCall()
                Log.i(tag, "✅ Call ended via InCallService")
                return
            }
        } catch (e: Throwable) {
            Log.w(tag, "InCallService end failed: ${e.message}")
        }

        // Priority 2: TelecomManager.endCall
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                @Suppress("MissingPermission")
                val ended = telecomManager?.endCall() ?: false
                if (ended) {
                    Log.i(tag, "✅ Call ended via TelecomManager")
                    return
                }
            }
        } catch (e: Throwable) {
            Log.w(tag, "TelecomManager.endCall failed: ${e.message} — trying MediaKey ENDCALL fallback")
        }

        // Priority 3: MediaKey ENDCALL event
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENDCALL)
            val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENDCALL)
            audioManager?.dispatchMediaKeyEvent(downEvent)
            audioManager?.dispatchMediaKeyEvent(upEvent)
        } catch (e: Throwable) {
            Log.e(tag, "Media key end call failed", e)
        }
    }

    private fun setSpeakerPhone(on: Boolean) {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.isSpeakerphoneOn = on
            Log.d(tag, "Speaker phone: $on")
        } catch (e: Throwable) {
            Log.w(tag, "Could not set speaker phone", e)
        }
    }

    // ─── Private utilities ────────────────────────────────────────────────────

    private fun transitionTo(state: CallState) {
        Log.d(tag, "State: ${_callState.value::class.simpleName} → ${state::class.simpleName}")
        _callState.value = state
        updateNotificationForState(state)
    }

    private fun buildAnnouncement(callerInfo: CallerInfo): String {
        val who = callerInfo.announcementLabel()
        // "Ullas, Rahul is calling you." / "Ullas, an unknown number is calling you."
        return "Ullas, $who is calling you."
    }

    private fun cancelAutoAnswer() {
        autoAnswerJob?.cancel()
        autoAnswerJob = null
    }

    private fun broadcastCallState(stateName: String, callerInfo: CallerInfo) {
        try {
            val intent = Intent(BROADCAST_CALL_STATE).apply {
                putExtra(BROADCAST_EXTRA_STATE, stateName)
                putExtra(BROADCAST_EXTRA_CALLER_NAME, callerInfo.displayName)
                putExtra(BROADCAST_EXTRA_CALLER_NUMBER, callerInfo.number)
                setPackage(packageName)
            }
            sendBroadcast(intent)
        } catch (e: Throwable) {
            Log.e(tag, "Broadcast failed", e)
        }
    }

    private fun unknownCaller() = CallerInfo("", "Unknown", false)

    private suspend fun saveSummary(summary: CallSummaryEntity) {
        try {
            withContext(Dispatchers.IO) {
                database.jarvisDao().insertCallSummary(summary)
            }
            Log.i(tag, "Call summary saved: ${summary.callerName}")
        } catch (e: Throwable) {
            Log.e(tag, "Error saving call summary", e)
        }
    }

    private fun sendUrgencyNotification(
        callerInfo: CallerInfo,
        urgencyResult: UrgencyResult,
        summary: CallSummaryEntity
    ) {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val title = when (urgencyResult.category) {
                CallUrgency.POTENTIALLY_URGENT ->
                    "⚠ Potentially Urgent: ${callerInfo.displayName}"
                CallUrgency.IMPORTANT ->
                    "📞 Important Call: ${callerInfo.displayName}"
                else -> return
            }
            val intent = PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java).apply {
                    action = "OPEN_CALL_SUMMARY"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setContentTitle(title)
                .setContentText(summary.callerReason.take(120))
                .setStyle(NotificationCompat.BigTextStyle().bigText(summary.summaryText))
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            nm.notify(NOTIF_ID + 1, notif)
        } catch (e: Throwable) {
            Log.e(tag, "Error sending urgency notification", e)
        }
    }

    // ─── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jarvis Call Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Jarvis Smart Call Assistant status"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("🤖 Jarvis")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotificationForState(state: CallState) {
        val text = when (state) {
            is CallState.IncomingCall -> "📞 Incoming call from ${state.callerName ?: state.callerNumber}"
            is CallState.AnnouncingCaller -> "🔊 Announcing caller..."
            is CallState.WaitingForCommand -> "🎙 Listening for your command..."
            is CallState.CallActive -> "📞 Call active"
            is CallState.JarvisAnswering -> "🤖 Jarvis is answering..."
            is CallState.JarvisConversing -> "💬 Jarvis is speaking with caller"
            is CallState.Summary -> "📋 Generating call summary..."
            else -> "Jarvis Call Assistant"
        }
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID, buildNotification(text))
        } catch (_: Throwable) {}
    }

    // ─── Handle user command from UI / overlay ─────────────────────────────────
    private fun handleUserCommand(commandString: String) {
        val callerInfo = currentCallerInfo ?: unknownCaller()
        when (commandString) {
            "PICK_UP" -> executeCommand(CallCommand.PickUp, callerInfo, commandString)
            "JARVIS_ANSWER" -> executeCommand(CallCommand.JarvisAnswer, callerInfo, commandString)
            "DECLINE" -> executeCommand(CallCommand.DoNotPickUp, callerInfo, commandString)
        }
    }
}
