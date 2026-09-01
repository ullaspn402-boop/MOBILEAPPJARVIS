package com.aistudio.jarvis.voiceagent.data.backend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android Client Network Bridge for the ULLAS Backend Gateway.
 * Sends authenticated requests to the ULLAS Backend.
 * Never holds or exposes the Gemini API key on the Android client.
 */
class UllasBackendClient(
    val backendGateway: UllasBackendGateway = UllasBackendGateway()
) {
    // Current authenticated user session on client
    var currentSession: UserSession = backendGateway.authManager.primaryUser

    suspend fun queryUllasBackend(
        query: String,
        contextMemory: Map<String, String>,
        recentHistorySummary: String = ""
    ): UllasBackendResponse = withContext(Dispatchers.IO) {
        val request = UllasBackendRequest(
            authToken = currentSession.authToken,
            query = query,
            contextMemory = contextMemory,
            recentHistorySummary = recentHistorySummary
        )

        backendGateway.processRequest(request)
    }

    fun switchUserSession(session: UserSession) {
        this.currentSession = session
    }
}
