package com.aistudio.jarvis.voiceagent.data.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class JarvisVoiceEngine(
    private val context: Context,
    private val scope: CoroutineScope
) : RecognitionListener, TextToSpeech.OnInitListener {

    private val tag = "JarvisVoiceEngine"

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var toneGenerator: ToneGenerator? = null
    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentRms = MutableStateFlow(0f)
    val currentRms: StateFlow<Float> = _currentRms.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow<String?>(null)
    val lastRecognizedText: StateFlow<String?> = _lastRecognizedText.asStateFlow()

    var onSpeechRecognized: ((String) -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null
    var onListeningCancelled: (() -> Unit)? = null

    var wakeWordEnabled: Boolean = true
    var wakePhrase: String = "Hey Jarvis"
    var voicePitch: Float = 1.0f
    var voiceSpeed: Float = 1.05f
    var selectedLanguageCode: String = "en-US"
    var soundEffectsEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    // Queue of text pending TTS initialization
    private val pendingSpeakQueue = mutableListOf<String>()

    init {
        // ✅ Initialize TTS EAGERLY at construction time on the main thread.
        // This ensures TTS is always ready before the user speaks.
        initTtsOnMainThread()
        ensureToneGenerator()
    }

    private fun initTtsOnMainThread() {
        try {
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                // Already on main thread — init directly
                initTts()
            } else {
                // Post to main thread
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    initTts()
                }
            }
        } catch (e: Throwable) {
            Log.e(tag, "initTtsOnMainThread failed", e)
        }
    }

    private fun ensureToneGenerator() {
        if (toneGenerator != null) return
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Throwable) {
            Log.e(tag, "ToneGenerator init failed", e)
        }
    }

    private fun ensureTts() {
        if (textToSpeech != null) return
        initTtsOnMainThread()
    }

    private fun initTts() {
        try {
            if (textToSpeech != null) return  // Already initialized
            Log.i(tag, "Initializing TextToSpeech engine...")
            textToSpeech = TextToSpeech(context, this)
        } catch (e: Throwable) {
            Log.e(tag, "Default TextToSpeech init failed — trying Google TTS engine", e)
            tryGoogleTtsFallback()
        }
    }

    private fun tryGoogleTtsFallback() {
        try {
            Log.i(tag, "Attempting Google TTS engine fallback (com.google.android.tts)...")
            textToSpeech = TextToSpeech(context, this, "com.google.android.tts")
        } catch (e: Throwable) {
            Log.e(tag, "Google TTS engine fallback failed", e)
            _isTtsReady.value = false
        }
    }

    override fun onInit(status: Int) {
        try {
            if (status == TextToSpeech.SUCCESS) {
                Log.i(tag, "✅ TTS engine initialized successfully")
                _isTtsReady.value = true
                setLanguage(selectedLanguageCode)
                textToSpeech?.setPitch(voicePitch)
                textToSpeech?.setSpeechRate(voiceSpeed)

                // Use STREAM_MUSIC so voice is always audible
                try {
                    val audioAttributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build()
                    textToSpeech?.setAudioAttributes(audioAttributes)
                } catch (e: Throwable) {
                    Log.w(tag, "Failed setting AudioAttributes for TTS", e)
                }

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        val cb = activeUtteranceCallback
                        activeUtteranceCallback = null
                        cb?.invoke()
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        val cb = activeUtteranceCallback
                        activeUtteranceCallback = null
                        cb?.invoke()
                    }
                })

                // Flush ALL queued speech now that TTS is ready
                val queuedItems = pendingSpeakQueue.toList()
                pendingSpeakQueue.clear()
                for (item in queuedItems) {
                    Log.i(tag, "Flushing queued speech: $item")
                    speakInternal(item)
                }
            } else {
                Log.e(tag, "❌ TTS initialization failed with status code: $status")
                _isTtsReady.value = false
                // Try fallback to Google TTS package if default engine failed
                if (textToSpeech?.defaultEngine != "com.google.android.tts") {
                    Log.w(tag, "Retrying TTS with Google TTS package...")
                    textToSpeech = null
                    tryGoogleTtsFallback()
                }
            }
        } catch (e: Throwable) {
            _isTtsReady.value = false
            Log.e(tag, "TTS onInit exception", e)
        }
    }

    fun setInCallAudioAttributes(inCall: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (inCall) {
                // MODE_IN_COMMUNICATION is correct for non-default-dialer VoIP apps.
                // MODE_IN_CALL is reserved for the system telephony stack only.
                audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager?.isSpeakerphoneOn = true
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                textToSpeech?.setAudioAttributes(audioAttributes)
                Log.i(tag, "🔊 TTS AudioAttributes set to USAGE_VOICE_COMMUNICATION (MODE_IN_COMMUNICATION) for live call")
            } else {
                audioManager?.isSpeakerphoneOn = false
                audioManager?.mode = AudioManager.MODE_NORMAL
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build()
                textToSpeech?.setAudioAttributes(audioAttributes)
                Log.i(tag, "🔊 TTS AudioAttributes reset to USAGE_MEDIA for standard playback")
            }
        } catch (e: Throwable) {
            Log.w(tag, "Failed setting in-call audio attributes", e)
        }
    }

    fun setLanguage(languageCode: String) {
        selectedLanguageCode = languageCode
        val locale = when (languageCode.lowercase()) {
            "hi-in", "hi", "hindi" -> Locale("hi", "IN")
            "kn-in", "kn", "kannada" -> Locale("kn", "IN")
            "ta-in", "ta", "tamil" -> Locale("ta", "IN")
            "te-in", "te", "telugu" -> Locale("te", "IN")
            "ml-in", "ml", "malayalam" -> Locale("ml", "IN")
            "mr-in", "mr", "marathi" -> Locale("mr", "IN")
            "bn-in", "bn", "bengali" -> Locale("bn", "IN")
            else -> Locale.US
        }
        applyLocaleWithFallback(locale)
    }

    /**
     * Applies a locale to the TTS engine and VERIFIES it actually took effect.
     * setLanguage()/`.language =` on Android's TextToSpeech does NOT throw when a
     * language's voice data isn't installed — it just returns a status code and
     * silently leaves TTS unable to speak from then on. Previously that status
     * code was never checked, so an unsupported/uninstalled language (e.g. a
     * regional Indian language whose voice pack isn't downloaded on-device)
     * would leave Jarvis completely silent with no error surfaced anywhere.
     */
    private fun applyLocaleWithFallback(locale: Locale) {
        val tts = textToSpeech
        if (tts == null) {
            Log.w(tag, "applyLocaleWithFallback: TTS not initialized yet, locale will be set on init")
            return
        }
        try {
            val result = tts.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(
                    tag,
                    "⚠️ TTS language '$locale' unsupported or voice data missing (status=$result). " +
                        "Falling back to English (US) so Jarvis can still speak. " +
                        "Install this language's voice pack under Settings > System > Languages > Text-to-speech."
                )
                if (locale != Locale.US) {
                    val fallbackResult = tts.setLanguage(Locale.US)
                    if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(tag, "❌ Even English (US) TTS voice data is missing on this device — TTS engine may need reinstalling/updating.")
                    }
                }
                onSpeechError?.invoke("Voice for '$locale' isn't installed on this device — using English instead. Install it under Settings > Languages > Text-to-speech.")
            } else {
                Log.i(tag, "✅ TTS language set to $locale (status=$result)")
            }
        } catch (e: Throwable) {
            Log.e(tag, "setLanguage threw for $locale, falling back to English (US)", e)
            try { tts.setLanguage(Locale.US) } catch (t: Throwable) { }
        }
    }

    fun setVoiceCharacteristics(pitch: Float, speed: Float) {
        voicePitch = pitch
        voiceSpeed = speed
        try {
            textToSpeech?.setPitch(pitch)
            textToSpeech?.setSpeechRate(speed)
        } catch (e: Throwable) {
            Log.w(tag, "Failed setting TTS characteristics", e)
        }
    }

    fun startListening() {
        if (_isListening.value) return

        try {
            ensureTts()
            ensureToneGenerator()
            stopSpeaking()
            playActivationTone()
            triggerHaptic()
        } catch (e: Throwable) {
            // Ignore feedback errors
        }

        scope.launch(Dispatchers.Main) {
            try {
                try {
                    speechRecognizer?.destroy()
                } catch (e: Throwable) {
                    // Ignore previous cleanup errors
                }
                speechRecognizer = null

                try {
                    if (SpeechRecognizer.isRecognitionAvailable(context)) {
                        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                            setRecognitionListener(this@JarvisVoiceEngine)
                        }
                    } else {
                        onSpeechError?.invoke("Speech recognition is not available on this device.")
                        return@launch
                    }
                } catch (e: Throwable) {
                    Log.e(tag, "SpeechRecognizer creation failed", e)
                    onSpeechError?.invoke("Could not initialize speech recognizer.")
                    return@launch
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguageCode)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                _liveTranscript.value = ""
                _isListening.value = true
                speechRecognizer?.startListening(intent)
            } catch (e: Throwable) {
                Log.w(tag, "startListening exception: ${e.message}")
                _isListening.value = false
                onSpeechError?.invoke("Could not initialize microphone: ${e.localizedMessage}")
            }
        }
    }

    fun stopListening() {
        scope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Throwable) {
                Log.w(tag, "stopListening error: ${e.message}")
            } finally {
                _isListening.value = false
                _currentRms.value = 0f
            }
        }
    }

    private var activeUtteranceCallback: (() -> Unit)? = null

    fun speak(text: String, onCompleted: (() -> Unit)? = null) {
        ensureTts()
        activeUtteranceCallback = onCompleted
        if (!_isTtsReady.value || textToSpeech == null) {
            // TTS still initializing — add to queue; onInit will flush it
            Log.w(tag, "TTS not ready — queuing: $text")
            pendingSpeakQueue.add(text)
            return
        }
        speakInternal(text)
    }

    private fun speakInternal(text: String) {
        try {
            val utteranceId = "JARVIS_${System.currentTimeMillis()}"
            // Clean markdown formatting (stars, hashtags, backticks, brackets) so TTS speaks natural sentences
            val cleanText = text
                .replace(Regex("<[^>]*>"), "")
                .replace(Regex("\\*\\*|\\*|_|#|`|\\[.*?\\]\\(.*?\\)"), "")
                .trim()

            val textToUtter = if (cleanText.isNotBlank()) cleanText else text
            Log.i(tag, "🔊 Speaking: $textToUtter")
            val result = textToSpeech?.speak(textToUtter, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                Log.e(tag, "❌ TextToSpeech.speak() returned failure code $result for: \"$textToUtter\"")
                _isSpeaking.value = false
                val cb = activeUtteranceCallback
                activeUtteranceCallback = null
                cb?.invoke()
            }
        } catch (e: Throwable) {
            Log.e(tag, "TTS speak exception", e)
            _isSpeaking.value = false
            val cb = activeUtteranceCallback
            activeUtteranceCallback = null
            cb?.invoke()
        }
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
            _isSpeaking.value = false
        } catch (e: Throwable) {
            Log.w(tag, "stopSpeaking error: ${e.message}")
        }
    }

    private fun playActivationTone() {
        if (!soundEffectsEnabled) return
        ensureToneGenerator()
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (e: Throwable) {
            // Ignore
        }
    }

    fun playCompletionTone() {
        if (!soundEffectsEnabled) return
        ensureToneGenerator()
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
        } catch (e: Throwable) {
            // Ignore
        }
    }

    fun playErrorTone() {
        if (!soundEffectsEnabled) return
        ensureToneGenerator()
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
        } catch (e: Throwable) {
            // Ignore
        }
    }

    private fun triggerHaptic() {
        if (!hapticsEnabled) return
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(40)
                }
            }
        } catch (e: Throwable) {
            // Ignore haptic errors
        }
    }

    // --- RecognitionListener Callbacks ---

    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {
        _isListening.value = true
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Normalize RMS dB (typical range -2 to 10 dB) to 0.0 .. 1.0 for orb animation
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _currentRms.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
        _currentRms.value = 0f
    }

    override fun onError(error: Int) {
        _isListening.value = false
        _currentRms.value = 0f

        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                // Normal user silence or listening timeout - not a critical error
                Log.d(tag, "Speech recognition completed with no speech detected ($error)")
                onListeningCancelled?.invoke()
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                Log.d(tag, "Speech recognizer busy, resetting ($error)")
                try {
                    speechRecognizer?.cancel()
                } catch (e: Exception) { }
                onListeningCancelled?.invoke()
            }
            SpeechRecognizer.ERROR_CLIENT -> {
                Log.d(tag, "Speech recognizer client cancelled or reset ($error)")
                onListeningCancelled?.invoke()
            }
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                Log.w(tag, "SpeechRecognizer missing microphone permission ($error)")
                onSpeechError?.invoke("Microphone permission required.")
            }
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                Log.w(tag, "SpeechRecognizer network issue ($error)")
                onSpeechError?.invoke("Network error during speech recognition.")
            }
            SpeechRecognizer.ERROR_AUDIO -> {
                Log.w(tag, "SpeechRecognizer audio recording error ($error)")
                onSpeechError?.invoke("Audio recording error. Please check microphone.")
            }
            SpeechRecognizer.ERROR_SERVER -> {
                Log.w(tag, "SpeechRecognizer server error ($error)")
                onSpeechError?.invoke("Voice recognition service temporarily unavailable.")
            }
            else -> {
                Log.d(tag, "SpeechRecognizer code: $error")
                onListeningCancelled?.invoke()
            }
        }
    }

    private var lastRecognizedSpeech: String = ""
    private var lastRecognizedSpeechTimestamp: Long = 0L

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        _currentRms.value = 0f
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val recognized = matches?.firstOrNull()?.trim()
        val now = System.currentTimeMillis()

        if (!recognized.isNullOrBlank()) {
            // Debounce identical speech results firing within 1.5 seconds
            if (recognized.equals(lastRecognizedSpeech, ignoreCase = true) && (now - lastRecognizedSpeechTimestamp) < 1500L) {
                Log.d(tag, "Debounced duplicate speech recognition result: '$recognized'")
                return
            }

            lastRecognizedSpeech = recognized
            lastRecognizedSpeechTimestamp = now
            _lastRecognizedText.value = recognized
            _liveTranscript.value = recognized
            onSpeechRecognized?.invoke(recognized)
        } else {
            onListeningCancelled?.invoke()
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull()
        if (!partial.isNullOrBlank()) {
            _liveTranscript.value = partial
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            toneGenerator?.release()
            toneGenerator = null
            pendingSpeakQueue.clear()
        } catch (e: Exception) {
            Log.e(tag, "destroy error", e)
        }
    }
}
