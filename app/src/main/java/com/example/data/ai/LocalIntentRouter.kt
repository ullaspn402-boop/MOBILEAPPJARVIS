package com.aistudio.jarvis.voiceagent.data.ai

import com.aistudio.jarvis.voiceagent.model.ExecutionPlan
import com.aistudio.jarvis.voiceagent.model.PlanStep
import com.aistudio.jarvis.voiceagent.tools.RiskLevel
import java.util.UUID

/**
 * High-Speed Local Command & Intent Router.
 *
 * CRITICAL PRODUCTION OPTIMIZATION:
 * Deterministic Android commands (Open YouTube, Open Maps, Set Alarm, Start Timer, Open Settings, Call, Message, Notes)
 * are resolved instantly on-device in < 1ms without hitting Gemini or the cloud backend.
 * Only complex reasoning or unstructured natural language prompts are routed to the cloud AI.
 */
object LocalIntentRouter {

    fun tryRouteLocalIntent(
        query: String,
        userPreferredName: String = "Sir",
        contextMemory: Map<String, String> = emptyMap()
    ): ExecutionPlan? {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return null

        // 1. Wake phrases & Assistant conversational greetings
        val greetingPlan = checkGreetings(trimmed, userPreferredName)
        if (greetingPlan != null) return greetingPlan

        // 2. Deterministic Heuristic parsing for all core Android actions
        return parseHeuristicPlan(trimmed, contextMemory)
    }

    private fun checkGreetings(query: String, name: String): ExecutionPlan? {
        val lower = query.lowercase().replace("?", "").replace("!", "").trim()

        val wakePhrases = setOf(
            "hey jarvis", "hello jarvis", "hi jarvis", "jarvis", "ok jarvis",
            "hey ullas", "hello ullas", "hi ullas", "ullas", "ok ullas",
            "hello", "hi", "hey"
        )

        if (wakePhrases.contains(lower)) {
            return ExecutionPlan(
                originalQuery = query,
                steps = emptyList(),
                finalSpokenSummary = "Yes, $name. How can I help you?"
            )
        }

        if (lower.contains("who are you") || lower.contains("what are you")) {
            return ExecutionPlan(
                originalQuery = query,
                steps = emptyList(),
                finalSpokenSummary = "I am ULLAS JARVIS, your production-scale personal AI voice agent for Android."
            )
        }

        if (lower.contains("who is your owner") || lower.contains("who made you") ||
            lower.contains("who created you") || lower.contains("who built you") ||
            lower.contains("who is your creator") || lower.contains("who is your master") ||
            lower.contains("your owner") || lower.contains("who owns you")) {
            return ExecutionPlan(
                originalQuery = query,
                steps = emptyList(),
                finalSpokenSummary = "My owner is Ullas. He created and built me."
            )
        }

        if (lower.contains("how are you") || lower.contains("status report") || lower.contains("system status")) {
            return ExecutionPlan(
                originalQuery = query,
                steps = emptyList(),
                finalSpokenSummary = "All systems operating at peak performance, $name. Core neural links are active."
            )
        }

        return null
    }

