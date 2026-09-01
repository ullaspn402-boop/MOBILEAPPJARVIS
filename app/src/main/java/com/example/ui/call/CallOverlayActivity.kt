package com.aistudio.jarvis.voiceagent.ui.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.jarvis.voiceagent.data.service.SmartCallService
import com.aistudio.jarvis.voiceagent.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-screen incoming call HUD displayed when Jarvis detects an incoming call.
 *
 * Shows:
 *  - Caller name / number
 *  - Live waveform animation while listening
 *  - Three action buttons: Pick Up / Jarvis Answer / Decline
 *  - Current Jarvis status (announcing, listening, conversing, etc.)
 *
 * Launched by [SmartCallService] as a full-screen intent overlay.
 * Dismisses itself when the call state changes to IDLE, CALL_ACTIVE, or CALL_DECLINED.
 */
class CallOverlayActivity : ComponentActivity() {

    private var callerName by mutableStateOf("Unknown")
    private var callerNumber by mutableStateOf("")
    private var overlayState by mutableStateOf(OverlayState.ANNOUNCING)

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getStringExtra(SmartCallService.BROADCAST_EXTRA_STATE) ?: return
            callerName = intent.getStringExtra(SmartCallService.BROADCAST_EXTRA_CALLER_NAME) ?: "Unknown"
            callerNumber = intent.getStringExtra(SmartCallService.BROADCAST_EXTRA_CALLER_NUMBER) ?: ""

            overlayState = when (state) {
                "ANNOUNCING" -> OverlayState.ANNOUNCING
                "WAITING" -> OverlayState.WAITING_FOR_COMMAND
                "CALL_ACTIVE" -> { finish(); return }
                "JARVIS_ANSWERING", "JARVIS_CONVERSING" -> OverlayState.JARVIS_CONVERSING
                "DECLINING", "CALL_DECLINED" -> { finish(); return }
                "SUMMARY" -> OverlayState.SUMMARY
                "IDLE" -> { finish(); return }
                else -> overlayState
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        callerName = intent?.getStringExtra(SmartCallService.BROADCAST_EXTRA_CALLER_NAME) ?: "Unknown"
        callerNumber = intent?.getStringExtra(SmartCallService.BROADCAST_EXTRA_CALLER_NUMBER) ?: ""

        // Register broadcast receiver for state updates
        val filter = IntentFilter(SmartCallService.BROADCAST_CALL_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(stateReceiver, filter)
        }

        setContent {
            CallOverlayTheme {
                val name by remember { derivedStateOf { callerName } }
                val state by remember { derivedStateOf { overlayState } }
                CallOverlayScreen(
                    callerName = name,
                    state = state,
                    onPickUp = { sendCommandToService("PICK_UP") },
                    onJarvisAnswer = { sendCommandToService("JARVIS_ANSWER") },
                    onDecline = { sendCommandToService("DECLINE") }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(stateReceiver) } catch (_: Throwable) {}
    }

    private fun sendCommandToService(command: String) {
        val intent = Intent(this, SmartCallService::class.java).apply {
            action = SmartCallService.ACTION_USER_COMMAND
            putExtra(SmartCallService.EXTRA_USER_COMMAND, command)
        }
        startService(intent)
    }
}

// ─── Overlay states ────────────────────────────────────────────────────────────
enum class OverlayState {
    ANNOUNCING,
    WAITING_FOR_COMMAND,
    JARVIS_CONVERSING,
    SUMMARY
}

// ─── Composable UI ────────────────────────────────────────────────────────────

@Composable
fun CallOverlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF050505),
            surface = Color(0xFF0D1117),
            primary = Color(0xFF00F2FF),
        ),
        content = content
    )
}

