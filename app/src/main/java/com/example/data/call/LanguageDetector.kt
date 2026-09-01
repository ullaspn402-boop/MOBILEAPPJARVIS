package com.aistudio.jarvis.voiceagent.data.call

import android.util.Log

/**
 * Lightweight on-device language detector for incoming call conversations.
 *
 * Supports English, Kannada, Hindi, and mixed-language speech.
 * Uses keyword and Unicode script heuristics — no external API required.
 *
 * Language detection is best-effort. Falls back to English.
 */
object LanguageDetector {

    private val tag = "LanguageDetector"

    // ─── Kannada Unicode range: U+0C80–U+0CFF ─────────────────────────────────
    private val kannadaRange = '\u0C80'..'\u0CFF'

    // ─── Devanagari Unicode range (Hindi): U+0900–U+097F ──────────────────────
    private val devanagariRange = '\u0900'..'\u097F'

    // Common Hindi words in transliterated Latin script
    private val hindiLatinKeywords = setOf(
        "aap", "haan", "nahi", "kya", "main", "mujhe", "hum", "aur",
        "theek", "accha", "bhai", "dost", "bolo", "karo", "lena", "dena",
        "matlab", "samajh", "agar", "toh", "jab", "kyunki", "abhi", "baad",
        "zaroor", "bilkul", "shukriya", "namaste", "kal"
    )

    // Common Kannada words in transliterated Latin script
    private val kannadaLatinKeywords = setOf(
        "nimma", "namma", "avaru", "illi", "enu", "beku", "beda", "hauda",
        "ilva", "hogi", "barthini", "antha", "gottu", "gotilla", "madona",
        "hogona", "swami", "anna", "akka", "yella", "sari", "ashte",
        "yellidira", "banni", "hogri", "namaskara", "dhanyavada"
    )

    /**
     * Detect the primary language from a text snippet.
     *
     * @param text Transcript of the caller's speech.
     * @return A language tag suitable for [JarvisVoiceEngine.setLanguage] (e.g. "kn-IN", "hi-IN", "en-US")
     *         and a human-readable name.
     */
    fun detect(text: String): DetectionResult {
        if (text.isBlank()) return DetectionResult("en-US", "English", 0f)

        val lower = text.lowercase().trim()
        val words = lower.split(Regex("\\s+"))

        // 1. Check for Kannada Unicode script characters
        val kannadaCharCount = text.count { it in kannadaRange }
        if (kannadaCharCount > 2) {
            Log.d(tag, "Detected Kannada (Unicode chars: $kannadaCharCount)")
            return DetectionResult("kn-IN", "Kannada", 0.9f)
        }

        // 2. Check for Devanagari (Hindi) Unicode script characters
        val devanagariCharCount = text.count { it in devanagariRange }
        if (devanagariCharCount > 2) {
            Log.d(tag, "Detected Hindi (Devanagari chars: $devanagariCharCount)")
            return DetectionResult("hi-IN", "Hindi", 0.9f)
        }

        // 3. Latin-script keyword matching
        val kannadaHits = words.count { it in kannadaLatinKeywords }
        val hindiHits = words.count { it in hindiLatinKeywords }

        return when {
            kannadaHits > 0 && kannadaHits >= hindiHits -> {
                val conf = (kannadaHits.toFloat() / words.size).coerceIn(0.3f, 0.85f)
                Log.d(tag, "Detected Kannada (latin keywords: $kannadaHits)")
                DetectionResult("kn-IN", "Kannada", conf)
            }
            hindiHits > 0 -> {
                val conf = (hindiHits.toFloat() / words.size).coerceIn(0.3f, 0.85f)
                Log.d(tag, "Detected Hindi (latin keywords: $hindiHits)")
                DetectionResult("hi-IN", "Hindi", conf)
            }
            else -> {
                Log.d(tag, "Defaulting to English")
                DetectionResult("en-US", "English", 0.5f)
            }
        }
    }

    /**
     * Detect mixed-language speech and return the dominant language.
     * Accumulates results over multiple turns.
     */
    fun detectFromMultipleTurns(turns: List<String>): DetectionResult {
        if (turns.isEmpty()) return DetectionResult("en-US", "English", 0f)
        val combined = turns.joinToString(" ")
        return detect(combined)
    }
}

data class DetectionResult(
    /** BCP-47 language tag, e.g. "kn-IN", "hi-IN", "en-US" */
    val languageTag: String,
    /** Human-readable name, e.g. "Kannada" */
    val languageName: String,
    /** Confidence score 0.0–1.0 */
    val confidence: Float
)
