package com.aistudio.jarvis.voiceagent.data.call

import android.util.Log

/**
 * Generates a concise, human-readable call summary from a Jarvis-handled conversation.
 *
 * Data minimization principle: only a short summary is stored, not the full transcript,
 * unless the user has enabled transcript storage.
 */
object CallSummaryManager {

    private val tag = "CallSummaryManager"

    /**
     * Build a [CallSummaryEntity] from the data gathered during a Jarvis-handled call.
     *
     * @param callerInfo Who called.
     * @param transcriptLines The turn-by-turn transcript (Jarvis + Caller lines).
     * @param urgencyResult Classification result from [UrgencyClassifier].
     * @param languageResult Detected language result.
     * @param durationSeconds How long Jarvis handled the call.
     * @param wasAutoAnswered Was this an automatic answer (no user command)?
     * @param wasDeclined Was this call declined?
     * @param saveTranscripts Should the full transcript be stored? (user setting)
     */
    fun buildSummary(
        callerInfo: CallerInfo,
        transcriptLines: List<String>,
        urgencyResult: UrgencyResult,
        languageResult: DetectionResult,
        durationSeconds: Int,
        wasAutoAnswered: Boolean = false,
        wasDeclined: Boolean = false,
        saveTranscripts: Boolean = false
    ): CallSummaryEntity {

        val callerLines = transcriptLines
            .filter { it.startsWith("CALLER:") }
            .map { it.removePrefix("CALLER:").trim() }

        val callerReason = extractReason(callerLines)
        val actionRequired = deriveAction(urgencyResult, callerLines)
        val summaryText = buildSummaryText(
            callerInfo = callerInfo,
            callerLines = callerLines,
            urgencyResult = urgencyResult,
            languageResult = languageResult,
            durationSeconds = durationSeconds
        )

        Log.d(tag, "Summary built for ${callerInfo.displayName} | ${urgencyResult.category}")

        return CallSummaryEntity(
            callerName = callerInfo.displayName,
            callerNumber = callerInfo.number,
            timestampMs = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            detectedLanguage = languageResult.languageName,
            urgencyCategory = urgencyResult.category.name,
            callerReason = callerReason,
            actionRequired = actionRequired,
            summaryText = summaryText,
            wasAutoAnswered = wasAutoAnswered,
            wasDeclined = wasDeclined
        )
    }

    /**
     * Build a summary for a declined call (no conversation happened).
     */
    fun buildDeclinedSummary(callerInfo: CallerInfo): CallSummaryEntity {
        return CallSummaryEntity(
            callerName = callerInfo.displayName,
            callerNumber = callerInfo.number,
            timestampMs = System.currentTimeMillis(),
            durationSeconds = 0,
            detectedLanguage = "N/A",
            urgencyCategory = CallUrgency.NORMAL.name,
            callerReason = "Call was declined.",
            actionRequired = "You may wish to call back.",
            summaryText = "A call from ${callerInfo.displayName} was declined.",
            wasDeclined = true
        )
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun extractReason(callerLines: List<String>): String {
        if (callerLines.isEmpty()) return "Reason not stated."

        // Take the most content-rich caller line (longest)
        val richest = callerLines.maxByOrNull { it.length } ?: callerLines.first()

        // Truncate to a reasonable summary length
        return if (richest.length > 200) richest.take(197) + "…" else richest
    }

    private fun deriveAction(urgencyResult: UrgencyResult, callerLines: List<String>): String {
        return when (urgencyResult.category) {
            CallUrgency.POTENTIALLY_URGENT -> "⚠ Contact the caller immediately. Review urgency details."
            CallUrgency.IMPORTANT -> "Call back when available."
            CallUrgency.NORMAL -> "No immediate action required."
        }
    }

    private fun buildSummaryText(
        callerInfo: CallerInfo,
        callerLines: List<String>,
        urgencyResult: UrgencyResult,
        languageResult: DetectionResult,
        durationSeconds: Int
    ): String {
        val callerLabel = if (callerInfo.isKnownContact) callerInfo.displayName else "Unknown caller (${callerInfo.number})"
        val languageLabel = languageResult.languageName
        val urgencyLabel = UrgencyClassifier.label(urgencyResult.category)
        val durationLabel = formatDuration(durationSeconds)
        val reasonSnippet = callerLines.firstOrNull()?.take(120) ?: "No message recorded."

        return buildString {
            appendLine("📞 Call from: $callerLabel")
            appendLine("🌐 Language: $languageLabel")
            appendLine("⚡ Category: $urgencyLabel")
            appendLine("⏱ Duration: $durationLabel")
            appendLine()
            appendLine("Caller's message: $reasonSnippet")
            if (urgencyResult.triggerPhrases.isNotEmpty()) {
                appendLine()
                appendLine("Urgency signals: ${urgencyResult.triggerPhrases.take(3).joinToString(", ")}")
            }
        }.trim()
    }

    private fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }
}
