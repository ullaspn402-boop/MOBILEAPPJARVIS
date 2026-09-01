package com.aistudio.jarvis.voiceagent.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.jarvis.voiceagent.data.call.AutoAnswerScope
import com.aistudio.jarvis.voiceagent.ui.theme.*
import com.aistudio.jarvis.voiceagent.viewmodel.JarvisViewModel

/**
 * Settings screen for the Jarvis Smart Call Assistant.
 *
 * Provides full user control over all Smart Call features with privacy-first defaults.
 * Includes Default Dialer explanation, auto-answer configuration, and data storage options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCallSettingsScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.smartCallSettings.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Smart Call Assistant",
                        color = CyanGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CyanGlow
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF050B14)
                )
            )
        },
        containerColor = Color(0xFF050B14)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Master toggle ───────────────────────────────────────────────
            SettingsCard(
                title = "Smart Call Assistant",
                icon = Icons.Default.SmartToy,
                accentColor = CyanGlow
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Enable Smart Call Assistant",
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Jarvis will announce callers and listen for your commands",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = settings.smartCallAssistantEnabled,
                        onCheckedChange = { viewModel.updateSmartCallEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanGlow,
                            checkedTrackColor = CyanGlow.copy(alpha = 0.3f)
                        )
                    )
                }

                AnimatedVisibility(
                    visible = !settings.smartCallAssistantEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        "⚠ Smart Call Assistant is disabled. Jarvis will not interact with incoming calls.",
                        color = AmberAccent,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmberAccent.copy(alpha = 0.08f))
                            .padding(10.dp)
                    )
                }
            }

            // ── Default Dialer section ──────────────────────────────────────
            AnimatedVisibility(visible = settings.smartCallAssistantEnabled) {
                SettingsCard(
                    title = "Default Phone App",
                    icon = Icons.Default.PhoneInTalk,
                    accentColor = Color(0xFF6C63FF)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "For full call control (accept & decline), Jarvis works best as your default phone app.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        InfoBadge(
                            text = "Without this role, Jarvis can still announce callers and listen for commands, " +
                                    "but call accept/decline may be limited on some devices.",
                            color = Color(0xFF6C63FF)
                        )
                        Button(
                            onClick = { viewModel.requestDefaultDialerRole() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6C63FF).copy(alpha = 0.15f),
                                contentColor = Color(0xFF6C63FF)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6C63FF).copy(0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DialerSip, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Set Jarvis as Default Phone App")
                        }
                    }
                }
            }

            // ── Speaker mode ────────────────────────────────────────────────
            AnimatedVisibility(visible = settings.smartCallAssistantEnabled) {
                SettingsCard(
                    title = "AI Answering Mode",
                    icon = Icons.Default.VolumeUp,
                    accentColor = EmeraldAccent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Use Speaker Phone",
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Required for Jarvis to hear and speak to callers. Disable for earpiece-only (AI answering won't work).",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = settings.useSpeakerForJarvisAnswer,
                            onCheckedChange = { viewModel.updateSmartCallUseSpeaker(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldAccent,
                                checkedTrackColor = EmeraldAccent.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // ── Auto-answer ─────────────────────────────────────────────────
            AnimatedVisibility(visible = settings.smartCallAssistantEnabled) {
                SettingsCard(
                    title = "Auto-Answer",
                    icon = Icons.Default.PhoneCallback,
                    accentColor = AmberAccent
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        InfoBadge(
                            text = "Auto-answering is OFF by default. When enabled, Jarvis will answer calls on your behalf after the configured delay.",
                            color = AmberAccent
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Enable Auto-Answer",
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = settings.autoAnswerEnabled,
                                onCheckedChange = { viewModel.updateSmartCallAutoAnswer(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AmberAccent,
                                    checkedTrackColor = AmberAccent.copy(alpha = 0.3f)
                                )
                            )
                        }

                        AnimatedVisibility(visible = settings.autoAnswerEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Delay slider
                                Text(
                                    "Answer after ${settings.autoAnswerDelaySeconds} seconds",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                                Slider(
                                    value = settings.autoAnswerDelaySeconds.toFloat(),
                                    onValueChange = { viewModel.updateSmartCallAutoAnswerDelay(it.toInt()) },
                                    valueRange = 5f..60f,
                                    steps = 10,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AmberAccent,
                                        activeTrackColor = AmberAccent
                                    )
                                )

                                // Scope selector
                                Text(
                                    "Auto-answer for:",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                AutoAnswerScopeSelector(
                                    current = settings.autoAnswerScope,
                                    onSelect = { viewModel.updateSmartCallAutoAnswerScope(it) }
                                )
                            }
                        }
                    }
                }
            }

            // ── Caller transparency ─────────────────────────────────────────
            AnimatedVisibility(visible = settings.smartCallAssistantEnabled) {
                SettingsCard(
                    title = "Caller Transparency",
                    icon = Icons.Default.Info,
                    accentColor = CyanGlow
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Inform Caller of AI",
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Jarvis will introduce itself as an AI assistant at the start of every handled call.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = settings.informCallerOfAi,
                            onCheckedChange = { viewModel.updateSmartCallInformCaller(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanGlow,
                                checkedTrackColor = CyanGlow.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // ── Data storage ────────────────────────────────────────────────
            SettingsCard(
                title = "Data & Privacy",
                icon = Icons.Default.Lock,
                accentColor = PurpleAccent
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    Text(
                        "Your calls belong to you. Jarvis processes minimum information necessary.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Save summaries
                    PrivacyToggleRow(
                        label = "Save Call Summaries",
                        subtitle = "Short AI-generated notes from Jarvis-handled calls (stored locally)",
                        checked = settings.saveSummaries,
                        onChecked = { viewModel.updateSmartCallSaveSummaries(it) },
                        accentColor = PurpleAccent
                    )

                    HorizontalDivider(color = Color(0xFF1E2A3A))

                    // Save transcripts
                    PrivacyToggleRow(
                        label = "Save Transcripts",
                        subtitle = "Full turn-by-turn conversation text (stored locally)",
                        checked = settings.saveTranscripts,
                        onChecked = { viewModel.updateSmartCallSaveTranscripts(it) },
                        accentColor = PurpleAccent
                    )

                    HorizontalDivider(color = Color(0xFF1E2A3A))

                    // Audio recording
                    PrivacyToggleRow(
                        label = "Audio Recording",
                        subtitle = "Disabled in this version. Planned for future release with full consent flow.",
                        checked = false,
                        onChecked = { /* No-op in prototype */ },
                        accentColor = Color(0xFF556677),
                        enabled = false
                    )
                }
            }

            // ── Privacy principle footer ────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x0A00F2FF))
                    .border(1.dp, Color(0x1A00F2FF), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "🔒 Privacy Principle: Your calls and conversations belong to you. " +
                            "Jarvis processes the minimum information necessary, clearly discloses when AI is involved, " +
                            "and gives you full control over your data.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D1117))
            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            Text(title, color = accentColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        HorizontalDivider(color = accentColor.copy(alpha = 0.1f))
        content()
    }
}

@Composable
private fun InfoBadge(text: String, color: Color) {
    Text(
        text,
        color = color.copy(alpha = 0.8f),
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(10.dp)
    )
}

@Composable
private fun PrivacyToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    accentColor: Color,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = if (enabled) TextPrimary else TextMuted, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun AutoAnswerScopeSelector(
    current: AutoAnswerScope,
    onSelect: (AutoAnswerScope) -> Unit
) {
    val options = listOf(
        AutoAnswerScope.ALL to "All callers",
        AutoAnswerScope.UNKNOWN_ONLY to "Unknown callers only",
        AutoAnswerScope.CONTACTS_ONLY to "Contacts only",
        AutoAnswerScope.NEVER to "Never (disabled)"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (scope, label) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RadioButton(
                    selected = current == scope,
                    onClick = { onSelect(scope) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = AmberAccent
                    )
                )
                Text(label, color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}
