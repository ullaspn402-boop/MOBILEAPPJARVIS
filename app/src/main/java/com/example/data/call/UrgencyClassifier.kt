package com.aistudio.jarvis.voiceagent.data.call

import android.util.Log

/**
 * Classifies a Jarvis-handled call conversation into urgency categories.
 *
 * SAFETY PRINCIPLE: When uncertain, always classify higher (IMPORTANT or POTENTIALLY_URGENT)
 * and notify the user. Never silently dismiss a call based on AI classification alone.
 *
 * Categories:
 *   NORMAL            — routine call, no urgency signals detected
 *   IMPORTANT         — caller explicitly mentions time-sensitivity or importance
 *   POTENTIALLY_URGENT — caller describes emergency, distress, or immediate need
 */
object UrgencyClassifier {

    private val tag = "UrgencyClassifier"

    // ─── POTENTIALLY_URGENT trigger phrases ────────────────────────────────────
    private val urgentPhrases = setOf(
        // English
        "emergency", "urgent", "immediately", "right now", "hospital",
        "accident", "help", "please hurry", "serious", "critical",
        "can't wait", "cannot wait", "life", "danger", "injured", "bleeding",
        "police", "fire", "ambulance", "911", "100", "102", "108",
        // Hindi
        "achaanak", "turant", "abhi", "zaroor", "madad", "khatra", "accident", "hospital", "ambulance",
        // Kannada
        "tumba urgent", "beega", "aytu", "madad", "khatara", "hospital", "ambulance"
    )

    // ─── IMPORTANT trigger phrases ─────────────────────────────────────────────
    private val importantPhrases = setOf(
        // English
        "important", "please call back", "need to talk", "as soon as possible",
        "asap", "when you can", "tell him to call", "tell her to call",
        "deadline", "meeting", "project", "decision needed", "waiting",
        "needs to know", "let him know", "let her know",
        // Hindi
        "zaruri", "please batao", "batao unhe", "call karo", "call karein",
        // Kannada
        "muhyavaada", "helidre", "helide", "call madri", "badlu madri"
    )

    // ─── Repeated-call urgency threshold ──────────────────────────────────────
    private val repeatedCallPhrases = setOf(
        "called before", "called again", "tried calling", "been calling",
        "called multiple", "third time", "second time"
    )

    /**
     * Classify the caller's conversation transcript.
     *
     * @param transcript Full text of the caller's utterances during the call.
     * @param callerTurnCount Number of turns the caller spoke (repeated insistence signals importance).
     */
    fun classify(transcript: String, callerTurnCount: Int = 1): UrgencyResult {
        val lower = transcript.lowercase()
        val triggers = mutableListOf<String>()

        // Check for potentially urgent signals
        val urgentHits = urgentPhrases.filter { lower.contains(it) }
        if (urgentHits.isNotEmpty()) {
            triggers.addAll(urgentHits)
            val confidence = (urgentHits.size.toFloat() / 3f).coerceIn(0.5f, 1.0f)
            Log.w(tag, "POTENTIALLY_URGENT detected: $urgentHits")
            return UrgencyResult(
                category = CallUrgency.POTENTIALLY_URGENT,
                triggerPhrases = triggers,
                confidence = confidence
            )
        }

        // Check for important signals
        val importantHits = importantPhrases.filter { lower.contains(it) }
        val repeatedHits = repeatedCallPhrases.filter { lower.contains(it) }
        val totalImportantSignals = importantHits.size + repeatedHits.size

        // Caller speaking many turns also signals importance
        val repeatedByTurnCount = callerTurnCount >= 4

        if (totalImportantSignals > 0 || repeatedByTurnCount) {
            triggers.addAll(importantHits)
            triggers.addAll(repeatedHits)
            val confidence = ((totalImportantSignals + (if (repeatedByTurnCount) 1 else 0)).toFloat() / 3f).coerceIn(0.4f, 0.9f)
            Log.d(tag, "IMPORTANT detected: signals=$triggers, turns=$callerTurnCount")
            return UrgencyResult(
                category = CallUrgency.IMPORTANT,
                triggerPhrases = triggers,
                confidence = confidence
            )
        }

        Log.d(tag, "NORMAL call classification")
        return UrgencyResult(
            category = CallUrgency.NORMAL,
            triggerPhrases = emptyList(),
            confidence = 0.7f
        )
    }

    /**
     * Returns a user-readable label for a [CallUrgency] value.
     */
    fun label(urgency: CallUrgency): String = when (urgency) {
        CallUrgency.NORMAL -> "Normal"
        CallUrgency.IMPORTANT -> "Important"
        CallUrgency.POTENTIALLY_URGENT -> "⚠ Potentially Urgent"
    }
}
