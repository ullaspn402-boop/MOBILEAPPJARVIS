package com.aistudio.jarvis.voiceagent.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val JarvisColorScheme = darkColorScheme(
    primary = CyanGlow,
    onPrimary = DeepNavyBg,
    primaryContainer = SurfaceDarkCard,
    onPrimaryContainer = CyanGlow,
    secondary = NeonBlue,
    onSecondary = DeepNavyBg,
    secondaryContainer = SurfaceBorder,
    onSecondaryContainer = TextPrimary,
    tertiary = StatusThinking,
    onTertiary = TextPrimary,
    background = DeepNavyBg,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDarkGlass,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorderGlow,
    error = StatusError,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            try {
                val window = (view.context as? Activity)?.window
                if (window != null) {
                    window.statusBarColor = DeepNavyBg.toArgb()
                    window.navigationBarColor = DeepNavyBg.toArgb()
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = false
                    controller.isAppearanceLightNavigationBars = false
                }
            } catch (_: Throwable) {
            }
        }
    }

    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
