package com.aistudio.jarvis.voiceagent.tools

import android.content.Context
import com.aistudio.jarvis.voiceagent.data.db.AppDatabase
import com.aistudio.jarvis.voiceagent.data.db.ReminderEntity
import java.util.Calendar

class ReminderTool : JarvisTool {
    override val id: String = "reminder"
    override val name: String = "Create Reminder"
    override val description: String = "Schedules a reminder for tasks, deadlines, and events."
    override val category: String = "Productivity"
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM
    override val requiredPermissions: List<String> = listOf(
        "android.permission.POST_NOTIFICATIONS"
    )
    override val examplePhrases: List<String> = listOf(
        "Remind me to submit my assignment tomorrow",
        "Remind me at 8 PM to call Mom",
        "Remind me at 6 PM to submit my project"
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolExecutionResult {
        val title = (params["title"] as? String ?: params["task"] as? String ?: "Reminder").trim()
        val timeDescription = (params["timeString"] as? String ?: params["time"] as? String ?: "Later today").trim()

        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, 2) // Default to 2 hours ahead if unspecified

        val reminder = ReminderEntity(
            title = title,
            targetTimeMillis = cal.timeInMillis,
            targetTimeString = timeDescription,
            isCompleted = false
        )

        try {
            val db = AppDatabase.getDatabase(context)
            db.jarvisDao().insertReminder(reminder)

            return ToolExecutionResult(
                isSuccess = true,
                spokenMessage = "Reminder scheduled for $timeDescription.",
                displayMessage = "Reminder scheduled: \"$title\" ($timeDescription)",
                payload = reminder
            )
        } catch (e: Exception) {
            return ToolExecutionResult(
                isSuccess = false,
                spokenMessage = "Unable to save reminder.",
                displayMessage = "Reminder error: ${e.localizedMessage}"
            )
        }
    }
}
