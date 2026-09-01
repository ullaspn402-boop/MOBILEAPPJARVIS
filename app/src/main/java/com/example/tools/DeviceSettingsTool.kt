package com.aistudio.jarvis.voiceagent.tools

import android.content.Context
import android.content.Intent
import android.provider.Settings

class DeviceSettingsTool : JarvisTool {
    override val id: String = "device_settings"
    override val name: String = "Device Settings"
    override val description: String = "Navigates directly to system settings like Bluetooth, Wi-Fi, Sound, and Display."
    override val category: String = "System & Hardware"
    override val riskLevel: RiskLevel = RiskLevel.LOW
    override val requiredPermissions: List<String> = emptyList()
    override val examplePhrases: List<String> = listOf(
        "Open Bluetooth settings",
        "Open Wi-Fi settings",
        "Open battery settings",
        "Open notification settings",
        "Open display settings"
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolExecutionResult {
        val target = (params["setting"] as? String ?: params["target"] as? String ?: "").lowercase().trim()

        val (action, readableName) = when {
            target.contains("bluetooth") || target.contains("bt") -> Pair(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth Settings")
            target.contains("wifi") || target.contains("wi-fi") || target.contains("network") -> Pair(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi Settings")
            target.contains("battery") || target.contains("power") -> Pair(Settings.ACTION_BATTERY_SAVER_SETTINGS, "Battery Settings")
            target.contains("display") || target.contains("brightness") || target.contains("screen") -> Pair(Settings.ACTION_DISPLAY_SETTINGS, "Display Settings")
            target.contains("sound") || target.contains("volume") || target.contains("audio") -> Pair(Settings.ACTION_SOUND_SETTINGS, "Sound Settings")
            target.contains("notification") -> Pair(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, "Notification Settings")
            target.contains("location") || target.contains("gps") -> Pair(Settings.ACTION_LOCATION_SOURCE_SETTINGS, "Location Settings")
            target.contains("assistant") || target.contains("voice") -> Pair(Settings.ACTION_VOICE_INPUT_SETTINGS, "Assistant & Voice Settings")
            target.contains("date") || target.contains("time") -> Pair(Settings.ACTION_DATE_SETTINGS, "Date & Time Settings")
            target.contains("app") || target.contains("application") -> Pair(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, "Application Settings")
            else -> Pair(Settings.ACTION_SETTINGS, "System Settings")
        }

        val intent = Intent(action).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            ToolExecutionResult(
                isSuccess = true,
                spokenMessage = "Opening $readableName.",
                displayMessage = "Opened $readableName",
                actionIntent = intent
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                spokenMessage = "Unable to open $readableName.",
                displayMessage = "Settings error: ${e.localizedMessage}"
            )
        }
    }
}
