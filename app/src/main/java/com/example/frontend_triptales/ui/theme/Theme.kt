package com.example.frontend_triptales.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Orange200,
    onPrimary = DarkOrangeText,
    primaryContainer = Orange700,
    onPrimaryContainer = LightOrangeText,
    secondary = OrangeA200,
    onSecondary = DarkOrangeText,
    secondaryContainer = Orange500,
    onSecondaryContainer = LightOrangeText,
    tertiary = OrangeA400,
    onTertiary = DarkOrangeText,
    tertiaryContainer = Orange300,
    onTertiaryContainer = DarkOrangeText,
    background = Dark_Background,
    onBackground = Dark_OnBackground,
    surface = Dark_Surface,
    onSurface = Dark_OnSurface,
    surfaceVariant = Dark_SurfaceVariant,
    onSurfaceVariant = Dark_OnSurfaceVariant,
    error = ErrorColor,
    onError = Dark_OnError
)

private val LightColorScheme = lightColorScheme(
    primary = Orange500,
    onPrimary = LightOrangeText,
    primaryContainer = Orange100,
    onPrimaryContainer = DarkOrangeText,
    secondary = Orange700,
    onSecondary = LightOrangeText,
    secondaryContainer = OrangeA100,
    onSecondaryContainer = DarkOrangeText,
    tertiary = OrangeA700,
    onTertiary = LightOrangeText,
    tertiaryContainer = OrangeA200,
    onTertiaryContainer = DarkOrangeText,
    background = Light_Background,
    onBackground = Light_OnBackground,
    surface = Light_Surface,
    onSurface = Light_OnSurface,
    surfaceVariant = Light_SurfaceVariant,
    onSurfaceVariant = Light_OnSurfaceVariant,
    error = ErrorColor,
    onError = Light_OnError
)

@Composable
fun FrontendtriptalesTheme(
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