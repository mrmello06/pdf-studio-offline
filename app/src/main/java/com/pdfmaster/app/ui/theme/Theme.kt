package com.pdfmaster.app.ui.theme

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
    primary = IndigoLight,
    onPrimary = DarkBackground,
    primaryContainer = IndigoDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = SkySecondary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = LightSurface,
    primaryContainer = IndigoLight.copy(alpha = 0.2f),
    onPrimaryContainer = IndigoDark,
    secondary = SkySecondary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightCard,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    outline = LightBorder
)

@Composable
fun PDFMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext
            dynamicDarkColorScheme(context.current)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
