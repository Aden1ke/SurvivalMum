package com.survivemum.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
)

@Composable
fun SurvivalMumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}