    private fun parseHeuristicPlan(
        query: String,
        memory: Map<String, String>
    ): ExecutionPlan? {
        val q = query.lowercase().trim()
        val steps = mutableListOf<PlanStep>()

        val youtubePlay = parseYouTubePlayIntent(q)
        if (youtubePlay != null) {
            return ExecutionPlan(
                originalQuery = query,
                steps = listOf(youtubePlay),
                finalSpokenSummary = null
            )
        }

        // 1. Compound multi-step commands: "Leaving for college"
        if (q.contains("leaving for college") || (q.contains("weather") && q.contains("maps") && q.contains("remind"))) {
            val collegeDest = memory["COLLEGE"] ?: "BMS Institute of Technology"
            steps.add(
                PlanStep(
                    id = "step_weather",
                    toolId = "web_search",
                    actionName = "Check Weather Forecast",
                    description = "Analyze local forecast and rain advisory",
                    params = mapOf("query" to "weather forecast umbrella"),
                    riskLevel = RiskLevel.LOW
                )
            )
            steps.add(
                PlanStep(
                    id = "step_maps",
                    toolId = "maps_navigation",
                    actionName = "Open Maps to $collegeDest",
                    description = "Start GPS navigation to $collegeDest",
                    params = mapOf("destination" to collegeDest),
                    riskLevel = RiskLevel.LOW
                )
            )
            steps.add(
                PlanStep(
                    id = "step_reminder",
                    toolId = "reminder",
                    actionName = "Schedule Assignment Reminder",
                    description = "Remind to submit assignment at 6 PM",
                    params = mapOf("title" to "Submit assignment", "timeString" to "6:00 PM"),
                    riskLevel = RiskLevel.MEDIUM
                )
            )
            return ExecutionPlan(
                originalQuery = query,
                steps = steps,
                finalSpokenSummary = "Local router executed: Weather forecast verified, navigation to $collegeDest started, and 6 PM reminder scheduled.",
                isMultiStep = true
            )
        }

        // 2. Chained "and then" commands: "Open YouTube and then open Maps"
        if (q.contains(" and then ") || q.contains(" and open ") || q.contains(" then open ")) {
            val segments = q.split(Regex(" and then | then | and open | and "))
            for (seg in segments) {
                val step = parseSingleIntent(seg.trim(), memory)
                if (step != null) steps.add(step)
            }
            if (steps.isNotEmpty()) {
                return ExecutionPlan(
                    originalQuery = query,
                    steps = steps,
                    isMultiStep = steps.size > 1,
                    finalSpokenSummary = "Executed multi-step Android operations."
                )
            }
        }

        // 3. Single Intent parsing
        val singleStep = parseSingleIntent(q, memory)
        if (singleStep != null) {
            val requiresConfirm = singleStep.riskLevel == RiskLevel.HIGH
            return ExecutionPlan(
                originalQuery = query,
                steps = listOf(singleStep),
                requiresConfirmation = requiresConfirm,
                confirmationStep = if (requiresConfirm) singleStep else null,
                finalSpokenSummary = null
            )
        }

        return null
    }

