package com.aistudio.jarvis.voiceagent.tools

import android.content.Context
import com.aistudio.jarvis.voiceagent.data.service.JarvisNotificationListenerService

class NotificationTool : JarvisTool {
    override val id: String = "notifications"
    override val name: String = "Notification Reader"
    override val description: String = "Summarizes recent notifications and unread messages from installed apps."
    override val category: String = "Communication & System"
    override val riskLevel: RiskLevel = RiskLevel.LOW
    override val requiredPermissions: List<String> = emptyList()
    override val examplePhrases: List<String> = listOf(
        "Read my recent notifications",
        "Jarvis, anything important?",
        "Read my latest WhatsApp notification",
        "Summarize my notifications"
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolExecutionResult {
        val isGranted = JarvisNotificationListenerService.isNotificationAccessGranted(context)
        if (!isGranted) {
            return ToolExecutionResult(
                isSuccess = false,
                spokenMessage = "I need notification access permission to read your notifications. Please enable it in Settings.",
                displayMessage = "Notification Access Required. Tap to open Settings."
            )
        }

        val notifications = JarvisNotificationListenerService.recentNotifications.value
        val appFilter = (params["app"] as? String ?: params["filter"] as? String ?: "").lowercase().trim()

        val filtered = if (appFilter.isNotBlank()) {
            notifications.filter {
                it.appName.lowercase().contains(appFilter) || it.packageName.lowercase().contains(appFilter)
            }
        } else {
            notifications
        }

        if (filtered.isEmpty()) {
            val msg = if (appFilter.isNotBlank()) {
                "You have no recent notifications from $appFilter."
            } else {
                "You have no unread notifications right now."
            }
            return ToolExecutionResult(
                isSuccess = true,
                spokenMessage = msg,
                displayMessage = msg,
                payload = emptyList<Any>()
            )
        }

        // Group by app
        val grouped = filtered.groupBy { it.appName }
        val summaryParts = grouped.entries.take(3).map { (app, items) ->
            "$app: ${items.size} notification${if (items.size > 1) "s" else ""}"
        }

        val spoken = "You have ${filtered.size} recent notification${if (filtered.size > 1) "s" else ""}, including ${summaryParts.joinToString(", ")}. Latest from ${filtered.first().appName}: ${filtered.first().title}."
        val display = filtered.take(5).joinToString("\n") { "• [${it.appName}] ${it.title}: ${it.text}" }

        return ToolExecutionResult(
            isSuccess = true,
            spokenMessage = spoken,
            displayMessage = "Recent Notifications (${filtered.size}):\n$display",
            payload = filtered
        )
    }
}
