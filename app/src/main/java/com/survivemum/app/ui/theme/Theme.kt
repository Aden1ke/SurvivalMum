package com.survivemum.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.Composable

/** 
 * Optimized for Low-Light Hospital Environments:
 * - Pure Black Background (OLED) to eliminate glare and maximize contrast.
 * - Vibrant Accents (Purple80) for high visibility.
 * - WCAG AAA compliant text-to-background ratios.
 */
private val HospitalDarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = DeepBlack,      // OLED Black
    surface = SurfaceDark,       // Subtle elevation
    onPrimary = Color.Black,     // Purple80 is light enough for black text
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,  // Max contrast
    onSurface = Color.White,
    error = CriticalRed,
    onError = Color.White
)

private val HospitalLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color.White,
    surface = Color(0xFFF2F2F7),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = DeepBlack,
    onSurface = DeepBlack,
    error = CriticalRed,
    onError = Color.White
)

@Composable
fun SurvivalMumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> HospitalDarkColorScheme
        else -> HospitalLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}