    private fun parseSingleIntent(q: String, memory: Map<String, String>): PlanStep? {
        val query = q.replace(Regex("^(hey|hi|hello|ok)?\\s*(jarvis|ullas)[,\\s]*"), "").trim()

        // 1. Phone Call Intent: "Call Mom", "Call Rahul", "Call 9876543210"
        if (query.startsWith("call ") || query.startsWith("phone ") || query.startsWith("dial ")) {
            val contact = query.replaceFirst(Regex("^(call|phone|dial)\\s+"), "").replace("?", "").trim()
            return PlanStep(
                id = "step_${UUID.randomUUID().toString().take(8)}",
                toolId = "call_contact",
                actionName = "Call $contact",
                description = "Initiate phone call to $contact",
                params = mapOf("contactName" to contact, "directCall" to true),
                riskLevel = RiskLevel.MEDIUM
            )
        }

        // 2. Messaging Intent (WhatsApp + SMS)
        // Patterns: "Message Rahul I'll be late", "Tell Mom I'm on my way",
        //           "WhatsApp Rahul saying I'll reach at 6", "Send WhatsApp to Dad Hi"
        val isWhatsAppPhrase = query.startsWith("whatsapp ") || query.startsWith("wa ") ||
            query.contains("send whatsapp") || query.contains("whatsapp message")
        val isMessagePhrase = query.startsWith("message ") || query.startsWith("tell ") ||
            query.startsWith("send message to ") || (query.startsWith("send ") && query.contains("message"))

        if (isWhatsAppPhrase || isMessagePhrase) {
            var recipient = "Contact"
            var message = ""

            // Normalize: strip leading keyword
            val normalized = query
                .replaceFirst(Regex("^(whatsapp|wa|message|tell|send whatsapp to|send message to|send whatsapp|send)\\s+"), "")
                .trim()

            when {
                // "Rahul saying I'll be late"
                normalized.contains(" saying ") -> {
                    val parts = normalized.split(" saying ")
                    recipient = parts[0].trim()
                    message = parts.getOrNull(1)?.trim() ?: ""
                }
                // "Rahul that I'll be late"
                normalized.contains(" that ") -> {
                    val parts = normalized.split(" that ")
                    recipient = parts[0].trim()
                    message = parts.getOrNull(1)?.trim() ?: ""
                }
                // "tell Mom I am on my way" → after stripping "tell " → "Mom I am on my way"
                else -> {
                    val firstSpace = normalized.indexOf(' ')
                    if (firstSpace > 0) {
                        recipient = normalized.substring(0, firstSpace).trim()
                        message = normalized.substring(firstSpace + 1).trim()
                    } else {
                        recipient = normalized
                        message = "Hello"
                    }
                }
            }

            return PlanStep(
                id = "step_${UUID.randomUUID().toString().take(8)}",
                toolId = "messaging",
                actionName = "WhatsApp $recipient",
                description = "Send \"$message\" to $recipient via WhatsApp",
                params = mapOf(
                    "recipient" to recipient,
                    "message" to message,
                    "isWhatsApp" to true
                ),
                riskLevel = RiskLevel.MEDIUM
            )
        }

        // 3. Alarms & Timers: "Set an alarm for 6 AM", "Alarm at 7:30 AM", "Start a timer for 5 minutes"
        if (query.contains("alarm") || query.contains("wake me up")) {
            val (hour, minute, isAm) = extractTime(query)
            return PlanStep(
                id = "step_${UUID.randomUUID().toString().take(8)}",
                toolId = "set_alarm",
                actionName = "Set Alarm",
                description = "Alarm set for ${if (hour != null) "$hour:${if (minute < 10) "0$minute" else minute}" else "specified time"}",
                params = mapOf("hour" to hour, "minute" to minute, "isAm" to isAm, "time" to query),
                riskLevel = RiskLevel.MEDIUM
            )
        }

        if (query.contains("timer")) {
            val minutes = Regex("(\\d+)\\s*(min|minute|minutes)").find(query)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 5
            return PlanStep(
                id = "step_${UUID.randomUUID().toString().take(8)}",
                toolId = "set_alarm",
                actionName = "Start $minutes Min Timer",
                description = "Set countdown timer for $minutes minutes",
                params = mapOf("hour" to null, "minute" to minutes, "isTimer" to true, "time" to "$minutes minutes"),
                riskLevel = RiskLevel.LOW
            )
        }

        // 4. Reminder Intent: "Remind me to submit my assignment tomorrow", "Remind me at 8 PM to call Mom"
        if (query.startsWith("remind me ") || query.contains("remind me")) {
            val afterRemind = query.substringAfter("remind me").trim()
            val task = afterRemind.replace(Regex("^at\\s+\\d+(:\\d+)?\\s*(am|pm)?\\s+to\\s+"), "").replace(Regex("^to\\s+"), "")
            return PlanStep(
                id = "step_${UUID.randomUUID().toString().take(8)}",
                toolId = "reminder",
                actionName = "Schedule Reminder",
                description = "Task: $task",
                params = mapOf("title" to task, "timeString" to "Scheduled time"),
                riskLevel = RiskLevel.MEDIUM
            )
        }

        // 5. Calendar Intent: "Create a meeting tomorrow at 3 PM", "Add project review on Friday"
        if (query.contains("meeting") || query.contains("calendar") || query.contains("schedule today") || query.contains("what do i have tomorrow") || query.contains("what's on my schedule")) {
            if (query.contains("what") || query.contains("show")) {
                return PlanStep(
                    id = "step_${UUID.randomUUID().toString().take(8)}",
                    toolId = "calendar",
                    actionName = "View Calendar",
                    description = "Check calendar schedule",
                    params = mapOf("action" to "view"),
                    riskLevel = RiskLevel.LOW
                )
            }
            val title = query.replace(Regex("^(create|add|schedule)\\s+(a\\s+)?(meeting|event|calendar event)?\\s*"), "").trim()
            return PlanStep(
                id = "step_${UUID.randomUUID().toString().take(8)}",
                toolId = "calendar",
                actionName = "Create Event: $title",
                description = "Add event to calendar",
                params = mapOf("title" to title),
                riskLevel = RiskLevel.MEDIUM
            )
        }

        // 6. Maps & Navigation: "Open Maps", "Take me to my college", "Search for nearest coffee shop", "Find restaurants near me"
        if (query.contains("maps") || query.startsWith("take me to ") || query.startsWith("navigate to ") || query.contains("nearest ") || query.contains("near me") || query.contains("find directions")) {
            val destination = when {
                query.startsWith("take me to ") -> query.substringAfter("take me to ")
                query.startsWith("navigate to ") -> query.substringAfter("navigate to ")
                query.contains("maps to ") -> query.substringAfter("maps to ")
                query.contains("nearest") -> query
                query.contains("near me") -> query
                else -> ""
            }.replace("my college", memory["COLLEGE"] ?: "college").trim()

            return PlanStep(
                id = "step_${UUID.randomUUID().toString().take(8)}",
                toolId = "maps_navigation",
                actionName = if (destination.isNotBlank()) "Navigate to $destination" else "Open Maps",
                description = "GPS navigation & location search",
                params = mapOf("destination" to destination),
                riskLevel = RiskLevel.LOW
            )
        }

        // 7. YouTube play/search MUST run before "open YouTube", otherwise
        // "Open YouTube play Believer" only launches the app homepage.
        val youtubeMusicStep = parseYouTubePlayIntent(query)
        if (youtubeMusicStep != null) return youtubeMusicStep

        // 8. App Launcher: "Open YouTube", "Open Chrome", "Open WhatsApp", "Open Spotify", "Open Settings", "Open Camera"
        if (query.startsWith("open ") || query.startsWith("launch ") || query.startsWith("start ")) {
            val appName = query.replaceFirst(Regex("^(open|launch|start)\\s+"), "").replace("app", "").trim()
            if (appName.contains("settings") || appName.contains("bluetooth") || appName.contains("wifi") || appName.contains("wi-fi")) {
                return PlanStep(
                    id = "step_${UUID.randomUUID().toString().take(8)}",
                    toolId = "device_settings",
                    actionName = "Open Settings",
                    description = "Navigate to $appName settings",
                    params = mapOf("setting" to appName),
                    riskLevel = RiskLevel.LOW
                )
            }
            return PlanStep(
                id = "step_${UUID.randomUUID().toString().take(8)}",
                toolId = "app_launcher",
                actionName = "Open $appName",
                description = "Launch application: $appName",
                params = mapOf("appName" to appName),
                riskLevel = RiskLevel.LOW
            )
        }

        // 9. Device Settings Intent: "Turn on Bluetooth", "Open Bluetooth settings", "Wi-Fi"
        if (query.contains("bluetooth") || query.contains("wi-fi") || query.contains("wifi") || query.contains("battery settings") || query.contains("settings")) {
            return PlanStep(
                id = "step_${UUID.randomUUID().toString().take(8)}",
                toolId = "device_settings",
                actionName = "Device Settings",
                description = "Open device settings for $query",
                params = mapOf("setting" to query),
                riskLevel = RiskLevel.LOW
            )
        }

        // 10. Notifications: "Read my recent notifications", "Read WhatsApp notifications"
        if (query.contains("notification") || query.contains("anything important") || query.contains("unread messages")) {
            return PlanStep(
                id = "step_${UUID.randomUUID().toString().take(8)}",
                toolId = "notifications",
                actionName = "Read Notifications",
                description = "Summarize recent device notifications",
                params = mapOf("query" to query),
                riskLevel = RiskLevel.LOW
            )
        }

        // 11. Notes: "Take a note: buy a new charger", "Note down..."
        if (query.startsWith("take a note") || query.startsWith("note down") || query.startsWith("write a note") || query.startsWith("create note")) {
            val content = query.replaceFirst(Regex("^(take a note|note down|write a note|create note)[:\\s]+(that\\s+)?"), "").trim()
            return PlanStep(
                id = "step_${UUID.randomUUID().toString().take(8)}",
                toolId = "notes",
                actionName = "Save Note",
                description = "Note: \"$content\"",
                params = mapOf("content" to content, "action" to "create"),
                riskLevel = RiskLevel.LOW
            )
        }

        // 12. Memory: "Remember that my presentation is Friday", "What is my college?"
        if (query.startsWith("remember that ") || query.startsWith("remember ") || query.contains("what did i ask you to remember") || query.contains("what is my ")) {
            if (query.contains("what")) {
                val searchKey = query.replace("what did i ask you to remember", "").replace("what is my", "").replace("?", "").trim()
                return PlanStep(
                    id = "step_${UUID.randomUUID().toString().take(8)}",
                    toolId = "memory",
                    actionName = "Recall Memory",
                    description = "Query memory vault",
                    params = mapOf("action" to "recall", "key" to searchKey),
                    riskLevel = RiskLevel.LOW
                )
            } else {
                val fact = query.replaceFirst(Regex("^remember\\s+(that\\s+)?"), "").trim()
                return PlanStep(
                    id = "step_${UUID.randomUUID().toString().take(8)}",
                    toolId = "memory",
                    actionName = "Save to Memory",
                    description = "Store fact: \"$fact\"",
                    params = mapOf("action" to "save", "fact" to fact),
                    riskLevel = RiskLevel.LOW
                )
            }
        }

        // 13. Music / Play Song: "Play Believer", "Play Arijit Singh on YouTube"
        if (query.startsWith("play ") || query.contains("play song") || query.contains("play music") || query.contains("play some music")) {
            val platform = detectMusicPlatform(query)
            val songName = query
                .replaceFirst(Regex("^play\\s+"), "")
                .replace(Regex("\\bon\\s+(youtube\\s+music|youtube|spotify)\\b"), "")
                .replace("some music", "")
                .replace("a song", "")
                .replace(Regex("\\bmusic\\b"), "")
                .trim()
            return musicPlanStep(songName, platform)
        }

        // 14. System Controls: power off, restart, lock screen, flashlight, mute, volume, DND
        val systemKeywords = listOf(
            "power off", "shutdown", "power down", "turn off phone",
            "restart", "reboot", "reset phone",
            "lock screen", "lock phone",
            "flashlight", "torch",
            "mute", "unmute", "silent mode", "vibrate",
            "volume up", "volume down", "increase volume", "decrease volume",
            "lower volume", "raise volume", "louder", "quieter", "max volume", "full volume",
            "do not disturb", "dnd",
            "airplane mode", "flight mode",
            "brightness"
        )
        if (systemKeywords.any { query.contains(it) }) {
            val isHighRisk = query.contains("power off") || query.contains("shutdown") ||
                query.contains("restart") || query.contains("reboot")
            return PlanStep(
                id = "step_${UUID.randomUUID().toString().take(8)}",
                toolId = "system_control",
                actionName = "System Control",
                description = "Executing: $query",
                params = mapOf("action" to query, "target" to query),
                riskLevel = if (isHighRisk) RiskLevel.HIGH else RiskLevel.LOW
            )
        }

        return null
    }