@Composable
fun CallOverlayScreen(
    callerName: String,
    state: OverlayState,
    onPickUp: () -> Unit,
    onJarvisAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF050B14), Color(0xFF020509))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background animated rings
        AnimatedRings()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            // Jarvis icon
            JarvisCallIcon(isActive = state == OverlayState.JARVIS_CONVERSING)

            Spacer(Modifier.height(8.dp))

            // Caller name
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📞 $callerName",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "is calling you",
                        color = Color(0xFF00F2FF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            // Status row
            StatusIndicator(state = state)

            Spacer(Modifier.height(16.dp))

            // Action buttons (only shown when waiting for user command)
            AnimatedVisibility(
                visible = state == OverlayState.WAITING_FOR_COMMAND ||
                        state == OverlayState.ANNOUNCING,
                enter = fadeIn() + scaleIn()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x1A00F2FF))
                        .border(1.dp, Color(0x3300F2FF), RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Say a command or tap:",
                        color = Color(0xFF8899AA),
                        fontSize = 13.sp
                    )

                    // Pick Up button
                    CallActionButton(
                        label = "\"Pick Up\"",
                        icon = Icons.Default.Call,
                        color = Color(0xFF10B981),
                        onClick = onPickUp
                    )

                    // Jarvis Answer button
                    CallActionButton(
                        label = "\"Answer Them\" (Jarvis talks)",
                        icon = Icons.Default.SmartToy,
                        color = Color(0xFF00F2FF),
                        onClick = onJarvisAnswer
                    )

                    // Decline button
                    CallActionButton(
                        label = "\"Don't Pick Up\"",
                        icon = Icons.Default.CallEnd,
                        color = Color(0xFFF43F5E),
                        onClick = onDecline
                    )
                }
            }

            // Jarvis conversing state info
            AnimatedVisibility(visible = state == OverlayState.JARVIS_CONVERSING) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WaveformAnimation()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "🤖 Jarvis is handling the call",
                        color = Color(0xFF00F2FF),
                        fontSize = 14.sp
                    )
                    Text(
                        "Tap outside to open Jarvis",
                        color = Color(0xFF556677),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun JarvisCallIcon(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.08f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(pulse)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF003344), Color(0xFF001122))
                )
            )
            .border(2.dp, Color(0xFF00F2FF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = "Jarvis",
            tint = Color(0xFF00F2FF),
            modifier = Modifier.size(52.dp)
        )
    }
}

@Composable
private fun StatusIndicator(state: OverlayState) {
    val (icon, label, color) = when (state) {
        OverlayState.ANNOUNCING -> Triple("🔊", "Announcing caller...", Color(0xFFA855F7))
        OverlayState.WAITING_FOR_COMMAND -> Triple("🎙", "Listening for your command...", Color(0xFF00F2FF))
        OverlayState.JARVIS_CONVERSING -> Triple("💬", "Jarvis is speaking with caller", Color(0xFF10B981))
        OverlayState.SUMMARY -> Triple("📋", "Generating summary...", Color(0xFFF59E0B))
    }

    val alpha by rememberInfiniteTransition(label = "status_blink").animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x22FFFFFF))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(icon, fontSize = 14.sp)
        Text(
            label,
            color = color.copy(alpha = alpha),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CallActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(targetValue = 1f, label = "btn_scale")

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun AnimatedRings() {
    val infiniteTransition = rememberInfiniteTransition(label = "rings")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "ring_angle"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * 0.28f)
        for (r in listOf(160f, 220f, 280f)) {
            val strokeAlpha = (0.06f * (1f - (r - 160f) / 200f)).coerceAtLeast(0.01f)
            drawCircle(
                color = android.graphics.Color.parseColor("#00F2FF").let {
                    Color(it).copy(alpha = strokeAlpha)
                },
                radius = r,
                center = center,
                style = Stroke(width = 1.5f)
            )
        }
        // Rotating arc
        val x = center.x + 200f * cos(Math.toRadians(angle.toDouble())).toFloat()
        val y = center.y + 200f * sin(Math.toRadians(angle.toDouble())).toFloat()
        drawCircle(
            color = Color(0xFF00F2FF).copy(alpha = 0.4f),
            radius = 4f,
            center = Offset(x, y)
        )
    }
}

@Composable
private fun WaveformAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val bars = 12
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(bars) { i ->
            val height by infiniteTransition.animateFloat(
                initialValue = 8f,
                targetValue = 32f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400 + i * 60,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF00F2FF).copy(alpha = 0.7f))
            )
        }
    }
}
