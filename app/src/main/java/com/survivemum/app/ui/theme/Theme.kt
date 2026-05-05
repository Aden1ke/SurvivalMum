package com.survivemum.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

//  LIGHT THEME — default for SurviveMum 
private val SurviveMumLightColors = lightColorScheme(
    primary = SurviveMumRed,
    onPrimary = SurviveMumWhite,
    primaryContainer = AlertHighBg,
    onPrimaryContainer = SurviveMumDark,

    secondary = MotherPrimary,
    onSecondary = SurviveMumWhite,
    secondaryContainer = MotherLight,
    onSecondaryContainer = MotherAccent,

    tertiary = NewbornPrimary,
    onTertiary = SurviveMumWhite,
    tertiaryContainer = NewbornLight,
    onTertiaryContainer = NewbornAccent,

    background = BackgroundPrimary,
    onBackground = TextPrimary,

    surface = SurfaceColor,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSecondary,
    onSurfaceVariant = TextSecondary,

    outline = DividerColor,
    outlineVariant = BackgroundSecondary,

    error = AlertHigh,
    onError = SurviveMumWhite,
    errorContainer = AlertHighBg,
    onErrorContainer = AlertCritical,
)

//  DARK THEME 
private val SurviveMumDarkColors = darkColorScheme(
    primary = SurviveMumRed,
    onPrimary = SurviveMumWhite,
    primaryContainer = AlertCritical,
    onPrimaryContainer = SurviveMumWhite,

    secondary = MotherPrimary,
    onSecondary = SurviveMumWhite,
    secondaryContainer = MotherAccent,
    onSecondaryContainer = MotherLight,

    tertiary = NewbornPrimary,
    onTertiary = SurviveMumWhite,
    tertiaryContainer = NewbornAccent,
    onTertiaryContainer = NewbornLight,

    background = SurviveMumDark,
    onBackground = SurviveMumWhite,

    surface = Color(0xFF1A1A2E),
    onSurface = SurviveMumWhite,
    surfaceVariant = Color(0xFF2C2C4A),
    onSurfaceVariant = TextSecondary,

    outline = Color(0xFF444466),
    outlineVariant = Color(0xFF2C2C4A),

    error = AlertHigh,
    onError = SurviveMumWhite,
    errorContainer = AlertCritical,
    onErrorContainer = SurviveMumWhite,
)

@Composable
fun SurvivalMumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Disabled dynamic color intentionally
    // SurviveMum must always look like SurviveMum
    // Dynamic color would replace our brand with phone wallpaper colors
    val colorScheme = if (darkTheme) {
        SurviveMumDarkColors
    } else {
        SurviveMumLightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SurviveMumTypography,
        content = content
    )
}
