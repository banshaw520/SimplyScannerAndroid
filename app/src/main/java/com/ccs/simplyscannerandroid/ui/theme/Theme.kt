package com.ccs.simplyscannerandroid.ui.theme

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
    primary = primary_dark,
    onPrimary = onPrimary_dark,
    primaryContainer = primaryContainer_dark,
    onPrimaryContainer = onPrimaryContainer_dark,
    secondary = secondary_dark,
    onSecondary = onSecondary_dark,
    secondaryContainer = secondaryContainer_dark,
    onSecondaryContainer = onSecondaryContainer_dark,
    tertiary = tertiary_dark,
    onTertiary = onTertiary_dark,
    tertiaryContainer = tertiaryContainer_dark,
    onTertiaryContainer = onTertiaryContainer_dark,
    error = error_dark,
    onError = onError_dark,
    errorContainer = errorContainer_dark,
    onErrorContainer = onErrorContainer_dark,
    background = background_dark,
    onBackground = onBackground_dark,
    surface = surface_dark,
    onSurface = onSurface_dark,
    surfaceVariant = surfaceVariant_dark,
    onSurfaceVariant = onSurfaceVariant_dark,
    outline = outline_dark,
    outlineVariant = outlineVariant_dark,
    scrim = scrim_dark
)

private val LightColorScheme = lightColorScheme(
    primary = primary_light,
    onPrimary = onPrimary_light,
    primaryContainer = primaryContainer_light,
    onPrimaryContainer = onPrimaryContainer_light,
    secondary = secondary_light,
    onSecondary = onSecondary_light,
    secondaryContainer = secondaryContainer_light,
    onSecondaryContainer = onSecondaryContainer_light,
    tertiary = tertiary_light,
    onTertiary = onTertiary_light,
    tertiaryContainer = tertiaryContainer_light,
    onTertiaryContainer = onTertiaryContainer_light,
    error = error_light,
    onError = onError_light,
    errorContainer = errorContainer_light,
    onErrorContainer = onErrorContainer_light,
    background = background_light,
    onBackground = onBackground_light,
    surface = surface_light,
    onSurface = onSurface_light,
    surfaceVariant = surfaceVariant_light,
    onSurfaceVariant = onSurfaceVariant_light,
    outline = outline_light,
    outlineVariant = outlineVariant_light,
    scrim = scrim_light
)

@Composable
fun SimplyScannerAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}