package com.aistudio.jarvis.voiceagent.data.backend

import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

data class RateLimitDecision(
    val isAllowed: Boolean,
    val reason: String? = null,
    val retryAfterSeconds: Long = 0,
    val remainingUserRpm: Int = 0,
    val remainingDailyQuota: Int = 0,
    val currentDayUsage: Int = 0
)

/**
 * Token Bucket implementation for rate limiting.
 */
class TokenBucket(
    val maxCapacity: Int,
    val refillRatePerSecond: Double
) {
    private var availableTokens: Double = maxCapacity.toDouble()
    private var lastRefillTimestamp: Long = System.currentTimeMillis()

    @Synchronized
    fun tryConsume(tokens: Int = 1): Boolean {
        refill()
        if (availableTokens >= tokens) {
            availableTokens -= tokens
            return true
        }
        return false
    }

    @Synchronized
    private fun refill() {
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - lastRefillTimestamp) / 1000.0
        if (elapsedSeconds > 0) {
            availableTokens = min(maxCapacity.toDouble(), availableTokens + elapsedSeconds * refillRatePerSecond)
            lastRefillTimestamp = now
        }
    }

    @Synchronized
    fun getAvailableTokens(): Int {
        refill()
        return availableTokens.toInt()
    }
}

/**
 * Server-side Rate Limiter and Quota Management.
 */
class UllasRateLimiter(
    var globalMaxRpm: Int = 15 // Global limit aligned with Gemini free tier (15 RPM)
) {
    private val globalBucket = TokenBucket(
        maxCapacity = globalMaxRpm,
        refillRatePerSecond = globalMaxRpm / 60.0
    )

    private val userBuckets = ConcurrentHashMap<String, TokenBucket>()
    private val userDailyUsage = ConcurrentHashMap<String, Pair<Int, Int>>() // Pair<DayOfYear, Count>

    fun checkRateLimit(userSession: UserSession): RateLimitDecision {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_YEAR)

        // 1. Check Daily Quota
        val currentUsagePair = userDailyUsage.compute(userSession.userId) { _, current ->
            if (current == null || current.first != currentDay) {
                Pair(currentDay, 0)
            } else {
                current
            }
        } ?: Pair(currentDay, 0)

        val currentDayCount = currentUsagePair.second
        val maxDailyQuota = userSession.tier.dailyQuota

        if (currentDayCount >= maxDailyQuota) {
            return RateLimitDecision(
                isAllowed = false,
                reason = "Daily request quota ($maxDailyQuota) exceeded for ${userSession.userName}.",
                retryAfterSeconds = 3600, // Try next cycle
                remainingUserRpm = 0,
                remainingDailyQuota = 0,
                currentDayUsage = currentDayCount
            )
        }

        // 2. Check Global Server Bucket
        if (!globalBucket.tryConsume(1)) {
            return RateLimitDecision(
                isAllowed = false,
                reason = "ULLAS Cloud Gateway is currently experiencing high load. Throttling active.",
                retryAfterSeconds = 2,
                remainingUserRpm = 0,
                remainingDailyQuota = maxDailyQuota - currentDayCount,
                currentDayUsage = currentDayCount
            )
        }

        // 3. Check Per-User Token Bucket
        val userBucket = userBuckets.computeIfAbsent(userSession.userId) {
            TokenBucket(
                maxCapacity = userSession.tier.maxRpm,
                refillRatePerSecond = userSession.tier.maxRpm / 60.0
            )
        }

        if (!userBucket.tryConsume(1)) {
            return RateLimitDecision(
                isAllowed = false,
                reason = "Per-user rate limit reached (${userSession.tier.maxRpm} req/min). Please slow down.",
                retryAfterSeconds = 3,
                remainingUserRpm = 0,
                remainingDailyQuota = maxDailyQuota - currentDayCount,
                currentDayUsage = currentDayCount
            )
        }

        // Increment daily count
        userDailyUsage[userSession.userId] = Pair(currentDay, currentDayCount + 1)

        return RateLimitDecision(
            isAllowed = true,
            remainingUserRpm = userBucket.getAvailableTokens(),
            remainingDailyQuota = maxDailyQuota - (currentDayCount + 1),
            currentDayUsage = currentDayCount + 1
        )
    }

    fun getUserUsage(userId: String, tier: UserTier): Pair<Int, Int> {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_YEAR)
        val usage = userDailyUsage[userId]
        val count = if (usage != null && usage.first == currentDay) usage.second else 0
        return Pair(count, tier.dailyQuota)
    }

    fun resetUserDailyUsage(userId: String) {
        userDailyUsage.remove(userId)
    }
}
