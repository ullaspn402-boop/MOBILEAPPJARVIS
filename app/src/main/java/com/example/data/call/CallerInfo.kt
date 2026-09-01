package com.aistudio.jarvis.voiceagent.data.call

/**
 * Identifies who is calling.
 */
data class CallerInfo(
    val number: String,
    val displayName: String,   // Contact name, or formatted number, or "Unknown"
    val isKnownContact: Boolean,
    val thumbnailUri: String? = null
) {
    /** Returns a human-readable label for TTS announcements. */
    fun announcementLabel(): String = when {
        isKnownContact && displayName.isNotBlank() -> displayName
        displayName.isNotBlank() && displayName != "Unknown" && !displayName.startsWith("+") && displayName.any { it.isLetter() } -> displayName
        number.isNotBlank() -> "an unknown number ($number)"
        else -> "an unknown number"
    }
}

/**
 * Parsed voice command from the user during an incoming call.
 */
sealed class CallCommand {
    /** User said "pick up", "answer", "accept" etc. */
    object PickUp : CallCommand()

    /** User said "answer them", "Jarvis answer", "you answer" etc. */
    object JarvisAnswer : CallCommand()

    /** User said "don't pick up", "decline", "reject", "ignore" etc. */
    object DoNotPickUp : CallCommand()

    /**
     * Ambiguous command — Jarvis should ask for confirmation before acting.
     * [rawText] is what was actually recognised.
     */
    data class Ambiguous(val rawText: String) : CallCommand()

    /** No command was understood or silence. */
    object Unknown : CallCommand()
}

/**
 * Result of urgency classification from a Jarvis-handled call conversation.
 */
data class UrgencyResult(
    val category: CallUrgency,
    val triggerPhrases: List<String> = emptyList(),
    val confidence: Float = 0f
)
