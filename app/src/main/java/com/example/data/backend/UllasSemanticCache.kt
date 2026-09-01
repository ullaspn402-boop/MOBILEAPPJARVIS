package com.aistudio.jarvis.voiceagent.data.backend

import com.aistudio.jarvis.voiceagent.model.ExecutionPlan
import java.util.concurrent.ConcurrentHashMap

data class CachedPlanEntry(
    val plan: ExecutionPlan,
    val timestamp: Long,
    val expiresAt: Long,
    val hitCount: Int = 1
)

/**
 * High-performance Query & Execution Plan Cache.
 * Dramatically reduces redundant AI calls for frequently repeated questions or commands.
 */
class UllasSemanticCache(private var defaultTtlMillis: Long = 30 * 60 * 1000L) { // 30 minutes default

    private val cache = ConcurrentHashMap<String, CachedPlanEntry>()
    private var totalHits = 0L
    private var totalMisses = 0L

    fun get(rawQuery: String): ExecutionPlan? {
        val key = normalizeKey(rawQuery)
        val entry = cache[key] ?: run {
            totalMisses++
            return null
        }

        val now = System.currentTimeMillis()
        if (now > entry.expiresAt) {
            cache.remove(key)
            totalMisses++
            return null
        }

        // Update hit count
        cache[key] = entry.copy(hitCount = entry.hitCount + 1)
        totalHits++
        return entry.plan
    }

    fun put(rawQuery: String, plan: ExecutionPlan, ttlMillis: Long = defaultTtlMillis) {
        val key = normalizeKey(rawQuery)
        val now = System.currentTimeMillis()
        cache[key] = CachedPlanEntry(
            plan = plan,
            timestamp = now,
            expiresAt = now + ttlMillis
        )
    }

    fun normalizeKey(query: String): String {
        return query.lowercase()
            .replace(Regex("^(hey|hi|hello|ok)?\\s*(jarvis|ullas)[,\\s]*"), "")
            .replace(Regex("[?!.,]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun clear() {
        cache.clear()
    }

    fun getStats(): CacheStats {
        val activeEntries = cache.count { System.currentTimeMillis() <= it.value.expiresAt }
        val totalQueries = totalHits + totalMisses
        val hitRate = if (totalQueries > 0) (totalHits.toDouble() / totalQueries * 100).toInt() else 0
        return CacheStats(
            activeEntries = activeEntries,
            totalHits = totalHits,
            totalMisses = totalMisses,
            hitRatePercentage = hitRate
        )
    }

    fun updateTtl(ttlMillis: Long) {
        this.defaultTtlMillis = ttlMillis
    }
}

data class CacheStats(
    val activeEntries: Int,
    val totalHits: Long,
    val totalMisses: Long,
    val hitRatePercentage: Int
)
