package com.aistudio.jarvis.voiceagent.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.jarvis.voiceagent.ui.theme.CyanGlow
import com.aistudio.jarvis.voiceagent.ui.theme.DeepNavyBg
import com.aistudio.jarvis.voiceagent.ui.theme.ElectricBlue
import com.aistudio.jarvis.voiceagent.ui.theme.NeonBlue
import com.aistudio.jarvis.voiceagent.ui.theme.StatusCompleted
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceBorder
import com.aistudio.jarvis.voiceagent.ui.theme.SurfaceDarkCard
import com.aistudio.jarvis.voiceagent.ui.theme.TextMuted
import com.aistudio.jarvis.voiceagent.ui.theme.TextPrimary
import com.aistudio.jarvis.voiceagent.ui.theme.TextSecondary
import com.aistudio.jarvis.voiceagent.viewmodel.JarvisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantSetupScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val openAssistantSettings = {
        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }

    Scaffold(
        containerColor = DeepNavyBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DEFAULT ASSISTANT INTEGRATION",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("assistant_setup_back")) {
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Graphic Box
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(CyanGlow.copy(alpha = 0.3f), NeonBlue.copy(alpha = 0.1f), DeepNavyBg)
                        )
                    )
                    .border(2.dp, CyanGlow, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Assistant,
                    contentDescription = null,
                    tint = CyanGlow,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Replace Default Phone Assistant",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Set JARVIS as your primary device digital assistant to trigger anytime from anywhere across Android.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button
            Button(
                onClick = openAssistantSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("set_default_assistant_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow, contentColor = DeepNavyBg)
            ) {
                Icon(imageVector = Icons.Filled.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Default Assistant Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "HOW TO TRIGGER JARVIS ANYWHERE:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyanGlow,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            TriggerMethodCard(
                icon = Icons.Outlined.Gesture,
                title = "1. Corner Swipe Gesture",
                description = "Swipe diagonally upwards from the bottom left or bottom right corner of your phone screen."
            )

            Spacer(modifier = Modifier.height(10.dp))

            TriggerMethodCard(
                icon = Icons.Filled.PowerSettingsNew,
                title = "2. Long Press Power Button",
                description = "On modern Android devices, hold down the Power / Side Key for 0.5s to launch JARVIS immediately."
            )

            Spacer(modifier = Modifier.height(10.dp))

            TriggerMethodCard(
                icon = Icons.Filled.Mic,
                title = "3. Wake-Word \"Hey Jarvis\"",
                description = "When the app is open or active in the foreground, simply say \"Hey Jarvis\" to initiate voice listening."
            )

            Spacer(modifier = Modifier.height(10.dp))

            TriggerMethodCard(
                icon = Icons.Outlined.Sensors,
                title = "4. Quick Settings Tile",
                description = "Add the JARVIS Quick Tile from your Android notification shade for instant one-tap voice commands."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TriggerMethodCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDarkCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyanGlow.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyanGlow,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
