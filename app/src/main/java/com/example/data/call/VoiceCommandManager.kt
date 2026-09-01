package com.aistudio.jarvis.voiceagent.data.call

import android.util.Log

/**
 * Parses the user's spoken words during an incoming call into a [CallCommand].
 *
 * Uses deterministic priority ordering:
 * 1. Decline commands (don't pick up, decline, cut, etc.)
 * 2. AI Answer commands (answer them, jarvis answer, you talk, etc.)
 * 3. Pick up commands (pick up, answer, receive, etc.)
 */
object VoiceCommandManager {

    private val tag = "VoiceCommandManager"

    // Decline phrases checked FIRST to avoid false pickup triggers
    private val declinePhrases = listOf(
        "don't pick up", "dont pick up", "do not pick up", "don't answer",
        "dont answer", "do not answer", "decline", "reject", "ignore",
        "hang up", "cut the call", "cut call", "cut", "not now", "busy",
        "leave it", "drop it", "send to voicemail", "voicemail",
        // Hindi
        "mat uthao", "nahi uthao", "mat utha", "chodo", "nhi uthana",
        // Kannada
        "beda", "thagobeda", "bidi", "mado beda"
    )

    // AI Answer phrases checked SECOND
    private val jarvisAnswerPhrases = listOf(
        "answer them", "you answer", "jarvis answer", "handle it",
        "handle the call", "you take it", "take it jarvis", "manage it",
        "deal with it", "you talk", "talk to them", "ai answer",
        // Hindi
        "tum uthao", "jarvis uthao", "tu le", "aap baat karo",
        // Kannada
        "neevu thago", "jarvis thago", "neevu maathu"
    )

    // Manual Pick Up phrases checked THIRD
    private val pickUpPhrases = listOf(
        "pick up", "pickup", "answer", "accept", "receive", "take the call",
        "take call", "answer call", "yes pick up", "go ahead", "pick",
        // Hindi
        "uthao", "utha", "lo", "uthaao", "haan",
        // Kannada
        "thago", "thagoli", "maathu", "hauda"
    )

    fun parse(rawText: String): CallCommand {
        val lower = rawText.lowercase().trim()
        Log.d(tag, "Parsing voice command: '$lower'")

        if (lower.isBlank()) return CallCommand.Unknown

        // Step 1: Check Decline
        for (kw in declinePhrases) {
            if (lower.contains(kw)) {
                Log.i(tag, "Matched DECLINE keyword '$kw' → DO_NOT_PICK_UP")
                return CallCommand.DoNotPickUp
            }
        }

        // Step 2: Check AI Answer
        for (kw in jarvisAnswerPhrases) {
            if (lower.contains(kw)) {
                Log.i(tag, "Matched JARVIS ANSWER keyword '$kw' → JARVIS_ANSWER")
                return CallCommand.JarvisAnswer
            }
        }

        // Step 3: Check Manual Pick Up
        for (kw in pickUpPhrases) {
            if (lower.contains(kw)) {
                Log.i(tag, "Matched PICK UP keyword '$kw' → PICK_UP")
                return CallCommand.PickUp
            }
        }

        Log.d(tag, "No voice command keyword matched for: '$lower'")
        return CallCommand.Unknown
    }

    /**
     * Parse confirmation for ambiguous commands if needed.
     */
    fun parseConfirmation(rawText: String, ambiguousCommand: CallCommand.Ambiguous): CallCommand? {
        val lower = rawText.lowercase().trim()
        val yesKeywords = listOf("yes", "yeah", "correct", "proceed", "haan", "hauda", "sari", "ok")
        val noKeywords = listOf("no", "nope", "cancel", "nahi", "beda", "illa")

        return when {
            yesKeywords.any { lower.contains(it) } -> CallCommand.PickUp
            noKeywords.any { lower.contains(it) } -> CallCommand.DoNotPickUp
            else -> null
        }
    }
}

