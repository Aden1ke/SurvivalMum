package com.survivemum.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
<<<<<<< fs-screens-database
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
=======
import androidx.compose.material3.*
>>>>>>> master
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

<<<<<<< fs-screens-database
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
=======
private val LightColorScheme = lightColorScheme(
    primary = MedicalPrimaryLight,
    onPrimary = MedicalOnPrimaryLight,
    primaryContainer = MedicalPrimaryContainerLight,
    onPrimaryContainer = MedicalOnPrimaryContainerLight,
    secondary = MedicalSecondaryLight,
    onSecondary = MedicalOnSecondaryLight,
    secondaryContainer = MedicalSecondaryContainerLight,
    onSecondaryContainer = MedicalOnSecondaryContainerLight,
    tertiary = MedicalTertiaryLight,
    onTertiary = MedicalOnTertiaryLight,
    tertiaryContainer = MedicalTertiaryContainerLight,
    onTertiaryContainer = MedicalOnTertiaryContainerLight,
    background = MedicalBackgroundLight,
    onBackground = MedicalOnBackgroundLight,
    surface = MedicalSurfaceLight,
    onSurface = MedicalOnSurfaceLight,
    surfaceVariant = MedicalSurfaceVariantLight,
    onSurfaceVariant = MedicalOnSurfaceVariantLight,
    error = MedicalErrorLight,
    onError = MedicalOnErrorLight,
    errorContainer = MedicalErrorContainerLight,
    onErrorContainer = MedicalOnErrorContainerLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = MedicalPrimaryDark,
    onPrimary = MedicalOnPrimaryDark,
    primaryContainer = MedicalPrimaryContainerDark,
    onPrimaryContainer = MedicalOnPrimaryContainerDark,
    secondary = MedicalSecondaryDark,
    onSecondary = MedicalOnSecondaryDark,
    secondaryContainer = MedicalSecondaryContainerDark,
    onSecondaryContainer = MedicalOnSecondaryContainerDark,
    tertiary = MedicalTertiaryDark,
    onTertiary = MedicalOnTertiaryDark,
    tertiaryContainer = MedicalTertiaryContainerDark,
    onTertiaryContainer = MedicalOnTertiaryContainerDark,
    background = MedicalBackgroundDark, // Pure Black
    onBackground = MedicalOnBackgroundDark,
    surface = MedicalSurfaceDark,
    onSurface = MedicalOnSurfaceDark,
    surfaceVariant = MedicalSurfaceVariantDark,
    onSurfaceVariant = MedicalOnSurfaceVariantDark,
    error = MedicalErrorDark,
    onError = MedicalOnErrorDark,
    errorContainer = MedicalErrorContainerDark,
    onErrorContainer = MedicalOnErrorContainerDark,
>>>>>>> master
)

@Composable
fun SurvivalMumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
<<<<<<< fs-screens-database
    // Disabled dynamic color intentionally
    // SurviveMum must always look like SurviveMum
    // Dynamic color would replace our brand with phone wallpaper colors
    val colorScheme = if (darkTheme) {
        SurviveMumDarkColors
    } else {
        SurviveMumLightColors
    }

=======
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
>>>>>>> master
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
<<<<<<< fs-screens-database
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
=======
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
>>>>>>> master
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SurviveMumTypography,
        content = content
    )
}
