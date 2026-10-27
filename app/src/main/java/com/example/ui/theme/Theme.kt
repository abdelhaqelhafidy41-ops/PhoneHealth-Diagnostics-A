package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DiagnosticColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = CyanOnAccent,
    primaryContainer = DiagnosticSurfaceVariant,
    onPrimaryContainer = CyanAccent,
    secondary = GreenSuccess,
    onSecondary = GreenOnSuccess,
    secondaryContainer = DiagnosticSurfaceVariant,
    onSecondaryContainer = GreenSuccess,
    tertiary = VioletTertiary,
    background = DiagnosticDarkBg,
    onBackground = TextPrimary,
    surface = DiagnosticSurface,
    onSurface = TextPrimary,
    surfaceVariant = DiagnosticSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DiagnosticCardBorder,
    error = RedDanger,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Hardware diagnostic tools excel in dark tech mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DiagnosticColorScheme,
        typography = Typography,
        content = content
    )
}
