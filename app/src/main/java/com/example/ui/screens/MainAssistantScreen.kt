package com.aistudio.jarvis.voiceagent.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aistudio.jarvis.voiceagent.model.JarvisState
import com.aistudio.jarvis.voiceagent.ui.components.ConfirmationModal
import com.aistudio.jarvis.voiceagent.ui.components.FuturisticOrb
import com.aistudio.jarvis.voiceagent.ui.components.HudHeader
import com.aistudio.jarvis.voiceagent.ui.components.PlanProgressCard
import com.aistudio.jarvis.voiceagent.ui.theme.CyanGlow
import com.aistudio.jarvis.voiceagent.ui.theme.DeepNavyBg
import com.aistudio.jarvis.voiceagent.ui.theme.ElectricBlue
import com.aistudio.jarvis.voiceagent.ui.theme.NeonBlue
import com.aistudio.jarvis.voiceagent.ui.theme.SleekBlackBg
import com.aistudio.jarvis.voiceagent.ui.theme.StatusListening
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceBorder
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceDark
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceDarkCard
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceDarkGlass
import com.aistudio.jarvis.voiceagent.ui.theme.TextMuted
import com.aistudio.jarvis.voiceagent.ui.theme.TextPrimary
import com.aistudio.jarvis.voiceagent.ui.theme.TextSecondary
import com.aistudio.jarvis.voiceagent.ui.theme.TextWhite
import com.aistudio.jarvis.voiceagent.viewmodel.JarvisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAssistantScreen(
    viewModel: JarvisViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAssistantSetup: () -> Unit,
    onNavigateToPermissions: () -> Unit
) {
    val context = LocalContext.current
    val jarvisState by viewModel.jarvisState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isListening by viewModel.voiceEngine.isListening.collectAsState()
    val liveTranscript by viewModel.voiceEngine.liveTranscript.collectAsState()
    val audioRms by viewModel.voiceEngine.currentRms.collectAsState()
    val currentPlan by viewModel.currentPlan.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmationPlan.collectAsState()
    val wakeWordEnabled by viewModel.wakeWordEnabled.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Permission launcher for Record Audio
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleVoiceListening()
        }
    }

    val handleMicClick = {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            viewModel.toggleVoiceListening()
        } else {
            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val quickSuggestions = listOf(
        "Play Believer on YouTube",
        "Call Mom",
        "Set alarm for 6 AM",
        "I'm leaving for college",
        "Read my recent notifications",
        "Take a note: buy charger",
        "Open Google Maps",
        "What is the weather?"
    )

    Scaffold(
        containerColor = DeepNavyBg,
        topBar = {
            HudHeader(
                state = jarvisState,
                wakeWordEnabled = wakeWordEnabled,
                onSettingsClick = onNavigateToSettings,
                onAssistantSetupClick = onNavigateToAssistantSetup
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Hero Futuristic AI Orb
                FuturisticOrb(
                    state = jarvisState,
                    audioRms = audioRms,
                    onClick = handleMicClick
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Dynamic Status & Transcript Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(SurfaceDarkCard)
                        .border(1.dp, if (isListening) CyanGlow.copy(alpha = 0.5f) else SurfaceBorder, RoundedCornerShape(18.dp))
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isListening && liveTranscript.isNotBlank()) "\"$liveTranscript\"" else statusMessage,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isListening) CyanGlow else TextPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        if (isListening) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(CyanGlow)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LISTENING LIVE...",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyanGlow,
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Confirmation Modal if high-risk action requires approval
                val confirmationPlan = pendingConfirmation
                if (confirmationPlan != null) {
                    ConfirmationModal(
                        plan = confirmationPlan,
                        onConfirm = { viewModel.confirmPendingAction() },
                        onCancel = { viewModel.cancelPendingAction() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Multi-step Execution Plan Checklist
                val activePlan = currentPlan
                if (activePlan != null && activePlan.steps.isNotEmpty() && confirmationPlan == null) {
                    PlanProgressCard(plan = activePlan)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Quick Suggestion Chips
                Text(
                    text = "COMMAND SUGGESTIONS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickSuggestions.forEach { suggestion ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(SurfaceDarkCard)
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                                .clickable {
                                    viewModel.processCommand(suggestion)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("suggestion_chip_$suggestion")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(CyanGlow.copy(alpha = 0.8f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = suggestion,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Floating Controls: Keyboard Input Bar + Big Mic + Quick Nav Hub
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Command Text Input Field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(SurfaceDarkCard)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(28.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("command_input_field"),
                        placeholder = {
                            Text(
                                text = "Type command or query...",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = CyanGlow
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInput.isNotBlank()) {
                                viewModel.processCommand(textInput)
                                textInput = ""
                            }
                        })
                    )

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.processCommand(textInput)
                                textInput = ""
                            }
                        },
                        modifier = Modifier.testTag("command_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Command",
                            tint = CyanGlow,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Mic Quick Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListening)
                                    Brush.linearGradient(listOf(CyanGlow, NeonBlue))
                                else
                                    Brush.linearGradient(listOf(SurfaceDarkCard, SurfaceDarkCard))
                            )
                            .border(1.dp, if (isListening) CyanGlow else SurfaceBorder, CircleShape)
                            .clickable(onClick = handleMicClick)
                            .testTag("main_mic_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Filled.Mic else Icons.Filled.Mic,
                            contentDescription = "Voice Input",
                            tint = if (isListening) SleekBlackBg else CyanGlow,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Navigation Hub
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavHubItem(
                        icon = Icons.Filled.History,
                        label = "History",
                        testTag = "nav_history_btn",
                        onClick = onNavigateToHistory
                    )
                    NavHubItem(
                        icon = Icons.Outlined.Memory,
                        label = "Memory",
                        testTag = "nav_memory_btn",
                        onClick = onNavigateToMemory
                    )
                    NavHubItem(
                        icon = Icons.Outlined.Build,
                        label = "Tools",
                        testTag = "nav_tools_btn",
                        onClick = onNavigateToTools
                    )
                    NavHubItem(
                        icon = Icons.Outlined.Notifications,
                        label = "Alerts",
                        testTag = "nav_alerts_btn",
                        onClick = onNavigateToNotifications
                    )
                    NavHubItem(
                        icon = Icons.Outlined.Security,
                        label = "Perms",
                        testTag = "nav_permissions_btn",
                        onClick = onNavigateToPermissions
                    )
                }
            }
        }
    }
}

@Composable
private fun NavHubItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceDarkCard)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = CyanGlow,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace
        )
    }
}
