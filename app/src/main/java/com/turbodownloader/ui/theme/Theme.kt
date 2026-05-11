package com.turbodownloader.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Yellow40,
    onPrimary = Color.Black,
    primaryContainer = Yellow90,
    onPrimaryContainer = Yellow10,
    secondary = Amber,
    onSecondary = Color.Black,
    secondaryContainer = Yellow80,
    onSecondaryContainer = Yellow10,
    tertiary = Orange40,
    onTertiary = Color.White,
    tertiaryContainer = Orange90,
    onTertiaryContainer = Orange10,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Grey99,
    onBackground = Grey10,
    surface = Grey99,
    onSurface = Grey10,
    surfaceVariant = Grey90,
    onSurfaceVariant = Grey30,
    outline = Grey40
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = Yellow40,
    onPrimary = Color.Black,
    primaryContainer = Yellow30,
    onPrimaryContainer = Yellow90,
    secondary = Yellow50,
    onSecondary = Yellow10,
    secondaryContainer = Yellow20,
    onSecondaryContainer = Yellow80,
    tertiary = Amber,
    onTertiary = Color.Black,
    tertiaryContainer = Orange30,
    onTertiaryContainer = Orange90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Color.Black,
    onBackground = Color(0xFFE6E1D5),
    surface = Color.Black,
    onSurface = Color(0xFFE6E1D5),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFCAC4B0),
    outline = Grey40
)

@Composable
fun TurboDownloaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AmoledDarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
