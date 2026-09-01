package com.aistudio.jarvis.voiceagent.tools

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings

class SystemControlTool : JarvisTool {
    override val id: String = "system_control"
    override val name: String = "System Controls"
    override val description: String = "Controls device system functions: power off, restart, screen lock, volume, brightness, flashlight, silent mode, and Do Not Disturb."
    override val category: String = "System & Hardware"
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM
    override val requiredPermissions: List<String> = emptyList()
    override val examplePhrases: List<String> = listOf(
        "Power off",
        "Restart phone",
        "Lock screen",
        "Turn on flashlight",
        "Mute phone",
        "Increase volume",
        "Enable Do Not Disturb",
        "Turn off torch"
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolExecutionResult {
        val action = (params["action"] as? String ?: params["target"] as? String ?: "").lowercase().trim()

        return when {
            // ── Power Off ───────────────────────────────────────────────────
            action.contains("power off") || action.contains("shutdown") || action.contains("power down") || action.contains("turn off phone") -> {
                try {
                    // On most Android devices this shows the native power-off confirmation dialog
                    val intent = Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN").apply {
                        putExtra("android.intent.extra.KEY_CONFIRM", true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ToolExecutionResult(
                        isSuccess = true,
                        spokenMessage = "Showing power off dialog.",
                        displayMessage = "Power Off dialog opened"
                    )
                } catch (e: Exception) {
                    // Fallback: open battery settings
                    val intent = Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(intent)
                    ToolExecutionResult(
                        isSuccess = false,
                        spokenMessage = "Power off requires the physical power button on your device. Please hold the power button to shut down.",
                        displayMessage = "Hold the power button to shut down your device."
                    )
                }
            }

            // ── Restart / Reboot ─────────────────────────────────────────────
            action.contains("restart") || action.contains("reboot") || action.contains("reset phone") -> {
                try {
                    val intent = Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN").apply {
                        putExtra("android.intent.extra.KEY_CONFIRM", true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ToolExecutionResult(
                        isSuccess = true,
                        spokenMessage = "Showing power menu. Please select restart.",
                        displayMessage = "Power menu opened — tap Restart"
                    )
                } catch (e: Exception) {
                    ToolExecutionResult(
                        isSuccess = false,
                        spokenMessage = "Restart requires the physical power button. Please hold the power button and tap Restart.",
                        displayMessage = "Hold power button → tap Restart"
                    )
                }
            }

            // ── Screen Lock ──────────────────────────────────────────────────
            action.contains("lock screen") || action.contains("lock phone") || action.contains("lock") -> {
                try {
                    // Use DevicePolicyManager if device admin is set, else open security settings
                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                    val activeAdmin = dpm?.activeAdmins?.firstOrNull()
                    if (dpm != null && activeAdmin != null) {
                        dpm.lockNow()
                        ToolExecutionResult(
                            isSuccess = true,
                            spokenMessage = "Screen locked.",
                            displayMessage = "Screen locked"
                        )
                    } else {
                        // Fallback: send to security settings
                        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        context.startActivity(intent)
                        ToolExecutionResult(
                            isSuccess = true,
                            spokenMessage = "To lock your screen instantly, press your power button once.",
                            displayMessage = "Press power button to lock screen"
                        )
                    }
                } catch (e: Exception) {
                    ToolExecutionResult(
                        isSuccess = false,
                        spokenMessage = "Press the power button once to lock the screen.",
                        displayMessage = "Press power button to lock screen"
                    )
                }
            }

            // ── Flashlight / Torch ───────────────────────────────────────────
            action.contains("flashlight") || action.contains("torch") -> {
                val turnOn = !action.contains("off") && !action.contains("disable") && !action.contains("turn off")
                toggleFlashlight(context, turnOn)
            }

            // ── Volume: Mute ─────────────────────────────────────────────────
            action.contains("mute") || action.contains("silent") -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.ringerMode = AudioManager.RINGER_MODE_SILENT
                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "Phone muted.",
                    displayMessage = "Phone muted 🔇"
                )
            }

            // ── Volume: Vibrate ──────────────────────────────────────────────
            action.contains("vibrate") || action.contains("vibration") -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "Phone set to vibrate.",
                    displayMessage = "Vibrate mode ON 📳"
                )
            }

            // ── Volume: Unmute / Ring ────────────────────────────────────────
            action.contains("unmute") || action.contains("ringer on") || action.contains("ring") && action.contains("on") -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "Phone unmuted. Ringer is on.",
                    displayMessage = "Ringer ON 🔔"
                )
            }

            // ── Volume: Increase ─────────────────────────────────────────────
            action.contains("volume up") || action.contains("increase volume") || action.contains("louder") || action.contains("raise volume") -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "Volume increased.",
                    displayMessage = "Volume Up 🔊"
                )
            }

            // ── Volume: Decrease ─────────────────────────────────────────────
            action.contains("volume down") || action.contains("decrease volume") || action.contains("lower volume") || action.contains("quieter") -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "Volume decreased.",
                    displayMessage = "Volume Down 🔉"
                )
            }

            // ── Max Volume ───────────────────────────────────────────────────
            action.contains("max volume") || action.contains("full volume") -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, AudioManager.FLAG_SHOW_UI)
                audio.setStreamVolume(AudioManager.STREAM_RING, audio.getStreamMaxVolume(AudioManager.STREAM_RING), AudioManager.FLAG_SHOW_UI)
                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "Volume set to maximum.",
                    displayMessage = "Max Volume 🔊🔊"
                )
            }

            // ── Brightness ───────────────────────────────────────────────────
            action.contains("brightness") -> {
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "Opening display brightness settings.",
                    displayMessage = "Opened Display Settings 💡"
                )
            }

            // ── Do Not Disturb ───────────────────────────────────────────────
            action.contains("do not disturb") || action.contains("dnd") -> {
                val turnOn = !action.contains("off") && !action.contains("disable")
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(
                        if (turnOn) NotificationManager.INTERRUPTION_FILTER_NONE
                        else NotificationManager.INTERRUPTION_FILTER_ALL
                    )
                    ToolExecutionResult(
                        isSuccess = true,
                        spokenMessage = if (turnOn) "Do Not Disturb enabled." else "Do Not Disturb disabled.",
                        displayMessage = if (turnOn) "DND ON 🔕" else "DND OFF 🔔"
                    )
                } else {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(intent)
                    ToolExecutionResult(
                        isSuccess = false,
                        spokenMessage = "Please grant Do Not Disturb permission to Jarvis in settings.",
                        displayMessage = "Grant DND permission in Settings"
                    )
                }
            }

            // ── Airplane Mode ────────────────────────────────────────────────
            action.contains("airplane") || action.contains("flight mode") -> {
                val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "Opening airplane mode settings.",
                    displayMessage = "Opened Airplane Mode Settings ✈️"
                )
            }

            // ── Wi-Fi ────────────────────────────────────────────────────────
            action.contains("wifi") || action.contains("wi-fi") -> {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "Opening Wi-Fi settings.",
                    displayMessage = "Opened Wi-Fi Settings 📶"
                )
            }

            // ── Bluetooth ────────────────────────────────────────────────────
            action.contains("bluetooth") -> {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "Opening Bluetooth settings.",
                    displayMessage = "Opened Bluetooth Settings 🔵"
                )
            }

            else -> ToolExecutionResult(
                isSuccess = false,
                spokenMessage = "I'm not sure what system control you'd like. You can say: power off, restart, lock screen, mute, volume up, flashlight on, Do Not Disturb.",
                displayMessage = "Unknown system command: $action"
            )
        }
    }

    private fun toggleFlashlight(context: Context, enable: Boolean): ToolExecutionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return ToolExecutionResult(
                isSuccess = false,
                spokenMessage = "No flashlight found on this device.",
                displayMessage = "Flashlight not available"
            )
            cameraManager.setTorchMode(cameraId, enable)
            ToolExecutionResult(
                isSuccess = true,
                spokenMessage = if (enable) "Flashlight on." else "Flashlight off.",
                displayMessage = if (enable) "Flashlight ON 🔦" else "Flashlight OFF"
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                spokenMessage = "Could not toggle flashlight: ${e.localizedMessage}",
                displayMessage = "Flashlight error: ${e.localizedMessage}"
            )
        }
    }
}
