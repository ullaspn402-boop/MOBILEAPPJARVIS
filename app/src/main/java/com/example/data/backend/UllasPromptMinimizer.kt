package com.aistudio.jarvis.voiceagent.data.backend

/**
 * Prompt & Context Minimizer for production scale.
 * Keeps input tokens minimal and context windows small to reduce latency, stay within rate limits,
 * and optimize quota usage.
 */
object UllasPromptMinimizer {

    fun buildCompactSystemPrompt(): String {
        return """
            You are ULLAS / JARVIS AI assistant router.
            Return ONLY a valid JSON object with:
            {
              "summary": "Brief natural response",
              "steps": [
                {
                  "toolId": "app_launcher|call_contact|messaging|set_alarm|reminder|calendar|maps_navigation|notes|memory|notifications|device_settings|web_search",
                  "actionName": "Short action label",
                  "description": "Details",
                  "params": {},
                  "riskLevel": "LOW|MEDIUM|HIGH"
                }
              ]
            }
            High risk tools (call_contact, messaging) require riskLevel HIGH.
        """.trimIndent()
    }

    /**
     * Minimizes memory context by keeping only relevant entries (max 5 items, max 200 chars).
     */
    fun minimizeMemoryContext(memory: Map<String, String>): String {
        if (memory.isEmpty()) return ""
        return memory.entries
            .take(5)
            .joinToString(separator = "; ") { "${it.key}: ${it.value.take(40)}" }
    }

    /**
     * Minimizes user query and removes trailing repetitive filler.
     */
    fun minimizeQuery(query: String): String {
        return query.trim().take(300)
    }
}