    private fun parseYouTubePlayIntent(query: String): PlanStep? {
        val openThenPlay = Regex(
            """(?:open|launch|start)?\s*youtube(?:\s+music)?\s+(?:and\s+)?(?:play|search(?:\s+for)?|find)\s+(.+)""",
            RegexOption.IGNORE_CASE
        ).find(query)
        if (openThenPlay != null) {
            val song = cleanSongName(openThenPlay.groupValues[1])
            if (song.isNotBlank()) return musicPlanStep(song, "youtube")
        }

        val playOnYoutube = Regex(
            """(?:play|search(?:\s+for)?|find)\s+(.+?)\s+on\s+youtube(?:\s+music)?""",
            RegexOption.IGNORE_CASE
        ).find(query)
        if (playOnYoutube != null) {
            val song = cleanSongName(playOnYoutube.groupValues[1])
            if (song.isNotBlank()) return musicPlanStep(song, "youtube")
        }

        val youtubeOnlySearch = Regex(
            """^(?:open|launch|start)?\s*youtube\s+(.+)$""",
            RegexOption.IGNORE_CASE
        ).find(query)
        if (youtubeOnlySearch != null) {
            val extra = cleanSongName(youtubeOnlySearch.groupValues[1])
            if (extra.isNotBlank() && extra !in setOf("app", "application")) {
                return musicPlanStep(extra, "youtube")
            }
        }

        return null
    }

