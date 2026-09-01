package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TelegramLightBlue,
    secondary = TelegramBlue,
    tertiary = GuestAmber,
    background = TelegramBackgroundDark,
    surface = TelegramSurfaceDark,
    surfaceVariant = Color(0xFF242F3D),
    primaryContainer = TelegramBubbleMeDark,
    secondaryContainer = TelegramBubbleOtherDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF7E8C9A)
)

private val LightColorScheme = lightColorScheme(
    primary = TelegramBlue,
    secondary = TelegramHeaderBlue,
    tertiary = GuestOrange,
    background = TelegramBackgroundLight,
    surface = TelegramSurfaceLight,
    surfaceVariant = Color(0xFFF1F1F1),
    primaryContainer = TelegramBubbleMeLight,
    secondaryContainer = TelegramBubbleOtherLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF222222),
    onSurface = Color(0xFF222222),
    onSurfaceVariant = Color(0xFF707579)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
