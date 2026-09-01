package com.aistudio.jarvis.voiceagent.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.jarvis.voiceagent.model.JarvisState
import com.aistudio.jarvis.voiceagent.ui.theme.CyanGlow
import com.aistudio.jarvis.voiceagent.ui.theme.NeonBlue
import com.aistudio.jarvis.voiceagent.ui.theme.StatusCompleted
import com.aistudio.jarvis.voiceagent.ui.theme.StatusError
import com.aistudio.jarvis.voiceagent.ui.theme.StatusExecuting
import com.aistudio.jarvis.voiceagent.ui.theme.StatusListening
import com.aistudio.jarvis.voiceagent.ui.theme.StatusReady
import com.aistudio.jarvis.voiceagent.ui.theme.StatusThinking
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceBorder
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceDarkCard
import com.aistudio.jarvis.voiceagent.ui.theme.TextMuted
import com.aistudio.jarvis.voiceagent.ui.theme.TextPrimary
import com.aistudio.jarvis.voiceagent.ui.theme.TextSecondary
import com.aistudio.jarvis.voiceagent.ui.theme.TextWhite

@Composable
fun HudHeader(
    state: JarvisState,
    wakeWordEnabled: Boolean,
    onSettingsClick: () -> Unit,
    onAssistantSetupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (state) {
            JarvisState.READY -> StatusReady
            JarvisState.LISTENING -> StatusListening
            JarvisState.THINKING -> StatusThinking
            JarvisState.CONFIRMATION_REQUIRED -> StatusExecuting
            JarvisState.EXECUTING -> StatusExecuting
            JarvisState.COMPLETED -> StatusCompleted
            JarvisState.ERROR -> StatusError
        },
        animationSpec = tween(300),
        label = "status_color"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("hud_header")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Title & Model badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDarkCard)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sensors,
                        contentDescription = "Core HUD",
                        tint = CyanGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "JARVIS",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = TextWhite,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI OS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (state) {
                                JarvisState.READY -> "SYSTEM READY"
                                JarvisState.LISTENING -> "LISTENING ACTIVE"
                                JarvisState.THINKING -> "PROCESSING QUERY"
                                JarvisState.CONFIRMATION_REQUIRED -> "APPROVAL REQUIRED"
                                JarvisState.EXECUTING -> "EXECUTING TASK"
                                JarvisState.COMPLETED -> "TASK COMPLETED"
                                JarvisState.ERROR -> "SYSTEM ALERT"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = statusColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Right Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Assistant Setup Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceDarkCard)
                        .border(1.dp, SurfaceBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onAssistantSetupClick,
                        modifier = Modifier.size(40.dp).testTag("header_assistant_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PowerSettingsNew,
                            contentDescription = "Default Assistant Setup",
                            tint = CyanGlow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Settings Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceDarkCard)
                        .border(1.dp, SurfaceBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(40.dp).testTag("header_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

