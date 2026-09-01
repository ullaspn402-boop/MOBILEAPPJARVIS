package com.aistudio.jarvis.voiceagent.data.call

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.smartCallDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "smart_call_prefs"
)

/**
 * DataStore-backed repository for [CallAssistantSettings].
 * Provides a reactive Flow of the current settings and suspend functions to update them.
 */
class CallAssistantSettingsRepository(private val context: Context) {

    private val ds = context.smartCallDataStore

    // ─── DataStore preference keys ────────────────────────────────────────────
    private val keyEnabled = booleanPreferencesKey(CallAssistantPrefsKeys.ENABLED)
    private val keyAutoAnswer = booleanPreferencesKey(CallAssistantPrefsKeys.AUTO_ANSWER_ENABLED)
    private val keyAutoAnswerDelay = intPreferencesKey(CallAssistantPrefsKeys.AUTO_ANSWER_DELAY)
    private val keyAutoAnswerScope = stringPreferencesKey(CallAssistantPrefsKeys.AUTO_ANSWER_SCOPE)
    private val keySpeaker = booleanPreferencesKey(CallAssistantPrefsKeys.USE_SPEAKER)
    private val keySaveSummaries = booleanPreferencesKey(CallAssistantPrefsKeys.SAVE_SUMMARIES)
    private val keySaveTranscripts = booleanPreferencesKey(CallAssistantPrefsKeys.SAVE_TRANSCRIPTS)
    private val keySaveAudio = booleanPreferencesKey(CallAssistantPrefsKeys.SAVE_AUDIO)
    private val keyShownDialerExplanation = booleanPreferencesKey(CallAssistantPrefsKeys.SHOWN_DIALER_EXPLANATION)
    private val keyInformCaller = booleanPreferencesKey(CallAssistantPrefsKeys.INFORM_CALLER_OF_AI)

    // ─── Flow ─────────────────────────────────────────────────────────────────
    val settings: Flow<CallAssistantSettings> = ds.data.map { prefs ->
        CallAssistantSettings(
            smartCallAssistantEnabled = prefs[keyEnabled] ?: true,
            autoAnswerEnabled = prefs[keyAutoAnswer] ?: false,
            autoAnswerDelaySeconds = prefs[keyAutoAnswerDelay] ?: 25,
            autoAnswerScope = try {
                AutoAnswerScope.valueOf(prefs[keyAutoAnswerScope] ?: "NEVER")
            } catch (e: Throwable) { AutoAnswerScope.NEVER },
            useSpeakerForJarvisAnswer = prefs[keySpeaker] ?: true,
            saveSummaries = prefs[keySaveSummaries] ?: true,
            saveTranscripts = prefs[keySaveTranscripts] ?: false,
            saveAudio = prefs[keySaveAudio] ?: false,
            hasShownDialerRoleExplanation = prefs[keyShownDialerExplanation] ?: false,
            informCallerOfAi = prefs[keyInformCaller] ?: true
        )
    }

    // ─── Mutators ─────────────────────────────────────────────────────────────
    suspend fun setEnabled(enabled: Boolean) {
        ds.edit { it[keyEnabled] = enabled }
    }

    suspend fun setAutoAnswerEnabled(enabled: Boolean) {
        ds.edit { it[keyAutoAnswer] = enabled }
    }

    suspend fun setAutoAnswerDelay(seconds: Int) {
        ds.edit { it[keyAutoAnswerDelay] = seconds.coerceIn(5, 60) }
    }

    suspend fun setAutoAnswerScope(scope: AutoAnswerScope) {
        ds.edit { it[keyAutoAnswerScope] = scope.name }
    }

    suspend fun setUseSpeaker(use: Boolean) {
        ds.edit { it[keySpeaker] = use }
    }

    suspend fun setSaveSummaries(save: Boolean) {
        ds.edit { it[keySaveSummaries] = save }
    }

    suspend fun setSaveTranscripts(save: Boolean) {
        ds.edit { it[keySaveTranscripts] = save }
    }

    suspend fun setSaveAudio(save: Boolean) {
        // Audio recording is always disabled in the prototype.
        // This key is reserved for a future feature with full consent flow.
        ds.edit { it[keySaveAudio] = false }
    }

    suspend fun setShownDialerExplanation(shown: Boolean) {
        ds.edit { it[keyShownDialerExplanation] = shown }
    }

    suspend fun setInformCallerOfAi(inform: Boolean) {
        ds.edit { it[keyInformCaller] = inform }
    }
}
