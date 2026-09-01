package com.aistudio.jarvis.voiceagent.viewmodel

import android.app.Activity
import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import java.lang.ref.WeakReference
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.jarvis.voiceagent.data.ai.JarvisAgentEngine
import com.aistudio.jarvis.voiceagent.data.backend.AdminConfig
import com.aistudio.jarvis.voiceagent.data.backend.BackendTelemetry
import com.aistudio.jarvis.voiceagent.data.backend.UserSession
import com.aistudio.jarvis.voiceagent.data.backend.UserTier
import com.aistudio.jarvis.voiceagent.data.call.AutoAnswerScope
import com.aistudio.jarvis.voiceagent.data.call.CallAssistantSettings
import com.aistudio.jarvis.voiceagent.data.call.CallAssistantSettingsRepository
import com.aistudio.jarvis.voiceagent.data.call.CallSummaryEntity
import com.aistudio.jarvis.voiceagent.data.db.AppDatabase
import com.aistudio.jarvis.voiceagent.data.db.HistoryEntity
import com.aistudio.jarvis.voiceagent.data.db.MemoryEntity
import com.aistudio.jarvis.voiceagent.data.db.NoteEntity
import com.aistudio.jarvis.voiceagent.data.db.ReminderEntity
import com.aistudio.jarvis.voiceagent.data.repository.JarvisRepository
import com.aistudio.jarvis.voiceagent.data.service.CapturedNotification
import com.aistudio.jarvis.voiceagent.data.service.JarvisNotificationListenerService
import com.aistudio.jarvis.voiceagent.data.service.JarvisVoiceEngine
import com.aistudio.jarvis.voiceagent.model.ConversationTurn
import com.aistudio.jarvis.voiceagent.model.ExecutionPlan
import com.aistudio.jarvis.voiceagent.model.JarvisState
import com.aistudio.jarvis.voiceagent.model.PlanStep
import com.aistudio.jarvis.voiceagent.tools.RiskLevel
import com.aistudio.jarvis.voiceagent.tools.ToolRegistry
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext

    // WeakReference to Activity — used by tools that must launch from an Activity context
    // (e.g. ACTION_CALL on Android 10+). Cleared in onDestroy to avoid memory leaks.
    private var activityRef: WeakReference<Activity>? = null

    fun setActivityContext(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun clearActivityContext() {
        activityRef = null
    }

    /** Returns the live Activity if available, otherwise falls back to application context. */
    fun getCallableContext(): Context = activityRef?.get() ?: context

    private val database = AppDatabase.getDatabase(context)

    private val repository = JarvisRepository(database.jarvisDao())

    private val agentEngine = JarvisAgentEngine()

    val voiceEngine = JarvisVoiceEngine(context, viewModelScope)

    // ─── Smart Call Assistant ─────────────────────────────────────────────────
    private val smartCallSettingsRepo = CallAssistantSettingsRepository(context)

    val smartCallSettings: StateFlow<CallAssistantSettings> = smartCallSettingsRepo.settings
        .catch { emit(CallAssistantSettings()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CallAssistantSettings())

    val callSummaries: StateFlow<List<CallSummaryEntity>> = repository.allCallSummaries
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- ULLAS Production Backend Telemetry & Admin States ---
    val backendGateway = agentEngine.backendGateway
    private val _backendTelemetry = MutableStateFlow(
        try {
            backendGateway.usageMonitor.getTelemetry()
        } catch (e: Throwable) {
            BackendTelemetry()
        }
    )
    val backendTelemetry: StateFlow<BackendTelemetry> = _backendTelemetry.asStateFlow()

    private val _currentUserSession = MutableStateFlow(
        try {
            backendGateway.authManager.primaryUser
        } catch (e: Throwable) {
            UserSession(
                userId = "local",
                authToken = "local",
                userName = "Ullas"
            )
        }
    )
    val currentUserSession: StateFlow<UserSession> = _currentUserSession.asStateFlow()

    private val _adminConfig = MutableStateFlow(
        try {
            backendGateway.adminConfig.config
        } catch (e: Throwable) {
            AdminConfig()
        }
    )
    val adminConfig: StateFlow<AdminConfig> = _adminConfig.asStateFlow()

    fun refreshBackendTelemetry() {
        _backendTelemetry.value = backendGateway.usageMonitor.getTelemetry(
            activeConcurrency = backendGateway.requestQueue.getActiveCallCount(),
            activeQueueSize = backendGateway.requestQueue.getQueuedCallCount()
        )
    }

    fun updateAdminGlobalRpm(rpm: Int) {
        backendGateway.adminConfig.updateGlobalRpm(rpm)
        backendGateway.rateLimiter.globalMaxRpm = rpm
        _adminConfig.value = backendGateway.adminConfig.config
    }

    fun updateAdminConcurrency(concurrency: Int) {
        backendGateway.adminConfig.updateMaxConcurrency(concurrency)
        backendGateway.requestQueue.updateConcurrencyLimit(concurrency)
        _adminConfig.value = backendGateway.adminConfig.config
    }

    fun updateAdminCacheTtl(minutes: Int) {
        backendGateway.adminConfig.updateCacheTtlMinutes(minutes)
        backendGateway.semanticCache.updateTtl(minutes * 60 * 1000L)
        _adminConfig.value = backendGateway.adminConfig.config
    }

    fun switchActiveAiProvider(providerId: String) {
        backendGateway.providerRegistry.switchProvider(providerId)
        backendGateway.adminConfig.updateActiveProvider(providerId)
        _adminConfig.value = backendGateway.adminConfig.config
    }

    fun switchUserTier(tier: UserTier) {
        val updated = _currentUserSession.value.copy(tier = tier)
        _currentUserSession.value = updated
        agentEngine.backendClient.switchUserSession(updated)
    }

    fun clearBackendCache() {
        backendGateway.semanticCache.clear()
        refreshBackendTelemetry()
    }

    fun resetBackendMetrics() {
        backendGateway.usageMonitor.resetMetrics()
        refreshBackendTelemetry()
    }

    fun resetUserDailyUsage() {
        backendGateway.rateLimiter.resetUserDailyUsage(_currentUserSession.value.userId)
        refreshBackendTelemetry()
    }

    // --- Observable UI States ---
    private val _jarvisState = MutableStateFlow(JarvisState.READY)
    val jarvisState: StateFlow<JarvisState> = _jarvisState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Tap microphone or say \"Hey Jarvis\"")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _currentPlan = MutableStateFlow<ExecutionPlan?>(null)
    val currentPlan: StateFlow<ExecutionPlan?> = _currentPlan.asStateFlow()

    private val _pendingConfirmationPlan = MutableStateFlow<ExecutionPlan?>(null)
    val pendingConfirmationPlan: StateFlow<ExecutionPlan?> = _pendingConfirmationPlan.asStateFlow()

    private val _conversationHistory = MutableStateFlow<List<ConversationTurn>>(emptyList())
    val conversationHistory: StateFlow<List<ConversationTurn>> = _conversationHistory.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- User Settings ---
    private val _userName = MutableStateFlow("Ullas")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _wakeWordEnabled = MutableStateFlow(true)
    val wakeWordEnabled: StateFlow<Boolean> = _wakeWordEnabled.asStateFlow()

    private val _wakePhrase = MutableStateFlow("Hey Jarvis")
    val wakePhrase: StateFlow<String> = _wakePhrase.asStateFlow()

    private val _languageCode = MutableStateFlow("en-US")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    private val _voicePitch = MutableStateFlow(1.0f)
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    private val _voiceSpeed = MutableStateFlow(1.05f)
    val voiceSpeed: StateFlow<Float> = _voiceSpeed.asStateFlow()

    private val _soundEffects = MutableStateFlow(true)
    val soundEffects: StateFlow<Boolean> = _soundEffects.asStateFlow()

    private val _haptics = MutableStateFlow(true)
    val haptics: StateFlow<Boolean> = _haptics.asStateFlow()

    private val _offlineMode = MutableStateFlow(false)
    val offlineMode: StateFlow<Boolean> = _offlineMode.asStateFlow()

    // --- Flows from Database ---
    val allHistory: StateFlow<List<HistoryEntity>> = repository.allHistory
        .catch { e ->
            Log.e("JarvisViewModel", "Error in history flow", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMemory: StateFlow<List<MemoryEntity>> = repository.allMemory
        .catch { e ->
            Log.e("JarvisViewModel", "Error in memory flow", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotes
        .catch { e ->
            Log.e("JarvisViewModel", "Error in notes flow", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReminders: StateFlow<List<ReminderEntity>> = repository.allReminders
        .catch { e ->
            Log.e("JarvisViewModel", "Error in reminders flow", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentNotifications: StateFlow<List<CapturedNotification>> =
        JarvisNotificationListenerService.recentNotifications

    init {
        setupVoiceCallbacks()
        seedInitialMemoryIfEmpty()
    }

    private fun setupVoiceCallbacks() {
        voiceEngine.onSpeechRecognized = { recognizedText ->
            processCommand(recognizedText)
        }

        voiceEngine.onListeningCancelled = {
            if (_jarvisState.value == JarvisState.LISTENING) {
                _jarvisState.value = JarvisState.READY
                _statusMessage.value = "Tap microphone or say \"Hey Jarvis\""
            }
        }

        voiceEngine.onSpeechError = { err ->
            _errorMessage.value = err
            _jarvisState.value = JarvisState.ERROR
            _statusMessage.value = err
            viewModelScope.launch {
                delay(3500)
                if (_jarvisState.value == JarvisState.ERROR) {
                    _jarvisState.value = JarvisState.READY
                    _statusMessage.value = "Say \"Hey Jarvis\" or tap the microphone."
                }
            }
        }
    }

    private fun seedInitialMemoryIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = repository.getMemoryMap()
                if (existing.isEmpty()) {
                    repository.saveMemory(MemoryEntity(key = "COLLEGE", value = "BMS Institute of Technology", category = "SAVED_LOCATION"))
                    repository.saveMemory(MemoryEntity(key = "HOME_ADDRESS", value = "124 Innovation Avenue", category = "SAVED_LOCATION"))
                    repository.saveMemory(MemoryEntity(key = "USER_NAME", value = "Sir", category = "PROFILE"))
                    repository.saveMemory(MemoryEntity(key = "FAVORITE_TOPIC", value = "Artificial Intelligence & Space", category = "PREFERENCE"))
                }
            } catch (e: Throwable) {
                Log.e("JarvisViewModel", "Error seeding initial memory", e)
            }
        }
    }

    fun toggleVoiceListening() {
        try {
            if (voiceEngine.isListening.value) {
                voiceEngine.stopListening()
                _jarvisState.value = JarvisState.READY
                _statusMessage.value = "Listening paused."
            } else {
                _jarvisState.value = JarvisState.LISTENING
                _statusMessage.value = "Listening..."
                _errorMessage.value = null
                voiceEngine.startListening()
            }
        } catch (e: Throwable) {
            Log.e("JarvisViewModel", "Error toggling voice listening", e)
            _jarvisState.value = JarvisState.ERROR
            _statusMessage.value = "Voice engine unavailable."
        }
    }

    private var lastProcessedQuery: String = ""
    private var lastProcessedTime: Long = 0L

    fun processCommand(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        val now = System.currentTimeMillis()
        // Prevent concurrent execution loop if already thinking/executing
        if (_jarvisState.value == JarvisState.THINKING || _jarvisState.value == JarvisState.EXECUTING) {
            Log.d("JarvisViewModel", "Ignoring command '$trimmed' because state is ${_jarvisState.value}")
            return
        }

        // Prevent duplicate execution of identical queries within 1.8 seconds
        if (trimmed.equals(lastProcessedQuery, ignoreCase = true) && (now - lastProcessedTime) < 1800L) {
            Log.d("JarvisViewModel", "De-duplicating command '$trimmed'")
            return
        }

        lastProcessedQuery = trimmed
        lastProcessedTime = now

        // Append user turn
        val userTurn = ConversationTurn(isUser = true, text = trimmed)
        _conversationHistory.value = _conversationHistory.value + userTurn

        _jarvisState.value = JarvisState.THINKING
        _statusMessage.value = "Analyzing command..."
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val memoryMap = repository.getMemoryMap()
                val plan = agentEngine.planActions(
                    query = query,
                    userPreferredName = _userName.value,
                    contextMemory = memoryMap
                )

                _currentPlan.value = plan

                if (plan.steps.isEmpty()) {
                    // Direct response / greeting
                    val reply = plan.finalSpokenSummary ?: "Yes, ${_userName.value}. How can I assist you?"
                    _jarvisState.value = JarvisState.COMPLETED
                    _statusMessage.value = reply
                    voiceEngine.speak(reply)
                    voiceEngine.playCompletionTone()

                    val botTurn = ConversationTurn(isUser = false, text = reply, plan = plan)
                    _conversationHistory.value = _conversationHistory.value + botTurn

                    repository.addHistory(
                        HistoryEntity(
                            userQuery = query,
                            responseText = reply,
                            status = "COMPLETED"
                        )
                    )
                    refreshBackendTelemetry()

                    delay(2500)
                    if (_jarvisState.value == JarvisState.COMPLETED) {
                        _jarvisState.value = JarvisState.READY
                        _statusMessage.value = "Say \"Hey Jarvis\" or tap to talk."
                    }
                    return@launch
                }

                // Check for high-risk action confirmation requirement
                if (plan.requiresConfirmation && plan.confirmationStep != null) {
                    _pendingConfirmationPlan.value = plan
                    _jarvisState.value = JarvisState.CONFIRMATION_REQUIRED
                    val confirmMsg = "Confirmation required for ${plan.confirmationStep.actionName}. Proceed?"
                    _statusMessage.value = confirmMsg
                    voiceEngine.speak("Please confirm: ${plan.confirmationStep.description}")
                    return@launch
                }

                // Execute the planned steps
                executePlan(plan)

            } catch (e: Exception) {
                _jarvisState.value = JarvisState.ERROR
                _statusMessage.value = "Execution failed: ${e.localizedMessage}"
                voiceEngine.speak("I encountered an error executing that request.")
                voiceEngine.playErrorTone()
            }
        }
    }

    fun confirmPendingAction() {
        val plan = _pendingConfirmationPlan.value ?: return
        _pendingConfirmationPlan.value = null
        executePlan(plan)
    }

    fun cancelPendingAction() {
        val plan = _pendingConfirmationPlan.value
        _pendingConfirmationPlan.value = null
        _jarvisState.value = JarvisState.READY
        _statusMessage.value = "Action cancelled."
        voiceEngine.speak("Action cancelled.")
        if (plan != null) {
            viewModelScope.launch {
                repository.addHistory(
                    HistoryEntity(
                        userQuery = plan.originalQuery,
                        responseText = "Action cancelled by user.",
                        status = "CANCELLED"
                    )
                )
            }
        }
    }

    private fun executePlan(plan: ExecutionPlan) {
        _jarvisState.value = JarvisState.EXECUTING
        _statusMessage.value = "Executing actions..."

        viewModelScope.launch {
            val updatedSteps = plan.steps.toMutableList()
            val spokenResponses = mutableListOf<String>()

            for (i in updatedSteps.indices) {
                val step = updatedSteps[i]
                updatedSteps[i] = step.copy(isExecuting = true)
                _currentPlan.value = plan.copy(steps = updatedSteps.toList())
                _statusMessage.value = "Executing: ${step.actionName}"

                delay(400) // Visual progress feedback

                val tool = ToolRegistry.getTool(step.toolId)
                if (tool != null) {
                    try {
                        val result = tool.execute(getCallableContext(), step.params)
                        if (result.isSuccess) {
                            updatedSteps[i] = step.copy(isExecuting = false, isCompleted = true)
                            spokenResponses.add(result.spokenMessage)
                        } else {
                            updatedSteps[i] = step.copy(isExecuting = false, isCompleted = false, errorMessage = result.displayMessage)
                            spokenResponses.add(result.spokenMessage)
                        }
                    } catch (e: Exception) {
                        updatedSteps[i] = step.copy(isExecuting = false, isCompleted = false, errorMessage = e.localizedMessage)
                        spokenResponses.add("Error in ${step.actionName}")
                    }
                } else {
                    updatedSteps[i] = step.copy(isExecuting = false, isCompleted = false, errorMessage = "Tool not found")
                }

                _currentPlan.value = plan.copy(steps = updatedSteps.toList())
            }

            _jarvisState.value = JarvisState.COMPLETED
            val finalSpoken = if (plan.isMultiStep && plan.finalSpokenSummary != null) {
                plan.finalSpokenSummary
            } else {
                spokenResponses.firstOrNull() ?: "Done."
            }

            _statusMessage.value = finalSpoken
            voiceEngine.speak(finalSpoken)
            voiceEngine.playCompletionTone()

            val botTurn = ConversationTurn(
                isUser = false,
                text = finalSpoken,
                plan = plan.copy(steps = updatedSteps.toList())
            )
            _conversationHistory.value = _conversationHistory.value + botTurn

            // Record into History
            repository.addHistory(
                HistoryEntity(
                    userQuery = plan.originalQuery,
                    responseText = finalSpoken,
                    status = if (updatedSteps.all { it.isCompleted }) "COMPLETED" else "PARTIAL",
                    toolUsed = plan.steps.joinToString { it.toolId },
                    executionDetails = updatedSteps.joinToString("; ") { "${it.actionName}: ${if (it.isCompleted) "OK" else it.errorMessage ?: "Failed"}" }
                )
            )
            refreshBackendTelemetry()

            delay(4000)
            if (_jarvisState.value == JarvisState.COMPLETED) {
                _jarvisState.value = JarvisState.READY
                _statusMessage.value = "Say \"Hey Jarvis\" or tap the microphone."
            }
        }
    }

    // --- History & Memory Management ---
    fun deleteHistoryEntry(id: Long) = viewModelScope.launch {
        repository.deleteHistory(id)
    }

    fun clearAllHistory() = viewModelScope.launch {
        repository.clearHistory()
        _conversationHistory.value = emptyList()
    }

    fun saveMemoryFact(key: String, value: String, category: String = "PREFERENCE") = viewModelScope.launch {
        repository.saveMemory(MemoryEntity(key = key, value = value, category = category))
    }

    fun deleteMemoryEntry(id: Long) = viewModelScope.launch {
        repository.deleteMemoryById(id)
    }

    fun clearAllMemory() = viewModelScope.launch {
        repository.clearMemory()
    }

    fun saveNote(title: String, content: String) = viewModelScope.launch {
        repository.saveNote(NoteEntity(title = title, content = content))
    }

    fun deleteNote(id: Long) = viewModelScope.launch {
        repository.deleteNote(id)
    }

    fun clearAllNotes() = viewModelScope.launch {
        repository.clearNotes()
    }

    fun deleteReminderById(id: Long) = viewModelScope.launch {
        repository.deleteReminder(id)
    }

    // --- Settings Updaters ---
    fun updateUserName(name: String) {
        _userName.value = name
        viewModelScope.launch {
            repository.saveMemory(MemoryEntity(key = "USER_NAME", value = name, category = "PROFILE"))
        }
    }

    fun updateLanguage(code: String) {
        _languageCode.value = code
        voiceEngine.setLanguage(code)
    }

    fun updateVoiceCharacteristics(pitch: Float, speed: Float) {
        _voicePitch.value = pitch
        _voiceSpeed.value = speed
        voiceEngine.setVoiceCharacteristics(pitch, speed)
    }

    fun toggleWakeWord(enabled: Boolean) {
        _wakeWordEnabled.value = enabled
        voiceEngine.wakeWordEnabled = enabled
    }

    fun updateWakePhrase(phrase: String) {
        _wakePhrase.value = phrase
        voiceEngine.wakePhrase = phrase
    }

    fun toggleSoundEffects(enabled: Boolean) {
        _soundEffects.value = enabled
        voiceEngine.soundEffectsEnabled = enabled
    }

    fun toggleHaptics(enabled: Boolean) {
        _haptics.value = enabled
        voiceEngine.hapticsEnabled = enabled
    }

    fun toggleOfflineMode(enabled: Boolean) {
        _offlineMode.value = enabled
    }

    override fun onCleared() {
        super.onCleared()
        clearActivityContext()
        try {
            voiceEngine.destroy()
        } catch (e: Throwable) {
            Log.e("JarvisViewModel", "Error destroying voice engine", e)
        }
    }

    // ─── Smart Call Assistant — Settings ──────────────────────────────────────

    fun updateSmartCallEnabled(enabled: Boolean) = viewModelScope.launch {
        smartCallSettingsRepo.setEnabled(enabled)
    }

    fun updateSmartCallAutoAnswer(enabled: Boolean) = viewModelScope.launch {
        smartCallSettingsRepo.setAutoAnswerEnabled(enabled)
    }

    fun updateSmartCallAutoAnswerDelay(seconds: Int) = viewModelScope.launch {
        smartCallSettingsRepo.setAutoAnswerDelay(seconds)
    }

    fun updateSmartCallAutoAnswerScope(scope: AutoAnswerScope) = viewModelScope.launch {
        smartCallSettingsRepo.setAutoAnswerScope(scope)
    }

    fun updateSmartCallUseSpeaker(use: Boolean) = viewModelScope.launch {
        smartCallSettingsRepo.setUseSpeaker(use)
    }

    fun updateSmartCallSaveSummaries(save: Boolean) = viewModelScope.launch {
        smartCallSettingsRepo.setSaveSummaries(save)
    }

    fun updateSmartCallSaveTranscripts(save: Boolean) = viewModelScope.launch {
        smartCallSettingsRepo.setSaveTranscripts(save)
    }

    fun updateSmartCallInformCaller(inform: Boolean) = viewModelScope.launch {
        smartCallSettingsRepo.setInformCallerOfAi(inform)
    }

    /**
     * Request the Android default dialer role (Android 10+).
     * Requires an Activity context — call from UI layer.
     */
    fun requestDefaultDialerRole() {
        val activity = activityRef?.get() ?: run {
            Log.w("JarvisViewModel", "No activity context for dialer role request")
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = activity.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    val roleIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                    activity.startActivityForResult(roleIntent, 9002)
                    viewModelScope.launch {
                        smartCallSettingsRepo.setShownDialerExplanation(true)
                    }
                }
            } else {
                // Android < 10: open the default apps settings page
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        } catch (e: Throwable) {
            Log.e("JarvisViewModel", "Error requesting default dialer role", e)
        }
    }

    // ─── Smart Call Assistant — Call Summary CRUD ─────────────────────────────

    fun deleteCallSummary(id: Long) = viewModelScope.launch {
        repository.deleteCallSummary(id)
    }

    fun clearAllCallSummaries() = viewModelScope.launch {
        repository.clearAllCallSummaries()
    }
}
