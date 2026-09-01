package com.aistudio.jarvis.voiceagent.model

import com.aistudio.jarvis.voiceagent.tools.RiskLevel

enum class JarvisState {
    READY,
    LISTENING,
    THINKING,
    CONFIRMATION_REQUIRED,
    EXECUTING,
    COMPLETED,
    ERROR
}

data class PlanStep(
    val id: String,
    val toolId: String,
    val actionName: String,
    val description: String,
    val params: Map<String, Any?> = emptyMap(),
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val isCompleted: Boolean = false,
    val isExecuting: Boolean = false,
    val errorMessage: String? = null
)

data class ExecutionPlan(
    val id: String = "plan_${System.currentTimeMillis()}",
    val originalQuery: String,
    val steps: List<PlanStep>,
    val finalSpokenSummary: String? = null,
    val isMultiStep: Boolean = false,
    val requiresConfirmation: Boolean = false,
    val confirmationStep: PlanStep? = null
)

data class ConversationTurn(
    val id: String = "turn_${System.currentTimeMillis()}",
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String? = null,
    val plan: ExecutionPlan? = null
)
