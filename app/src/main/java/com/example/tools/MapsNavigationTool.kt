package com.aistudio.jarvis.voiceagent.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri

class MapsNavigationTool : JarvisTool {
    override val id: String = "maps_navigation"
    override val name: String = "Maps & Navigation"
    override val description: String = "Finds nearby places, opens Google Maps, and starts GPS navigation."
    override val category: String = "Location & Maps"
    override val riskLevel: RiskLevel = RiskLevel.LOW
    override val requiredPermissions: List<String> = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    override val examplePhrases: List<String> = listOf(
        "Open Google Maps",
        "Take me to my college",
        "Search for the nearest coffee shop",
        "Find restaurants near me",
        "Find a pharmacy nearby"
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolExecutionResult {
        val destination = (params["destination"] as? String ?: params["query"] as? String ?: params["target"] as? String ?: "").trim()

        val uri = if (destination.isBlank() || destination.equals("maps", ignoreCase = true)) {
            Uri.parse("geo:0,0?q=")
        } else {
            Uri.parse("geo:0,0?q=${Uri.encode(destination)}")
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pm = context.packageManager
        val finalIntent = if (mapIntent.resolveActivity(pm) != null) {
            mapIntent
        } else {
            // Generic web or app maps intent
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(destination)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        return try {
            context.startActivity(finalIntent)
            val spoken = if (destination.isBlank()) "Opening Maps." else "Opening directions to $destination."
            ToolExecutionResult(
                isSuccess = true,
                spokenMessage = spoken,
                displayMessage = if (destination.isBlank()) "Opened Maps" else "Navigating to: $destination",
                actionIntent = finalIntent,
                payload = mapOf("destination" to destination)
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                spokenMessage = "Failed to launch Maps.",
                displayMessage = "Maps error: ${e.localizedMessage}"
            )
        }
    }
}
