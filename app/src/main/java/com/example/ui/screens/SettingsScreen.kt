package com.aistudio.jarvis.voiceagent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.jarvis.voiceagent.ui.theme.CyanGlow
import com.aistudio.jarvis.voiceagent.ui.theme.DeepNavyBg
import com.aistudio.jarvis.voiceagent.ui.theme.NeonBlue
import com.aistudio.jarvis.voiceagent.ui.theme.StatusCompleted
import com.aistudio.jarvis.voiceagent.ui.theme.StatusError
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceBorder
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceDarkCard
import com.aistudio.jarvis.voiceagent.ui.theme.TextMuted
import com.aistudio.jarvis.voiceagent.ui.theme.TextPrimary
import com.aistudio.jarvis.voiceagent.ui.theme.TextSecondary
import com.aistudio.jarvis.voiceagent.viewmodel.JarvisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAssistantSetup: () -> Unit,
    onNavigateToArchitecture: () -> Unit = {},
    onNavigateToSmartCallSettings: () -> Unit = {},
    onNavigateToCallSummaries: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    val userName by viewModel.userName.collectAsState()
    val wakeWordEnabled by viewModel.wakeWordEnabled.collectAsState()
    val wakePhrase by viewModel.wakePhrase.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState()
    val voicePitch by viewModel.voicePitch.collectAsState()
    val voiceSpeed by viewModel.voiceSpeed.collectAsState()
    val soundEffects by viewModel.soundEffects.collectAsState()
    val haptics by viewModel.haptics.collectAsState()
    val offlineMode by viewModel.offlineMode.collectAsState()

    var showNameDialog by remember { mutableStateOf(false) }
    var showWipeDialog by remember { mutableStateOf(false) }

    val languages = listOf(
        "en-US" to "English (US)",
        "hi-IN" to "Hindi (हिंदी)",
        "kn-IN" to "Kannada (ಕನ್ನಡ)",
        "ta-IN" to "Tamil (தமிழ்)",
        "te-IN" to "Telugu (తెలుగు)",
        "ml-IN" to "Malayalam (മലയാളം)",
        "mr-IN" to "Marathi (मराठी)",
        "bn-IN" to "Bengali (বাংলা)"
    )

    var langDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DeepNavyBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS & CONFIG",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CyanGlow
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavyBg)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: User Profile
            SettingsSectionHeader(title = "USER IDENTITY & ADDRESSING")

            SettingsItemCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNameDialog = true }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = CyanGlow)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Preferred Addressing Name", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Currently: \"$userName\"", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                }
            }

            // Section 2: Voice & Audio Tuning
            SettingsSectionHeader(title = "VOICE ENGINE & SYNTHESIS")

            SettingsItemCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Language Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = CyanGlow)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Voice Language", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        ExposedDropdownMenuBox(
                            expanded = langDropdownExpanded,
                            onExpandedChange = { langDropdownExpanded = it }
                        ) {
                            val currentLabel = languages.find { it.first == languageCode }?.second ?: "English (US)"
                            Box(
                                modifier = Modifier
                                    .menuAnchor()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceDarkCard)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = currentLabel, fontSize = 11.sp, color = CyanGlow, fontWeight = FontWeight.Bold)
                            }
                            ExposedDropdownMenu(
                                expanded = langDropdownExpanded,
                                onDismissRequest = { langDropdownExpanded = false }
                            ) {
                                languages.forEach { (code, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name, fontSize = 12.sp) },
                                        onClick = {
                                            viewModel.updateLanguage(code)
                                            langDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pitch Slider
                    Text(
                        text = "Voice Pitch: ${String.format("%.1f", voicePitch)}x",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Slider(
                        value = voicePitch,
                        onValueChange = { viewModel.updateVoiceCharacteristics(it, voiceSpeed) },
                        valueRange = 0.5f..1.8f,
                        colors = SliderDefaults.colors(thumbColor = CyanGlow, activeTrackColor = CyanGlow)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Speed Slider
                    Text(
                        text = "Speech Rate: ${String.format("%.2f", voiceSpeed)}x",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Slider(
                        value = voiceSpeed,
                        onValueChange = { viewModel.updateVoiceCharacteristics(voicePitch, it) },
                        valueRange = 0.5f..1.8f,
                        colors = SliderDefaults.colors(thumbColor = CyanGlow, activeTrackColor = CyanGlow)
                    )
                }
            }

            // Section 3: Wake-word & Assistant
            SettingsSectionHeader(title = "ASSISTANT & WAKE MECHANISM")

            SettingsItemCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = CyanGlow)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Wake-Word Detection", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "Phrase: \"$wakePhrase\"", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        Switch(
                            checked = wakeWordEnabled,
                            onCheckedChange = { viewModel.toggleWakeWord(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanGlow, checkedTrackColor = CyanGlow.copy(alpha = 0.3f))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAssistantSetup() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Default Assistant Setup Guide", fontSize = 12.sp, color = CyanGlow)
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = CyanGlow)
                    }
                }
            }

            // Section 4: Haptics & Sound FX
            SettingsSectionHeader(title = "SENSORY FEEDBACK & PERFORMANCE")

            SettingsItemCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = CyanGlow)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "High-Tech Audio Chimes", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Switch(
                            checked = soundEffects,
                            onCheckedChange = { viewModel.toggleSoundEffects(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanGlow, checkedTrackColor = CyanGlow.copy(alpha = 0.3f))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Vibration, contentDescription = null, tint = CyanGlow)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Haptic Vibration Cues", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Switch(
                            checked = haptics,
                            onCheckedChange = { viewModel.toggleHaptics(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanGlow, checkedTrackColor = CyanGlow.copy(alpha = 0.3f))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = CyanGlow)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Offline Mode Strict", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "Disable cloud AI queries entirely", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        Switch(
                            checked = offlineMode,
                            onCheckedChange = { viewModel.toggleOfflineMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanGlow, checkedTrackColor = CyanGlow.copy(alpha = 0.3f))
                        )
                    }
                }
            }

            // Section 5: ULLAS Production Architecture & Gateway Telemetry
            SettingsSectionHeader(title = "CLOUD ARCHITECTURE & TELEMETRY")

            SettingsItemCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToArchitecture() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CyanGlow)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Production Architecture & Gateway", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "Rate limiting, auth quotas, cache stats, telemetry", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = CyanGlow)
                    }
                }
            }

            // Section 6: Privacy & Wipe
            SettingsSectionHeader(title = "PRIVACY & LOCAL DATA")

            SettingsItemCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPrivacy() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = StatusCompleted)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Privacy & Data Architecture", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showWipeDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusError.copy(alpha = 0.2f),
                            contentColor = StatusError
                        )
                    ) {
                        Icon(imageVector = Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Instant Wipe All Data & Memory", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Section 7: Smart Call Assistant
            SettingsSectionHeader(title = "SMART CALL ASSISTANT")

            SettingsItemCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Smart Call Settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToSmartCallSettings() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = CyanGlow
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Smart Call Settings", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Configure caller announcements, auto-answer & privacy", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Call Summaries
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToCallSummaries() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = CyanGlow
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Call Summaries", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("View AI-generated summaries from Jarvis-handled calls", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Change Name Dialog
        if (showNameDialog) {
            var tempName by remember { mutableStateOf(userName) }
            AlertDialog(
                onDismissRequest = { showNameDialog = false },
                containerColor = SurfaceDarkCard,
                title = { Text("Addressing Name", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Your Name or Title (e.g. Tony, Sir, Chief)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanGlow,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (tempName.isNotBlank()) {
                                viewModel.updateUserName(tempName.trim())
                                showNameDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow, contentColor = DeepNavyBg)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showNameDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Wipe Confirmation Dialog
        if (showWipeDialog) {
            AlertDialog(
                onDismissRequest = { showWipeDialog = false },
                containerColor = SurfaceDarkCard,
                title = { Text("Wipe All Local Data?", color = StatusError, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "This will permanently erase all history logs, memory entities, notes, and reminders stored on this phone.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllHistory()
                            viewModel.clearAllMemory()
                            viewModel.clearAllNotes()
                            showWipeDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                    ) {
                        Text("Erase Everything", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showWipeDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.2.sp,
        color = CyanGlow
    )
}

@Composable
private fun SettingsItemCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDarkCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
    ) {
        content()
    }
}
