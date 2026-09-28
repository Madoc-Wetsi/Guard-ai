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
    primary = SafetyBlueLight,
    onPrimary = ShieldNavy,
    primaryContainer = SafetyBluePrimary,
    onPrimaryContainer = Color.White,
    secondary = WarningAmberLight,
    onSecondary = ShieldNavy,
    secondaryContainer = Color(0xFF451A03),
    onSecondaryContainer = WarningAmberLight,
    tertiary = SafeGreenLight,
    onTertiary = ShieldNavy,
    error = AlertRedLight,
    onError = Color.White,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = AlertRedLight,
    background = SurfaceDark,
    onBackground = TextLightPrimary,
    surface = SurfaceContainerDark,
    onSurface = TextLightPrimary,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = TextLightSecondary,
    outline = Color(0xFF334155)
)

private val LightColorScheme = lightColorScheme(
    primary = SafetyBluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = WarningAmber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = SafeGreen,
    onTertiary = Color.White,
    error = AlertRed,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    background = SurfaceLight,
    onBackground = TextDarkPrimary,
    surface = SurfaceContainerLight,
    onSurface = TextDarkPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextDarkSecondary,
    outline = Color(0xFFCBD5E1)
)

@Composable
fun CampusGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep branded safety colors by default
    content: @Composable () -> Unit,
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
