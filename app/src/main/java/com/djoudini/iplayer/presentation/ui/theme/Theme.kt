package com.djoudini.iplayer.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE53935),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB71C1C),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFF5252),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5C1A1A),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFFF8A80),
    onTertiary = Color.Black,
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFE8E0E0),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFE8E0E0),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFCAC4C4),
    error = Color(0xFFFF6659),
    outline = Color(0xFF5C5C5C),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC62828),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFFD32F2F),
    onSecondary = Color.White,
    background = Color(0xFFF5F0F0),
    onBackground = Color(0xFF1C1B1B),
    surface = Color(0xFFFFFBFB),
    onSurface = Color(0xFF1C1B1B),
    surfaceVariant = Color(0xFFEDE0E0),
    onSurfaceVariant = Color(0xFF534343),
    error = Color(0xFFBA1A1A),
    outline = Color(0xFF857373),
)

@Composable
fun DjoudinisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
