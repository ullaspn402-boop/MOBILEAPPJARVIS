package com.aistudio.jarvis.voiceagent.data.call

import android.util.Log
import com.aistudio.jarvis.voiceagent.data.service.JarvisVoiceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages a live multi-turn AI dialogue with the caller when Jarvis has answered the call.
 *
 * TRANSPARENCY RULE: Jarvis ALWAYS identifies itself as an AI assistant. It NEVER impersonates
 * the phone owner.
 *
 * Implementation uses the existing [JarvisVoiceEngine] for TTS/STT. The caller's audio is
 * captured via the device microphone (works best in speaker-phone mode).
 *
 * The full transcript is kept in memory for [CallSummaryManager]. It is not stored unless
 * the user has enabled transcript storage in settings.
 */
class CallConversationAgent(
    private val voiceEngine: JarvisVoiceEngine,
    private val scope: CoroutineScope,
    private val userName: String,
    private val callerInfo: CallerInfo,
    private val languageTag: String = "en-US",
    private val informCaller: Boolean = true
) {

    private val tag = "CallConversationAgent"

    // Transcript lines (alternating Jarvis / Caller turns)
    private val _transcriptLines = MutableStateFlow<List<String>>(emptyList())
    val transcriptLines: StateFlow<List<String>> = _transcriptLines.asStateFlow()

    private var callerTurnCount = 0
    private var latestCallerUtterance = ""

    private val _isListeningToCaller = MutableStateFlow(false)
    val isListeningToCaller: StateFlow<Boolean> = _isListeningToCaller.asStateFlow()

    private val _isJarvisSpeaking = MutableStateFlow(false)
    val isJarvisSpeaking: StateFlow<Boolean> = _isJarvisSpeaking.asStateFlow()

    private val _conversationEnded = MutableStateFlow(false)
    val conversationEnded: StateFlow<Boolean> = _conversationEnded.asStateFlow()

    /** Start the conversation: speak the opening greeting, then listen. */
    fun startConversation(onReadyToListen: () -> Unit) {
        val greeting = buildOpeningGreeting()
        appendTranscript("JARVIS: $greeting")
        voiceEngine.setLanguage(languageTag)
        speakAndThenListen(greeting, onReadyToListen)
    }

    /** Called when the speech recognizer returns the caller's utterance. */
    fun onCallerSpeechReceived(callerText: String) {
        callerTurnCount++
        latestCallerUtterance = callerText
        appendTranscript("CALLER: $callerText")
        _isListeningToCaller.value = false

        scope.launch {
            val response = generateResponse(callerText)
            appendTranscript("JARVIS: $response")
            voiceEngine.speak(response)
        }
    }

    /**
     * Call this after Jarvis finishes speaking each turn, to listen for the caller's next utterance.
     * [onCallerSpeechReceived] will be called with the result.
     */
    fun startListeningToCaller() {
        if (_isListeningToCaller.value) return
        _isListeningToCaller.value = true
        voiceEngine.startListening()
        Log.d(tag, "Listening to caller...")
    }

    /** Gracefully end the conversation. */
    fun endConversation(reason: String = "The user is now available.") {
        val farewell = when {
            languageTag.startsWith("kn") -> "Saari, $userName avaru swalpa busy iddaare. Nantara call maadi. Dhanyavaada."
            languageTag.startsWith("hi") -> "Theek hai. $userName abhi busy hai. Baad mein call kar lijiye. Shukriya."
            else -> "Thank you for calling. I'll let $userName know. Goodbye."
        }
        appendTranscript("JARVIS: $farewell")
        voiceEngine.speak(farewell)
        scope.launch {
            delay(3000)
            _conversationEnded.value = true
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun buildOpeningGreeting(): String {
        val consentNote = if (informCaller)
            " Please note this call is handled by an AI assistant. "
        else ""

        return when {
            languageTag.startsWith("kn") ->
                "Namaskara! Ullas avarige call madiddakke dhanyavaada. Naanu Jarvis, Ullas avara AI calling assistant.$consentNote " +
                "Avaru iga busy iddaare. Naanu nimage hege sahaya madli, matthu yake call madidira?"
            languageTag.startsWith("hi") ->
                "Namaste! Ullas ko call karne ke liye dhanyavaad. Main Jarvis hoon, Ullas ka AI calling assistant.$consentNote " +
                "Woh abhi busy hain. Main aapki kya madad kar sakta hoon, aur aapne kis silsile mein call kiya hai?"
            else ->
                "Hello! Thank you for calling Ullas. I am Jarvis, Ullas's AI calling assistant.$consentNote " +
                "They are currently unavailable. How may I assist you today, and what message would you like me to pass along?"
        }
    }

    private fun generateResponse(callerText: String): String {
        val lower = callerText.lowercase()

        return when {
            lower.contains("urgent") || lower.contains("emergency") || lower.contains("hospital") || lower.contains("accident") -> {
                scope.launch { delay(5000); endConversation("Urgent request processed") }
                buildUrgentResponse()
            }
            lower.contains("bye") || lower.contains("ok thanks") || lower.contains("ok thank you") || lower.contains("that's all") || lower.contains("thats all") -> {
                scope.launch { delay(4000); endConversation("Caller said farewell") }
                buildFarewellResponse()
            }
            lower.contains("message") || lower.contains("tell") || lower.contains("inform") || lower.contains("say") || lower.contains("note") ->
                buildMessageAckResponse()
            lower.contains("when") || lower.contains("available") || lower.contains("free") || lower.contains("call back") ->
                buildAvailabilityResponse()
            callerTurnCount >= 4 -> {
                scope.launch { delay(4000); endConversation("Max turns reached") }
                buildWrapUpResponse()
            }
            else ->
                buildGenericAckResponse(callerText)
        }
    }

    private fun buildUrgentResponse(): String = when {
        languageTag.startsWith("kn") ->
            "Arthamaayitu. EE vishaya tumba urgent antha nanu note madidini. Ullas avrige takshana urgent alert kaluhistini. Dhanyavaada."
        languageTag.startsWith("hi") ->
            "Samajh gaya. Yeh kaafi zaruri maamla hai. Main Ullas ko abhi urgent alert bhej raha hoon. Shukriya."
        else ->
            "I understand this is urgent. I have flagged your request with high priority and sent an immediate alert to Ullas. Thank you."
    }

    private fun buildMessageAckResponse(): String = when {
        languageTag.startsWith("kn") ->
            "Kanditha, nanu nimma message na poorthiyagi note madikondiddini. Ullas avarige innenu helabeka?"
        languageTag.startsWith("hi") ->
            "Ji bilkul, main aapka message achhe se note kar chuka hoon. Kya Ullas ke liye aur koi jankari bhejni hai?"
        else ->
            "Certainly, I have carefully recorded your message. Is there any additional detail you would like me to convey to Ullas?"
    }

    private fun buildAvailabilityResponse(): String = when {
        languageTag.startsWith("kn") ->
            "Ullas avaru free aaddaga nimage takshana call back madtini antha helidare. Nanu avarige remind madtini."
        languageTag.startsWith("hi") ->
            "Jaise hi Ullas free honge, woh aapko jald se jald call back karenge. Main unhe remind kar dunga."
        else ->
            "Ullas will be notified immediately, and they will return your call as soon as they are free."
    }

    private fun buildFarewellResponse(): String = when {
        languageTag.startsWith("kn") ->
            "Call madiddakke tumba dhanyavaada! Nanu nimma message na Ullas avarige thalupistini. Olleyadagali, goodbye!"
        languageTag.startsWith("hi") ->
            "Call karne ke liye bahut dhanyavaad! Main aapka message Ullas tak pahuncha dunga. Aapka din shubh ho, goodbye!"
        else ->
            "Thank you very much for calling! I will ensure Ullas receives your complete message right away. Have a wonderful day, goodbye!"
    }

    private fun buildWrapUpResponse(): String = when {
        languageTag.startsWith("kn") ->
            "Nanu ella vishayavannu note madikondiddini matthu Ullas avarige notify madidini. Call madiddakke dhanyavaada, goodbye!"
        languageTag.startsWith("hi") ->
            "Main aapki saari baatein note kar chuka hoon aur Ullas ko notify kar diya hai. Call karne ke liye shukriya, goodbye!"
        else ->
            "I have noted all your details and notified Ullas. Thank you for calling our AI customer care agent, goodbye!"
    }

    private fun buildGenericAckResponse(text: String): String = when {
        languageTag.startsWith("kn") ->
            "Arthamaayitu. Ullas avarige sariyada summary kodalu, innenu vishaya helabeka?"
        languageTag.startsWith("hi") ->
            "Ji samajh gaya. Ullas ko sahi summary dene ke liye, kya aap thoda aur bata sakte hain?"
        else ->
            "Understood. To help me give Ullas an accurate summary of your call, could you share a bit more detail?"
    }

    private fun speakAndThenListen(text: String, onReadyToListen: () -> Unit) {
        voiceEngine.speak(text) {
            onReadyToListen()
        }
    }

    private fun appendTranscript(line: String) {
        val current = _transcriptLines.value.toMutableList()
        current.add(line)
        _transcriptLines.value = current
        Log.d(tag, line)
    }

    fun getCallerTurnCount() = callerTurnCount
    fun getLatestCallerUtterance() = latestCallerUtterance
    fun getFullTranscript(): List<String> = _transcriptLines.value
}