    private fun detectMusicPlatform(query: String): String {
        val lower = query.lowercase()
        return when {
            lower.contains("youtube music") -> "ytmusic"
            lower.contains("youtube") -> "youtube"
            lower.contains("spotify") -> "spotify"
            else -> "auto"
        }
    }

    private fun cleanSongName(raw: String): String {
        return raw
            .replace(Regex("(?i)\\bon\\s+(youtube\\s+music|youtube|spotify)\\b"), "")
            .replace(Regex("(?i)\\b(the\\s+song|a\\s+song|song|video|music)\\b"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('"', '\'')
    }

    private fun musicPlanStep(songName: String, platform: String): PlanStep {
        return PlanStep(
            id = "step_${UUID.randomUUID().toString().take(8)}",
            toolId = "music",
            actionName = if (songName.isBlank()) "Play Music" else "Play $songName",
            description = if (songName.isBlank()) "Open music player" else "Search and play \"$songName\"",
            params = mapOf(
                "songName" to songName,
                "query" to songName,
                "platform" to platform
            ),
            riskLevel = RiskLevel.LOW
        )
    }

    private fun extractTime(str: String): Triple<Int?, Int, Boolean?> {
        val regex = Regex("(\\d{1,2})(:(\\d{2}))?\\s*(am|pm|baje)?", RegexOption.IGNORE_CASE)
        val match = regex.find(str) ?: return Triple(null, 0, null)
        val hour = match.groupValues[1].toIntOrNull()
        val minute = match.groupValues[3].toIntOrNull() ?: 0
        val meridiem = match.groupValues[4].lowercase()
        val isAm = when {
            meridiem == "am" -> true
            meridiem == "pm" -> false
            else -> null
        }
        return Triple(hour, minute, isAm)
    }
}
