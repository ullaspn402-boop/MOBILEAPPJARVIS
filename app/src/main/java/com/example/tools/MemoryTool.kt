package com.aistudio.jarvis.voiceagent.tools

import android.content.Context
import com.aistudio.jarvis.voiceagent.data.db.AppDatabase
import com.aistudio.jarvis.voiceagent.data.db.MemoryEntity

class MemoryTool : JarvisTool {
    override val id: String = "memory"
    override val name: String = "Personal Memory"
    override val description: String = "Stores and recalls personal facts, routines, preferred locations, and context."
    override val category: String = "Intelligence & Memory"
    override val riskLevel: RiskLevel = RiskLevel.LOW
    override val requiredPermissions: List<String> = emptyList()
    override val examplePhrases: List<String> = listOf(
        "Remember that my college is BMSIT",
        "Remember my car is parked on floor 2",
        "What did I ask you to remember?",
        "What is my college?"
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolExecutionResult {
        val action = (params["action"] as? String ?: "recall").lowercase()
        val key = (params["key"] as? String ?: "").trim()
        val value = (params["value"] as? String ?: params["fact"] as? String ?: "").trim()
        val category = (params["category"] as? String ?: "PREFERENCE").uppercase()

        val db = AppDatabase.getDatabase(context)
        val dao = db.jarvisDao()

        return when (action) {
            "save", "store", "remember" -> {
                if (value.isBlank()) {
                    return ToolExecutionResult(
                        isSuccess = false,
                        spokenMessage = "What information would you like me to remember?",
                        displayMessage = "No information provided to remember."
                    )
                }

                val memKey = if (key.isNotBlank()) key else generateKeyFromFact(value)
                val existing = dao.getMemoryByKey(memKey)
                val entity = if (existing != null) {
                    existing.copy(value = value, updatedAt = System.currentTimeMillis())
                } else {
                    MemoryEntity(key = memKey, value = value, category = category)
                }
                dao.insertOrUpdateMemory(entity)

                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "I've committed that to memory.",
                    displayMessage = "Remembered: $memKey = \"$value\"",
                    payload = entity
                )
            }
            "delete" -> {
                if (key.isNotBlank()) {
                    val existing = dao.getMemoryByKey(key)
                    if (existing != null) {
                        dao.deleteMemory(existing)
                    }
                }
                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "Memory deleted.",
                    displayMessage = "Deleted memory for: $key"
                )
            }
            else -> {
                // Recall
                val query = if (key.isNotBlank()) key else value
                val results = if (query.isBlank()) {
                    dao.searchMemory("")
                } else {
                    dao.searchMemory(query)
                }

                if (results.isEmpty()) {
                    ToolExecutionResult(
                        isSuccess = true,
                        spokenMessage = "I couldn't find anything matching that in your memory.",
                        displayMessage = "No memory entries found.",
                        payload = emptyList<MemoryEntity>()
                    )
                } else {
                    val top = results.first()
                    ToolExecutionResult(
                        isSuccess = true,
                        spokenMessage = "According to my memory: ${top.value}",
                        displayMessage = "Memory: ${top.key} → ${top.value}",
                        payload = results
                    )
                }
            }
        }
    }

    private fun generateKeyFromFact(fact: String): String {
        val lower = fact.lowercase()
        return when {
            lower.contains("college") || lower.contains("university") || lower.contains("school") -> "COLLEGE"
            lower.contains("name") -> "USER_NAME"
            lower.contains("home") -> "HOME_ADDRESS"
            lower.contains("work") || lower.contains("office") -> "WORK_ADDRESS"
            lower.contains("car") || lower.contains("park") -> "PARKING_LOCATION"
            lower.contains("wifi") || lower.contains("wi-fi") -> "WIFI_PASSWORD"
            else -> {
                val words = fact.split(" ").filter { it.length > 3 }.take(2)
                if (words.isNotEmpty()) words.joinToString("_").uppercase() else "FACT_${System.currentTimeMillis() % 10000}"
            }
        }
    }
}
