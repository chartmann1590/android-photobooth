package com.example.photobooth.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// — Color palette —
val Rose = Color(0xFFE91E63)
val RoseLight = Color(0xFFFF5C8D)
val RoseDark = Color(0xFFAD1457)
val Gold = Color(0xFFFFD54F)
val GoldDark = Color(0xFFF9A825)
val Champagne = Color(0xFFFFF8E1)
val DarkSurface = Color(0xFF1A1A2E)
val DarkBackground = Color(0xFF0F0F1A)
val CardSurface = Color(0xFF252540)
val CardSurfaceLight = Color(0xFF2E2E4A)
val TextPrimary = Color(0xFFF5F5F5)
val TextSecondary = Color(0xFFB0B0C8)
val Success = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFEF5350)

private val PhotoboothColorScheme = darkColorScheme(
    primary = Rose,
    onPrimary = Color.White,
    primaryContainer = RoseDark,
    onPrimaryContainer = Color.White,
    secondary = Gold,
    onSecondary = Color.Black,
    secondaryContainer = GoldDark,
    onSecondaryContainer = Color.Black,
    tertiary = Champagne,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White,
    outline = Color(0xFF4A4A6A),
)

@Composable
fun PhotoboothTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = PhotoboothColorScheme,
        typography = PhotoboothTypography,
        content = content,
    )
}
