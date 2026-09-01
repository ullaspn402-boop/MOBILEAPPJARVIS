package com.aistudio.jarvis.voiceagent.tools

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import java.util.Calendar

class AlarmTool : JarvisTool {
    override val id: String = "set_alarm"
    override val name: String = "Set Alarm"
    override val description: String = "Sets a clock alarm on your Android device."
    override val category: String = "Utilities & Clocks"
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM
    override val requiredPermissions: List<String> = listOf(
        "com.android.alarm.permission.SET_ALARM"
    )
    override val examplePhrases: List<String> = listOf(
        "Set an alarm for 6 AM",
        "Set an alarm for 7:30 AM",
        "Wake me up at 5:00 AM",
        "Alarm at 6 PM"
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolExecutionResult {
        var hour = (params["hour"] as? Number)?.toInt()
        val minute = (params["minute"] as? Number)?.toInt() ?: 0
        val isAm = params["isAm"] as? Boolean
        val label = (params["label"] as? String ?: "JARVIS Alarm").trim()

        if (hour == null) {
            // Parse from raw time string if provided
            val timeStr = (params["time"] as? String ?: "").lowercase()
            val parsed = parseTime(timeStr)
            if (parsed != null) {
                hour = parsed.first
            } else {
                return ToolExecutionResult(
                    isSuccess = false,
                    spokenMessage = "What time should I set the alarm for?",
                    displayMessage = "Please specify alarm time."
                )
            }
        }

        // Normalize hour based on AM/PM
        var finalHour = hour
        if (isAm != null) {
            if (!isAm && finalHour < 12) finalHour += 12
            if (isAm && finalHour == 12) finalHour = 0
        }

        val displayTime = formatTime(finalHour, minute)

        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, finalHour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            if (alarmIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(alarmIntent)
                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "Alarm set for $displayTime.",
                    displayMessage = "Alarm set for $displayTime ($label)",
                    actionIntent = alarmIntent,
                    payload = mapOf("time" to displayTime, "hour" to finalHour, "minute" to minute)
                )
            } else {
                ToolExecutionResult(
                    isSuccess = false,
                    spokenMessage = "No clock application found to set alarm.",
                    displayMessage = "Clock app not available on this device."
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                spokenMessage = "Failed to set alarm.",
                displayMessage = "Alarm error: ${e.localizedMessage}"
            )
        }
    }

    private fun parseTime(str: String): Pair<Int, Int>? {
        val regex = Regex("(\\d{1,2})(:(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE)
        val match = regex.find(str) ?: return null
        var hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[3].toIntOrNull() ?: 0
        val meridiem = match.groupValues[4].lowercase()

        if (meridiem == "pm" && hour < 12) hour += 12
        if (meridiem == "am" && hour == 12) hour = 0
        return Pair(hour, minute)
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val isPm = cal.get(Calendar.AM_PM) == Calendar.PM
        val h12 = if (cal.get(Calendar.HOUR) == 0) 12 else cal.get(Calendar.HOUR)
        return if (minute == 0) {
            "$h12 ${if (isPm) "PM" else "AM"}"
        } else {
            String.format("%d:%02d %s", h12, minute, if (isPm) "PM" else "AM")
        }
    }
}
