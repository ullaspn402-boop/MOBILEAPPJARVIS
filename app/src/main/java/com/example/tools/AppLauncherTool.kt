package com.aistudio.jarvis.voiceagent.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings

class AppLauncherTool : JarvisTool {
    override val id: String = "app_launcher"
    override val name: String = "Open Application"
    override val description: String = "Launches installed applications and native device utilities."
    override val category: String = "System & Apps"
    override val riskLevel: RiskLevel = RiskLevel.LOW
    override val requiredPermissions: List<String> = emptyList()
    override val examplePhrases: List<String> = listOf(
        "Open YouTube",
        "Open Chrome",
        "Open WhatsApp",
        "Open Maps",
        "Open Camera",
        "Open Settings",
        "Open Spotify"
    )

    private val commonPackageMap = mapOf(
        "youtube" to listOf("com.google.android.youtube", "https://youtube.com"),
        "chrome" to listOf("com.android.chrome", "https://google.com"),
        "browser" to listOf("com.android.chrome", "https://google.com"),
        "whatsapp" to listOf("com.whatsapp"),
        "maps" to listOf("com.google.android.apps.maps", "geo:0,0"),
        "google maps" to listOf("com.google.android.apps.maps", "geo:0,0"),
        "spotify" to listOf("com.spotify.music"),
        "camera" to listOf("android.media.action.STILL_IMAGE_CAMERA"),
        "settings" to listOf("android.settings.SETTINGS"),
        "calculator" to listOf("com.google.android.calculator", "com.android.calculator2"),
        "clock" to listOf("com.google.android.deskclock", "com.android.deskclock"),
        "gmail" to listOf("com.google.android.gm", "mailto:"),
        "mail" to listOf("com.google.android.gm", "mailto:"),
        "photos" to listOf("com.google.android.apps.photos"),
        "gallery" to listOf("com.google.android.apps.photos", "com.android.gallery3d"),
        "phone" to listOf("android.intent.action.DIAL"),
        "contacts" to listOf("com.google.android.contacts", "com.android.contacts")
    )

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolExecutionResult {
        val appQuery = (params["appName"] as? String ?: params["target"] as? String ?: "").lowercase().trim()
        if (appQuery.isBlank()) {
            return ToolExecutionResult(
                isSuccess = false,
                spokenMessage = "Which application would you like me to open?",
                displayMessage = "Please specify an application name."
            )
        }

        val pm = context.packageManager

        val youtubeSearch = extractYouTubeSearchQuery(appQuery)
        if (youtubeSearch != null) {
            val videoId = fetchTopYouTubeVideoId(youtubeSearch)
            val watchIntent = if (videoId != null) {
                val nativeUri = Uri.parse("vnd.youtube:$videoId")
                val nativeIntent = Intent(Intent.ACTION_VIEW, nativeUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                if (nativeIntent.resolveActivity(context.packageManager) != null) {
                    nativeIntent
                } else {
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId")).apply {
                        setPackage("com.google.android.youtube")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                }
            } else {
                Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                    putExtra(android.app.SearchManager.QUERY, youtubeSearch)
                    putExtra("query", youtubeSearch)
                    putExtra(MediaStore.EXTRA_MEDIA_TITLE, youtubeSearch)
                    putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                    putExtra("android.intent.extra.focus", "vnd.android.cursor.item/*")
                    putExtra("autostart", true)
                    setPackage("com.google.android.youtube")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
            return try {
                context.startActivity(watchIntent)
                ToolExecutionResult(
                    isSuccess = true,
                    spokenMessage = "Playing $youtubeSearch on YouTube.",
                    displayMessage = "Playing \"$youtubeSearch\" on YouTube",
                    actionIntent = watchIntent
                )
            } catch (_: Exception) {
                val encoded = Uri.encode(youtubeSearch)
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/results?search_query=$encoded")
                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                tryLaunchIntent(context, browserIntent, "YouTube search")
            }
        }

        // 1. Check special actions like camera / settings / phone
        if (appQuery.contains("camera")) {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return tryLaunchIntent(context, intent, "Camera")
        }

        if (appQuery.contains("setting")) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return tryLaunchIntent(context, intent, "Settings")
        }

        // 2. Check well-known package map
        for ((key, targets) in commonPackageMap) {
            if (appQuery.contains(key)) {
                for (target in targets) {
                    if (target.startsWith("http://") || target.startsWith("https://") || target.startsWith("geo:") || target.startsWith("mailto:")) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        if (browserIntent.resolveActivity(pm) != null) {
                            context.startActivity(browserIntent)
                            return ToolExecutionResult(
                                isSuccess = true,
                                spokenMessage = "Opening ${key.replaceFirstChar { it.uppercase() }}.",
                                displayMessage = "Opened ${key.replaceFirstChar { it.uppercase() }}",
                                actionIntent = browserIntent
                            )
                        }
                    } else if (target.startsWith("android.")) {
                        val actionIntent = Intent(target).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        if (actionIntent.resolveActivity(pm) != null) {
                            context.startActivity(actionIntent)
                            return ToolExecutionResult(
                                isSuccess = true,
                                spokenMessage = "Opening ${key.replaceFirstChar { it.uppercase() }}.",
                                displayMessage = "Opened ${key.replaceFirstChar { it.uppercase() }}",
                                actionIntent = actionIntent
                            )
                        }
                    } else {
                        val launchIntent = pm.getLaunchIntentForPackage(target)
                        if (launchIntent != null) {
                            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(launchIntent)
                            return ToolExecutionResult(
                                isSuccess = true,
                                spokenMessage = "Opening ${key.replaceFirstChar { it.uppercase() }}.",
                                displayMessage = "Opened ${key.replaceFirstChar { it.uppercase() }}",
                                actionIntent = launchIntent
                            )
                        }
                    }
                }
            }
        }

        // 3. Search installed applications
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in installedApps) {
                val label = pm.getApplicationLabel(appInfo).toString().lowercase()
                if (label.contains(appQuery) || appQuery.contains(label)) {
                    val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(launchIntent)
                        val readableName = pm.getApplicationLabel(appInfo).toString()
                        return ToolExecutionResult(
                            isSuccess = true,
                            spokenMessage = "Opening $readableName.",
                            displayMessage = "Opened $readableName",
                            actionIntent = launchIntent
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore search error
        }

        return ToolExecutionResult(
            isSuccess = false,
            spokenMessage = "I couldn't open $appQuery because the application is not installed on your device.",
            displayMessage = "Application '$appQuery' not found on this device."
        )
    }

    private fun extractYouTubeSearchQuery(appQuery: String): String? {
        val match = Regex(
            """youtube(?:\s+music)?\s+(?:and\s+)?(?:play|search(?:\s+for)?|find)?\s*(.+)""",
            RegexOption.IGNORE_CASE
        ).find(appQuery) ?: return null
        val extra = match.groupValues[1]
            .replace(Regex("(?i)\\b(app|application)\\b"), "")
            .trim()
        return extra.takeIf { it.isNotBlank() }
    }

    private fun tryLaunchIntent(context: Context, intent: Intent, readableName: String): ToolExecutionResult {
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
                displayMessage = "Failed to launch $readableName: ${e.localizedMessage}"
            )
        }
    }

    private suspend fun fetchTopYouTubeVideoId(query: String): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val encoded = Uri.encode(query)
                val url = "https://www.youtube.com/results?search_query=$encoded"
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(4, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(4, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string().orEmpty()
                val match = Regex("""(?:"videoId"\s*:\s*"|/watch\?v=|"watchEndpoint":\{"videoId":")([a-zA-Z0-9_-]{11})""").find(body)
                match?.groupValues?.get(1)
            } catch (_: Throwable) {
                null
            }
        }
    }
}
