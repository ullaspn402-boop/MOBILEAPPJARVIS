package com.aistudio.jarvis.voiceagent.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.jarvis.voiceagent.model.JarvisState
import com.aistudio.jarvis.voiceagent.ui.theme.CyanGlow
import com.aistudio.jarvis.voiceagent.ui.theme.ElectricBlue
import com.aistudio.jarvis.voiceagent.ui.theme.NeonBlue
import com.aistudio.jarvis.voiceagent.ui.theme.StatusCompleted
import com.aistudio.jarvis.voiceagent.ui.theme.StatusError
import com.aistudio.jarvis.voiceagent.ui.theme.StatusExecuting
import com.aistudio.jarvis.voiceagent.ui.theme.StatusListening
import com.aistudio.jarvis.voiceagent.ui.theme.StatusReady
import com.aistudio.jarvis.voiceagent.ui.theme.StatusThinking
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FuturisticOrb(
    state: JarvisState,
    audioRms: Float = 0f,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    // Rotation animations
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation1"
    )

    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation2"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // State based primary & secondary accent colors
    val primaryColor by animateColorAsState(
        targetValue = when (state) {
            JarvisState.READY -> StatusReady
            JarvisState.LISTENING -> StatusListening
            JarvisState.THINKING -> StatusThinking
            JarvisState.CONFIRMATION_REQUIRED -> StatusExecuting
            JarvisState.EXECUTING -> StatusExecuting
            JarvisState.COMPLETED -> StatusCompleted
            JarvisState.ERROR -> StatusError
        },
        animationSpec = tween(400),
        label = "primary_color"
    )

    val secondaryColor by animateColorAsState(
        targetValue = when (state) {
            JarvisState.READY -> NeonBlue
            JarvisState.LISTENING -> CyanGlow
            JarvisState.THINKING -> ElectricBlue
            JarvisState.CONFIRMATION_REQUIRED -> Color(0xFFFFB703)
            JarvisState.EXECUTING -> Color(0xFFFB8500)
            JarvisState.COMPLETED -> CyanGlow
            JarvisState.ERROR -> Color(0xFFFF0055)
        },
        animationSpec = tween(400),
        label = "secondary_color"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("futuristic_orb")
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.75f

            // Factor in live audio RMS for pulsing
            val dynamicScale = if (state == JarvisState.LISTENING) {
                breathingScale * (1f + (audioRms * 0.45f))
            } else {
                breathingScale
            }

            val radius = baseRadius * dynamicScale

            // 1. Outer ambient glow ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        secondaryColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.5f
                ),
                radius = radius * 1.4f,
                center = center
            )

            // 2. Outer Technical HUD Ticks Ring (Rotating Clockwise)
            val numTicks = 24
            val tickRadius = radius * 1.18f
            for (i in 0 until numTicks) {
                val angleDeg = (i * (360f / numTicks)) + rotation1
                val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                val isMajor = i % 4 == 0
                val tickLength = if (isMajor) 14f else 7f

                val startX = center.x + tickRadius * cos(angleRad)
                val startY = center.y + tickRadius * sin(angleRad)
                val endX = center.x + (tickRadius + tickLength) * cos(angleRad)
                val endY = center.y + (tickRadius + tickLength) * sin(angleRad)

                drawLine(
                    color = if (isMajor) primaryColor.copy(alpha = 0.9f) else secondaryColor.copy(alpha = 0.4f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajor) 3.5f else 1.8f,
                    cap = StrokeCap.Round
                )
            }

            // 3. Middle Dashed Orbital Ring (Rotating Counter-Clockwise)
            drawCircle(
                color = secondaryColor.copy(alpha = 0.6f),
                radius = radius * 0.95f,
                center = center,
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(25f, 15f), rotation2 * 2f)
                )
            )

            // 4. Arc Segments (HUD Brackets)
            drawArc(
                color = primaryColor,
                startAngle = rotation1,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.85f, center.y - radius * 0.85f),
                size = androidx.compose.ui.geometry.Size(radius * 1.7f, radius * 1.7f),
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )

            drawArc(
                color = primaryColor,
                startAngle = rotation1 + 180f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.85f, center.y - radius * 0.85f),
                size = androidx.compose.ui.geometry.Size(radius * 1.7f, radius * 1.7f),
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )

            // 5. Glowing Central Core Reactor
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryColor.copy(alpha = 0.85f),
                        secondaryColor.copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.6f
                ),
                radius = radius * 0.6f,
                center = center
            )

            // 6. Core Inner Tech Hex/Triangle Rings
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = radius * 0.22f,
                center = center,
                style = Stroke(width = 3f)
            )

            // Core center dot
            drawCircle(
                color = Color.White,
                radius = 6f,
                center = center
            )
        }
    }
}
