package com.example.modicanalyzer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import com.example.modicanalyzer.ui.theme.*

private val DarkColorScheme = darkColorScheme(
    primary = ModicarePrimaryVariant,
    secondary = ModicareSecondary,
    tertiary = ModicareAccent,
    background = ModicareBackground,
    surface = ModicareSurface,
    onPrimary = TextPrimary,
    onSecondary = TextSecondary,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = ModicarePrimary,
    secondary = ModicareSecondary,
    tertiary = ModicareAccent,
    background = ModicareBackground,
    surface = ModicareSurface,
    onPrimary = TextPrimary,
    onSecondary = TextSecondary,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun ModicAnalyzerTheme(
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}