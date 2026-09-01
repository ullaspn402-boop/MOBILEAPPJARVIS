package com.aistudio.jarvis.voiceagent.tools

import android.content.Context
import android.content.Intent

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class ToolExecutionResult(
    val isSuccess: Boolean,
    val spokenMessage: String,
    val displayMessage: String,
    val actionIntent: Intent? = null,
    val payload: Any? = null
)

interface JarvisTool {
    val id: String
    val name: String
    val description: String
    val category: String
    val riskLevel: RiskLevel
    val requiredPermissions: List<String>
    val examplePhrases: List<String>

    suspend fun execute(context: Context, params: Map<String, Any?>): ToolExecutionResult
}
