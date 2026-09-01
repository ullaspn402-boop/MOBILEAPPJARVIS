package com.aistudio.jarvis.voiceagent

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aistudio.jarvis.voiceagent.ui.screens.AssistantSetupScreen
import com.aistudio.jarvis.voiceagent.ui.screens.CallSummaryScreen
import com.aistudio.jarvis.voiceagent.ui.screens.ConversationHistoryScreen
import com.aistudio.jarvis.voiceagent.ui.screens.MainAssistantScreen
import com.aistudio.jarvis.voiceagent.ui.screens.MemoryScreen
import com.aistudio.jarvis.voiceagent.ui.screens.NotificationsSummaryScreen
import com.aistudio.jarvis.voiceagent.ui.screens.PermissionsScreen
import com.aistudio.jarvis.voiceagent.ui.screens.PrivacyScreen
import com.aistudio.jarvis.voiceagent.ui.screens.SettingsScreen
import com.aistudio.jarvis.voiceagent.ui.screens.SmartCallSettingsScreen
import com.aistudio.jarvis.voiceagent.ui.screens.ToolsCatalogScreen
import com.aistudio.jarvis.voiceagent.ui.screens.UllasArchitectureScreen
import com.aistudio.jarvis.voiceagent.ui.theme.MyApplicationTheme
import com.aistudio.jarvis.voiceagent.viewmodel.JarvisViewModel

class MainActivity : ComponentActivity() {

    private var viewModel: JarvisViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // NOTE: Do NOT call setTheme() before super.onCreate() — it violates the Android lifecycle
        // and causes ResourcesNotFoundException on sideloaded APKs on Android 12+.
        // The theme is already declared in AndroidManifest.xml via android:theme.
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
        } catch (_: Throwable) {
        }

        val launched = try {
            setContent {
                MyApplicationTheme {
                    JarvisStartup(activity = this@MainActivity)
                }
            }
            true
        } catch (t: Throwable) {
            Log.e("MainActivity", "Compose setContent failed", t)
            false
        }

        if (!launched) {
            try {
                setContentView(R.layout.activity_safe_root)
            } catch (inner: Throwable) {
                Log.e("MainActivity", "Safe layout failed", inner)
            }
        }

        if (savedInstanceState == null) {
            try {
                handleIntent(intent)
            } catch (_: Throwable) {
            }
        }
    }

    internal fun obtainViewModel(): JarvisViewModel {
        val existing = viewModel
        if (existing != null) return existing
        val created = ViewModelProvider(this)[JarvisViewModel::class.java]
        viewModel = created
        // Register activity context immediately so call tools work from first use
        try { created.setActivityContext(this) } catch (_: Throwable) {}
        return created
    }

    override fun onResume() {
        super.onResume()
        // Register Activity context so tools like CallContactTool can use ACTION_CALL
        // (requires foreground Activity context on Android 10+)
        try {
            viewModel?.setActivityContext(this)
        } catch (_: Throwable) {}
    }

    override fun onPause() {
        super.onPause()
        // Unregister to avoid Activity context leaks when app goes to background
        try {
            viewModel?.clearActivityContext()
        } catch (_: Throwable) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        try {
            handleIntent(intent)
        } catch (_: Throwable) {
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action == Intent.ACTION_ASSIST || action == Intent.ACTION_VOICE_COMMAND) {
            try {
                viewModel?.toggleVoiceListening()
            } catch (_: Throwable) {
            }
        }
    }
}

@Composable
private fun JarvisStartup(activity: MainActivity) {
    var viewModel by remember { mutableStateOf<JarvisViewModel?>(null) }
    var initError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            viewModel = activity.obtainViewModel()
        } catch (t: Throwable) {
            Log.e("MainActivity", "Failed to create JarvisViewModel", t)
            initError = t.message ?: t.javaClass.simpleName
        }
    }

    val vm = viewModel
    when {
        vm != null -> JarvisNavGraph(viewModel = vm)
        initError != null -> StartupFallbackScreen(
            message = "JARVIS could not finish starting.\n$initError"
        )
        else -> StartupFallbackScreen(message = "Starting JARVIS…")
    }
}

@Composable
private fun StartupFallbackScreen(
    message: String = "JARVIS is starting.\nIf this screen stays, reopen the app after granting permissions."
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050B14))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color(0xFF00E5FF),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun JarvisNavGraph(viewModel: JarvisViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main",
        modifier = Modifier.fillMaxSize(),
        enterTransition = { fadeIn(animationSpec = tween(250)) },
        exitTransition = { fadeOut(animationSpec = tween(250)) }
    ) {
        composable("main") {
            MainAssistantScreen(
                viewModel = viewModel,
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToMemory = { navController.navigate("memory") },
                onNavigateToTools = { navController.navigate("tools") },
                onNavigateToNotifications = { navController.navigate("notifications") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToAssistantSetup = { navController.navigate("assistant_setup") },
                onNavigateToPermissions = { navController.navigate("permissions") }
            )
        }

        composable("history") {
            ConversationHistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("memory") {
            MemoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("tools") {
            ToolsCatalogScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onExecuteSampleCommand = { phrase ->
                    navController.popBackStack()
                    viewModel.processCommand(phrase)
                }
            )
        }

        composable("notifications") {
            NotificationsSummaryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate("privacy") },
                onNavigateToAssistantSetup = { navController.navigate("assistant_setup") },
                onNavigateToArchitecture = { navController.navigate("architecture") },
                onNavigateToSmartCallSettings = { navController.navigate("smart_call_settings") },
                onNavigateToCallSummaries = { navController.navigate("call_summaries") }
            )
        }

        composable("architecture") {
            UllasArchitectureScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("assistant_setup") {
            AssistantSetupScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("permissions") {
            PermissionsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("privacy") {
            PrivacyScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("smart_call_settings") {
            SmartCallSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("call_summaries") {
            CallSummaryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
