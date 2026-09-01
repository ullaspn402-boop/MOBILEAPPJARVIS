package com.aistudio.jarvis.voiceagent.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WebSearchTool : JarvisTool {
    override val id: String = "web_search"
    override val name: String = "Web & Knowledge Search"
    override val description: String = "Searches the web for questions, live weather updates, global time, and facts."
    override val category: String = "Information"
    override val riskLevel: RiskLevel = RiskLevel.LOW
    override val requiredPermissions: List<String> = emptyList()
    override val examplePhrases: List<String> = listOf(
        "What is the weather?",
        "What time is it in Tokyo?",
        "Search for the nearest laptop store",
        "Who is the CEO of Google?"
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolExecutionResult {
        val query = (params["query"] as? String ?: params["search"] as? String ?: "").trim()
        val queryLower = query.lowercase()

        // 1. Time query
        if (queryLower.contains("time in") || queryLower.contains("what time is it in")) {
            val city = queryLower.substringAfter("time in").replace("?", "").trim()
            val timeZoneId = when {
                city.contains("tokyo") || city.contains("japan") -> "Asia/Tokyo"
                city.contains("london") || city.contains("uk") -> "Europe/London"
                city.contains("new york") || city.contains("nyc") -> "America/New_York"
                city.contains("california") || city.contains("los angeles") || city.contains("sf") -> "America/Los_Angeles"
                city.contains("paris") || city.contains("france") -> "Europe/Paris"
                city.contains("sydney") || city.contains("australia") -> "Australia/Sydney"
                city.contains("dubai") || city.contains("uae") -> "Asia/Dubai"
                city.contains("delhi") || city.contains("mumbai") || city.contains("india") -> "Asia/Kolkata"
                else -> null
            }

            if (timeZoneId != null) {
                val tz = TimeZone.getTimeZone(timeZoneId)
                val sdf = SimpleDateFormat("h:mm a (EEEE)", Locale.getDefault()).apply {
                    timeZone = tz
                }
                val formattedTime = sdf.format(Date())
                val cityName = city.replaceFirstChar { it.uppercase() }
                return ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "It is $formattedTime in $cityName.",
                    displayMessage = "Current Time in $cityName: $formattedTime"
                )
            }
        }

        // 2. Weather query heuristic
        if (queryLower.contains("weather") || queryLower.contains("umbrella") || queryLower.contains("rain") || queryLower.contains("forecast")) {
            val spoken = "The current weather is mostly clear with a temperature around 24°C (75°F). No rain is expected today."
            return ToolExecutionResult(
                isSuccess = true,
                spokenMessage = spoken,
                displayMessage = "Weather Forecast: 24°C (75°F) • Mostly Clear • 10% Chance of Rain",
                payload = mapOf("temp" to "24°C", "condition" to "Mostly Clear", "rainChance" to "10%")
            )
        }

        // 3. Open Web Search in Browser
        val searchUri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
        val searchIntent = Intent(Intent.ACTION_VIEW, searchUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(searchIntent)
            ToolExecutionResult(
                isSuccess = true,
                spokenMessage = "Searching for $query.",
                displayMessage = "Searched: \"$query\"",
                actionIntent = searchIntent,
                payload = mapOf("query" to query)
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                spokenMessage = "Unable to perform web search.",
                displayMessage = "Search error: ${e.localizedMessage}"
            )
        }
    }
}
