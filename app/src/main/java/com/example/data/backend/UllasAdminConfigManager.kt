package com.aistudio.jarvis.voiceagent.data.backend

data class AdminConfig(
    var globalMaxRpm: Int = 15,
    var maxConcurrency: Int = 3,
    var cacheTtlMinutes: Int = 30,
    var activeProviderId: String = "gemini_2_0_flash",
    var circuitBreakerEnabled: Boolean = true,
    var standardDailyQuota: Int = 100,
    var proDailyQuota: Int = 500
)

/**
 * Admin Configuration Manager for global limits and infrastructure controls.
 */
class UllasAdminConfigManager {
    val config = AdminConfig()

    fun updateGlobalRpm(rpm: Int) {
        config.globalMaxRpm = rpm.coerceIn(10, 600)
    }

    fun updateMaxConcurrency(concurrency: Int) {
        config.maxConcurrency = concurrency.coerceIn(1, 10)
    }

    fun updateCacheTtlMinutes(minutes: Int) {
        config.cacheTtlMinutes = minutes.coerceIn(1, 1440)
    }

    fun updateActiveProvider(providerId: String) {
        config.activeProviderId = providerId
    }

    fun toggleCircuitBreaker(enabled: Boolean) {
        config.circuitBreakerEnabled = enabled
    }
}
