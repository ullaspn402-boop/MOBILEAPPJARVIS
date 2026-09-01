package com.aistudio.jarvis.voiceagent.data.call

/**
 * Settings model for the Smart Call Assistant feature.
 * Persisted via DataStore as a plain key-value map.
 *
 * All features default to OFF (privacy-first).
 */
data class CallAssistantSettings(

    // ─── Master switch ────────────────────────────────────────────────────────
    /** Master toggle. If false, Jarvis will not interact with any incoming calls. */
    val smartCallAssistantEnabled: Boolean = true,

    // ─── Auto-answer ──────────────────────────────────────────────────────────
    /**
     * When true, Jarvis will automatically accept calls on behalf of the user
     * after [autoAnswerDelaySeconds] of ringing, subject to [autoAnswerScope].
     * Default: OFF
     */
    val autoAnswerEnabled: Boolean = false,

    /**
     * How long (in seconds) Jarvis waits before auto-answering.
     * Range: 5–60. Default: 25.
     */
    val autoAnswerDelaySeconds: Int = 25,

    /**
     * Which callers Jarvis may auto-answer:
     *   ALL, UNKNOWN_ONLY, CONTACTS_ONLY, NEVER
     */
    val autoAnswerScope: AutoAnswerScope = AutoAnswerScope.NEVER,

    // ─── Speaker mode ─────────────────────────────────────────────────────────
    /**
     * Whether to use speaker phone when Jarvis answers a call.
     * Required for TTS/STT to work in Jarvis-answering mode.
     * User-configurable as per design decision.
     */
    val useSpeakerForJarvisAnswer: Boolean = true,

    // ─── Privacy / storage ───────────────────────────────────────────────────
    /** Store a concise summary of each Jarvis-handled call. Default: true. */
    val saveSummaries: Boolean = true,

    /** Store turn-by-turn transcript. Default: false. */
    val saveTranscripts: Boolean = false,

    /** Record and store call audio. Default: false (always off in prototype). */
    val saveAudio: Boolean = false,

    // ─── Default dialer role ──────────────────────────────────────────────────
    /**
     * Whether the user has been shown the default dialer explanation screen.
     * We show it at most once.
     */
    val hasShownDialerRoleExplanation: Boolean = false,

    // ─── Caller notification ──────────────────────────────────────────────────
    /**
     * Inform the caller that the call may be processed by an AI assistant.
     * When true, Jarvis reads a brief consent notice at the start of every
     * Jarvis-answered call.
     */
    val informCallerOfAi: Boolean = true
)

enum class AutoAnswerScope {
    ALL,
    UNKNOWN_ONLY,
    CONTACTS_ONLY,
    NEVER
}

// ─── DataStore keys ───────────────────────────────────────────────────────────
object CallAssistantPrefsKeys {
    const val ENABLED = "sca_enabled"
    const val AUTO_ANSWER_ENABLED = "sca_auto_answer_enabled"
    const val AUTO_ANSWER_DELAY = "sca_auto_answer_delay"
    const val AUTO_ANSWER_SCOPE = "sca_auto_answer_scope"
    const val USE_SPEAKER = "sca_use_speaker"
    const val SAVE_SUMMARIES = "sca_save_summaries"
    const val SAVE_TRANSCRIPTS = "sca_save_transcripts"
    const val SAVE_AUDIO = "sca_save_audio"
    const val SHOWN_DIALER_EXPLANATION = "sca_shown_dialer_explanation"
    const val INFORM_CALLER_OF_AI = "sca_inform_caller_of_ai"
}
