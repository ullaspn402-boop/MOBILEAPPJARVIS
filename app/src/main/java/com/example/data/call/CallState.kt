package com.aistudio.jarvis.voiceagent.data.call

/**
 * Complete state machine for the Jarvis Smart Call Assistant.
 *
 * State flow:
 *   IDLE
 *     → INCOMING_CALL
 *         → ANNOUNCING_CALLER
 *             → WAITING_FOR_COMMAND
 *                 ├── USER_PICKUP       → CALL_ACTIVE → CALL_ENDED → SUMMARY → IDLE
 *                 ├── JARVIS_ANSWERING  → JARVIS_CONVERSING → CALL_ENDED → SUMMARY → IDLE
 *                 ├── DECLINING        → CALL_DECLINED → IDLE
 *                 └── NO_RESPONSE      → CONFIGURED_FALLBACK → CALL_ENDED → SUMMARY → IDLE
 */
sealed class CallState {

    /** No active call. Normal assistant mode. */
    object Idle : CallState()

    /** An incoming call has been detected. Contains raw telephony data. */
    data class IncomingCall(
        val callerNumber: String,
        val callerName: String?,
        val isKnownContact: Boolean,
        val detectedAt: Long = System.currentTimeMillis()
    ) : CallState()

    /** Jarvis is currently speaking the caller announcement. */
    data class AnnouncingCaller(
        val callerInfo: CallerInfo
    ) : CallState()

    /** Jarvis has finished announcing and is now listening for the user's voice command. */
    data class WaitingForCommand(
        val callerInfo: CallerInfo,
        val autoAnswerDeadlineMs: Long?   // null = auto-answer disabled
    ) : CallState()

    /** User said "pick up" — call is being accepted, user talks directly. */
    data class CallActive(
        val callerInfo: CallerInfo
    ) : CallState()

    /** Jarvis accepted the call and is now speaking to the caller as an AI assistant. */
    data class JarvisAnswering(
        val callerInfo: CallerInfo,
        val speakerMode: Boolean
    ) : CallState()

    /** Jarvis is in a live conversation with the caller. */
    data class JarvisConversing(
        val callerInfo: CallerInfo,
        val turnCount: Int = 0,
        val transcriptSoFar: String = ""
    ) : CallState()

    /** Call is being declined per user command or setting. */
    data class Declining(
        val callerInfo: CallerInfo,
        val reason: DeclineReason
    ) : CallState()

    /** No user response — waiting to apply the configured fallback action. */
    data class NoResponse(
        val callerInfo: CallerInfo
    ) : CallState()

    /** The call has ended (normally, after user pickup, or after Jarvis conversation). */
    data class CallEnded(
        val callerInfo: CallerInfo,
        val durationSeconds: Int,
        val handledByJarvis: Boolean,
        val transcriptLines: List<String> = emptyList()
    ) : CallState()

    /** Generating and storing the call summary. */
    data class Summary(
        val callerInfo: CallerInfo,
        val durationSeconds: Int,
        val handledByJarvis: Boolean,
        val transcriptLines: List<String>
    ) : CallState()

    /** Call declined, waiting to return to Idle. */
    data class CallDeclined(
        val callerInfo: CallerInfo
    ) : CallState()
}

enum class DeclineReason {
    USER_COMMAND,          // User explicitly said "don't pick up"
    USER_SETTINGS,         // Excluded contact / auto-decline setting
    TIMEOUT_NO_ANSWER      // Ring timeout with auto-answer disabled
}

enum class CallUrgency {
    NORMAL,
    IMPORTANT,
    POTENTIALLY_URGENT
}
