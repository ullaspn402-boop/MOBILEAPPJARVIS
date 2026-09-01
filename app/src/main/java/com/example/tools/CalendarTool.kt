package com.aistudio.jarvis.voiceagent.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import java.util.Calendar

class CalendarTool : JarvisTool {
    override val id: String = "calendar"
    override val name: String = "Add Calendar Event"
    override val description: String = "Creates a calendar event or meeting in your device calendar."
    override val category: String = "Productivity"
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM
    override val requiredPermissions: List<String> = listOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )
    override val examplePhrases: List<String> = listOf(
        "Create a meeting tomorrow at 3 PM",
        "Add project review on Friday at 5 PM",
        "Create a calendar event tomorrow at 2 PM"
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolExecutionResult {
        val title = (params["title"] as? String ?: params["event"] as? String ?: "New Event").trim()
        val description = (params["description"] as? String ?: "Created by JARVIS").trim()
        val location = (params["location"] as? String ?: "").trim()

        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1) // default to tomorrow
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
        }

        val beginTime = (params["beginTimeMillis"] as? Number)?.toLong() ?: cal.timeInMillis
        val endTime = beginTime + (60 * 60 * 1000) // 1 hour duration

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            ToolExecutionResult(
                isSuccess = true,
                spokenMessage = "Calendar event created for $title.",
                displayMessage = "Created Calendar Event: $title",
                actionIntent = intent,
                payload = mapOf("title" to title, "beginTime" to beginTime)
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                spokenMessage = "Could not open calendar.",
                displayMessage = "Calendar launch error: ${e.localizedMessage}"
            )
        }
    }
}
