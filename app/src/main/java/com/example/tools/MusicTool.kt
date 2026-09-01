package com.aistudio.jarvis.voiceagent.tools

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore

class MusicTool : JarvisTool {
    override val id: String = "music"
    override val name: String = "Play Music"
    override val description: String = "Searches and plays a song or artist on YouTube, YouTube Music, or Spotify."
    override val category: String = "Entertainment"
    override val riskLevel: RiskLevel = RiskLevel.LOW
    override val requiredPermissions: List<String> = emptyList()
    override val examplePhrases: List<String> = listOf(
        "Play Believer",
        "Open YouTube play Shape of You",
        "Play Kesariya on YouTube",
        "Play songs by Arijit Singh"
    )

    private val youtubePackage = "com.google.android.youtube"
    private val youtubeMusicPackage = "com.google.android.apps.youtube.music"
    private val spotifyPackage = "com.spotify.music"

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolExecutionResult {
        val songQuery = (params["songName"] as? String
            ?: params["query"] as? String
            ?: params["target"] as? String
            ?: "").trim()
        val platform = (params["platform"] as? String)?.lowercase()?.trim().orEmpty()

        return when {
            platform == "youtube" || platform == "yt" -> playOnYouTube(context, songQuery)
            platform == "ytmusic" || platform == "youtube music" -> {
                val ytMusic = tryYouTubeMusic(context, songQuery)
                if (ytMusic.isSuccess) ytMusic else playOnYouTube(context, songQuery)
            }
            platform == "spotify" -> {
                val spotify = trySpotify(context, songQuery)
                if (spotify.isSuccess) spotify else playOnYouTube(context, songQuery)
            }
            else -> {
                val spotify = trySpotify(context, songQuery)
                if (spotify.isSuccess) return spotify
                val ytMusic = tryYouTubeMusic(context, songQuery)
                if (ytMusic.isSuccess) return ytMusic
                playOnYouTube(context, songQuery)
            }
        }
    }

    private fun trySpotify(context: Context, query: String): ToolExecutionResult {
        if (query.isBlank() || !isInstalled(context, spotifyPackage)) {
            return failed()
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("spotify:search:${Uri.encode(query)}")
            setPackage(spotifyPackage)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return start(context, intent, "Playing $query on Spotify.", "Playing \"$query\" on Spotify")
    }

    private fun tryYouTubeMusic(context: Context, query: String): ToolExecutionResult {
        if (query.isBlank() || !isInstalled(context, youtubeMusicPackage)) {
            return failed()
        }
        val playIntent = mediaPlayIntent(query, youtubeMusicPackage)
        val playResult = start(
            context,
            playIntent,
            "Playing $query on YouTube Music.",
            "Playing \"$query\" on YouTube Music"
        )
        if (playResult.isSuccess) return playResult

        val searchIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://music.youtube.com/search?q=${Uri.encode(query)}")
            setPackage(youtubeMusicPackage)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return start(
            context,
            searchIntent,
            "Searching YouTube Music for $query.",
            "Searching YouTube Music for \"$query\""
        )
    }

    private suspend fun playOnYouTube(context: Context, query: String): ToolExecutionResult {
        if (query.isBlank()) {
            val launch = context.packageManager.getLaunchIntentForPackage(youtubePackage)
            return if (launch != null) {
                launch.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                start(context, launch, "Opening YouTube.", "Opened YouTube")
            } else {
                openWebYouTube(context, "")
            }
        }

        // Try direct top video playback
        val videoId = fetchTopYouTubeVideoId(query)
        if (videoId != null) {
            val nativeUri = Uri.parse("vnd.youtube:$videoId")
            val nativeIntent = Intent(Intent.ACTION_VIEW, nativeUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            if (nativeIntent.resolveActivity(context.packageManager) != null) {
                val nativeResult = start(
                    context,
                    nativeIntent,
                    "Playing $query on YouTube.",
                    "Playing \"$query\" on YouTube"
                )
                if (nativeResult.isSuccess) return nativeResult
            }

            val watchUri = Uri.parse("https://www.youtube.com/watch?v=$videoId")
            val webWatchIntent = Intent(Intent.ACTION_VIEW, watchUri).apply {
                if (isInstalled(context, youtubePackage)) setPackage(youtubePackage)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val watchResult = start(
                context,
                webWatchIntent,
                "Playing $query on YouTube.",
                "Playing \"$query\" on YouTube"
            )
            if (watchResult.isSuccess) return watchResult
        }

        if (isInstalled(context, youtubePackage)) {
            val playIntent = mediaPlayIntent(query, youtubePackage)
            val playResult = start(
                context,
                playIntent,
                "Playing $query on YouTube.",
                "Playing \"$query\" on YouTube"
            )
            if (playResult.isSuccess) return playResult

            val searchIntent = youtubeSearchIntent(query, youtubePackage)
            val searchResult = start(
                context,
                searchIntent,
                "Searching YouTube for $query.",
                "Searching YouTube for \"$query\""
            )
            if (searchResult.isSuccess) return searchResult
        }

        return openWebYouTube(context, query)
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

    private fun mediaPlayIntent(query: String, packageName: String): Intent {
        return Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            putExtra("query", query)
            putExtra(MediaStore.EXTRA_MEDIA_TITLE, query)
            putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            putExtra("android.intent.extra.focus", "vnd.android.cursor.item/*")
            putExtra("autostart", true)
            setPackage(packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    private fun youtubeSearchIntent(query: String, packageName: String?): Intent {
        val encoded = Uri.encode(query)
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.youtube.com/results?search_query=$encoded")
            if (!packageName.isNullOrBlank()) setPackage(packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    private fun openWebYouTube(context: Context, query: String): ToolExecutionResult {
        val intent = youtubeSearchIntent(query, null)
        if (query.isBlank()) {
            intent.data = Uri.parse("https://www.youtube.com")
        }
        return start(
            context,
            intent,
            if (query.isBlank()) "Opening YouTube." else "Searching YouTube for $query.",
            if (query.isBlank()) "Opened YouTube" else "Searching YouTube for \"$query\""
        )
    }

    private fun isInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        } catch (_: Throwable) {
            false
        }
    }

    private fun start(
        context: Context,
        intent: Intent,
        spoken: String,
        display: String
    ): ToolExecutionResult {
        return try {
            context.startActivity(intent)
            ToolExecutionResult(
                isSuccess = true,
                spokenMessage = spoken,
                displayMessage = display,
                actionIntent = intent
            )
        } catch (_: Exception) {
            failed()
        }
    }

    private fun failed(): ToolExecutionResult {
        return ToolExecutionResult(isSuccess = false, spokenMessage = "", displayMessage = "")
    }
}
