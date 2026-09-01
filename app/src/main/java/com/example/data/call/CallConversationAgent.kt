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
            " Please note that this call is being handled by an AI assistant. "
        else ""

        return when {
            languageTag.startsWith("kn") ->
                "Namaskara. Naanu Jarvis, $userName avara AI assistant.$consentNote " +
                "Avaru eeaga available aagilla. Neevu yake call madidira?"
            languageTag.startsWith("hi") ->
                "Namaste. Main Jarvis hoon, $userName ka AI assistant.$consentNote " +
                "Woh abhi available nahi hain. Aap kyon call kar rahe hain?"
            else ->
                "Hello. I am Jarvis, $userName's AI assistant.$consentNote " +
                "They are currently unavailable. How may I help you, and why are you calling?"
        }
    }

    private fun generateResponse(callerText: String): String {
        val lower = callerText.lowercase()

        // Very simple rule-based responses for the prototype.
        // In production this would use the Gemini REST API with a system prompt.
        return when {
            lower.contains("urgent") || lower.contains("emergency") ->
                buildUrgentResponse()
            lower.contains("message") || lower.contains("tell") ->
                buildMessageAckResponse()
            lower.contains("when") || lower.contains("available") ->
                buildAvailabilityResponse()
            lower.contains("bye") || lower.contains("ok thanks") || lower.contains("ok thank you") ->
                buildFarewellResponse()
            callerTurnCount >= 5 ->
                buildWrapUpResponse()
            else ->
                buildGenericAckResponse(callerText)
        }
    }

    private fun buildUrgentResponse(): String = when {
        languageTag.startsWith("kn") ->
            "Arthamaayitu. Neevu tumba urgent antha heltidira. Naanu $userName avrige takshaNav notify madtini. Neevu yenu helidira antha summary madtini."
        languageTag.startsWith("hi") ->
            "Samajh gaya. Yeh kaafi urgent lag raha hai. Main $userName ko turant notify karunga. Apna message bata dijiye."
        else ->
            "I understand this is urgent. I will immediately notify $userName with the details. Please continue — what would you like me to tell them?"
    }

    private fun buildMessageAckResponse(): String = when {
        languageTag.startsWith("kn") ->
            "Sari, nanu note madtini. Illade, innenu yenu heli?"
        languageTag.startsWith("hi") ->
            "Bilkul, main note kar leta hoon. Aur kuch kehna chahte hain?"
        else ->
            "Understood, I'll pass that message along. Is there anything else you'd like to add?"
    }

    private fun buildAvailabilityResponse(): String = when {
        languageTag.startsWith("kn") ->
            "Nanu $userName avrige helidaga, avaru nimage ASAP call madtaare."
        languageTag.startsWith("hi") ->
            "Main $userName ko bataunga. Woh aapko jald se jald call karenge."
        else ->
            "I'll let $userName know, and they will call you back as soon as possible."
    }

    private fun buildFarewellResponse(): String = when {
        languageTag.startsWith("kn") ->
            "Sari, dhanyavaada. $userName avrige nimage message tattisthini."
        languageTag.startsWith("hi") ->
            "Theek hai, shukriya. Main $userName ko aapka message bata dunga."
        else ->
            "Thank you for calling. I'll make sure $userName gets your message. Have a good day."
    }

    private fun buildWrapUpResponse(): String = when {
        languageTag.startsWith("kn") ->
            "Nanu nimma message note madidini. $userName avrige helidaga avaru call madtaare. Dhanyavaada."
        languageTag.startsWith("hi") ->
            "Main sab note kar chuka hoon. $userName aapko call karenge. Shukriya."
        else ->
            "I have noted everything you've shared. I'll pass it all to $userName. Thank you for your patience."
    }

    private fun buildGenericAckResponse(text: String): String = when {
        languageTag.startsWith("kn") ->
            "Arthamaayitu. Innenu yenu heli?"
        languageTag.startsWith("hi") ->
            "Samajh gaya. Aur kuch?"
        else ->
            "I see. Could you tell me more, so I can give $userName an accurate summary?"
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
