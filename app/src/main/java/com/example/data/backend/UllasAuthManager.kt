package com.aistudio.jarvis.voiceagent.data.backend

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class UserTier(val dailyQuota: Int, val maxRpm: Int, val priority: Int) {
    FREE_STANDARD(dailyQuota = 100, maxRpm = 15, priority = 1),
    PRO(dailyQuota = 500, maxRpm = 45, priority = 2),
    ENTERPRISE(dailyQuota = 2500, maxRpm = 120, priority = 3),
    ADMIN(dailyQuota = 10000, maxRpm = 300, priority = 4)
}

data class UserSession(
    val userId: String,
    val authToken: String,
    val userName: String,
    val tier: UserTier = UserTier.FREE_STANDARD,
    val createdAt: Long = System.currentTimeMillis(),
    var isBlockedForAbuse: Boolean = false,
    var abuseReason: String? = null
)

data class AuthValidationResult(
    val isValid: Boolean,
    val userSession: UserSession? = null,
    val errorMessage: String? = null
)

/**
 * Per-user Authentication and Abuse Protection Manager.
 */
class UllasAuthManager {

    private val sessions = ConcurrentHashMap<String, UserSession>()
    private val requestTimestampsByUser = ConcurrentHashMap<String, MutableList<Long>>()

    // Default primary active user session for client
    val primaryUser: UserSession by lazy {
        registerUser(
            userId = "usr_ullas_production_01",
            userName = "Sir",
            tier = UserTier.PRO
        )
    }

    fun registerUser(
        userId: String = "usr_${UUID.randomUUID().toString().take(8)}",
        userName: String = "User",
        tier: UserTier = UserTier.FREE_STANDARD
    ): UserSession {
        val token = "utk_${UUID.randomUUID().toString().replace("-", "").take(24)}"
        val session = UserSession(
            userId = userId,
            authToken = token,
            userName = userName,
            tier = tier
        )
        sessions[token] = session
        return session
    }

    fun validateToken(token: String): AuthValidationResult {
        if (token.isBlank()) {
            return AuthValidationResult(isValid = false, errorMessage = "Authentication token missing.")
        }
        val session = sessions[token] ?: return AuthValidationResult(
            isValid = false,
            errorMessage = "Invalid or expired authorization token."
        )

        if (session.isBlockedForAbuse) {
            return AuthValidationResult(
                isValid = false,
                errorMessage = "Account temporarily locked due to security policy: ${session.abuseReason}"
            )
        }

        // Check for abuse flood (e.g. > 20 requests in 3 seconds)
        val now = System.currentTimeMillis()
        val timestamps = requestTimestampsByUser.computeIfAbsent(session.userId) { mutableListOf() }
        synchronized(timestamps) {
            timestamps.add(now)
            // Keep timestamps within last 5 seconds
            timestamps.removeAll { now - it > 5000L }
            if (timestamps.size > 25) {
                session.isBlockedForAbuse = true
                session.abuseReason = "Automated query flood detected (25+ requests / 5s)."
                return AuthValidationResult(
                    isValid = false,
                    errorMessage = "Abuse detected. Rate limited for security."
                )
            }
        }

        return AuthValidationResult(isValid = true, userSession = session)
    }

    fun unblockUser(userId: String) {
        sessions.values.find { it.userId == userId }?.let {
            it.isBlockedForAbuse = false
            it.abuseReason = null
        }
    }

    fun getAllUsers(): List<UserSession> = sessions.values.toList()
}
