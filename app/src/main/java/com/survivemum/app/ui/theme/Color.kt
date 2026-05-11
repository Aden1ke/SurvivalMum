package com.survivemum.app.ui.theme

import androidx.compose.ui.graphics.Color

<<<<<<< fs-screens-database
//  SURVIVEMUM BRAND 
val SurviveMumRed = Color(0xFFC0392B)        // Primary — alerts, danger, brand
val SurviveMumDark = Color(0xFF1A1A2E)       // Dark navy — headers, text
val SurviveMumWhite = Color(0xFFFDFEFE)      // Background

//  LAYER COLORS — Mother, Newborn, Toddler 

// Mother — warm blue. Calm, trustworthy, clinical
val MotherPrimary = Color(0xFF2471A3)
val MotherLight = Color(0xFFEAF4FB)
val MotherAccent = Color(0xFF1A5276)

// Newborn — soft green. New life, gentle, hopeful
val NewbornPrimary = Color(0xFF1E8449)
val NewbornLight = Color(0xFFEAFAF1)
val NewbornAccent = Color(0xFF196F3D)

// Toddler — warm purple. Growth, development, curiosity
val ToddlerPrimary = Color(0xFF6C3483)
val ToddlerLight = Color(0xFFF5EEF8)
val ToddlerAccent = Color(0xFF5B2C6F)

//  ALERT SEVERITY 
val AlertCritical = Color(0xFF922B21)
val AlertCriticalBg = Color(0xFFFADBD8)
val AlertHigh = Color(0xFFC0392B)
val AlertHighBg = Color(0xFFF9EBEA)
val AlertMedium = Color(0xFFB7770D)
val AlertMediumBg = Color(0xFFFEF9E7)
val AlertLow = Color(0xFF1E8449)
val AlertLowBg = Color(0xFFEAFAF1)

//  RISK LEVEL 
val RiskLow = Color(0xFF1E8449)
val RiskMedium = Color(0xFFB7770D)
val RiskHigh = Color(0xFFC0392B)
val RiskCritical = Color(0xFF922B21)

//  UI NEUTRALS 
val TextPrimary = Color(0xFF1A1A2E)
val TextSecondary = Color(0xFF717D7E)
val TextDisabled = Color(0xFFBDC3C7)
val BackgroundPrimary = Color(0xFFFDFEFE)
val BackgroundSecondary = Color(0xFFF2F3F4)
val DividerColor = Color(0xFFDDDDDD)
val SurfaceColor = Color(0xFFFFFFFF)
val CardBackground = Color(0xFFF8F9FA)

//  OFFLINE / ONLINE STATUS 
val OfflineGreen = Color(0xFF1E8449)   // Green = offline is GOOD for SurviveMum
val OnlineAmber = Color(0xFFB7770D)    // Amber = connected, SMS enabled

//  THINKING TRACE PANEL 
val ThinkingTraceBg = Color(0xFF1A1A2E)
val ThinkingTraceText = Color(0xFFEAFAF1)
val ThinkingTraceAccent = Color(0xFF1E8449)

//  BUTTON COLORS 
val ButtonPrimary = Color(0xFFC0392B)
val ButtonPrimaryText = Color(0xFFFFFFFF)
val ButtonSecondary = Color(0xFF1A1A2E)
val ButtonSecondaryText = Color(0xFFFFFFFF)
val ButtonDisabled = Color(0xFFBDC3C7)
=======
/**
 * SurviveMum Design System: Medical-Professional Palette
 * Optimized for high contrast, calm aesthetics, and hospital environment.
 */

// --- Primary: Calm Medical Purple/Blue ---
val MedicalPrimaryLight = Color(0xFF6750A4)
val MedicalOnPrimaryLight = Color(0xFFFFFFFF)
val MedicalPrimaryContainerLight = Color(0xFFEADDFF)
val MedicalOnPrimaryContainerLight = Color(0xFF21005D)

val MedicalPrimaryDark = Color(0xFFD0BCFF)
val MedicalOnPrimaryDark = Color(0xFF381E72)
val MedicalPrimaryContainerDark = Color(0xFF4F378B)
val MedicalOnPrimaryContainerDark = Color(0xFFEADDFF)

// --- Secondary: Soft Professional Grey/Teal ---
val MedicalSecondaryLight = Color(0xFF625B71)
val MedicalOnSecondaryLight = Color(0xFFFFFFFF)
val MedicalSecondaryContainerLight = Color(0xFFE8DEF8)
val MedicalOnSecondaryContainerLight = Color(0xFF1D192B)

val MedicalSecondaryDark = Color(0xFFCCC2DC)
val MedicalOnSecondaryDark = Color(0xFF332D41)
val MedicalSecondaryContainerDark = Color(0xFF4A4458)
val MedicalOnSecondaryContainerDark = Color(0xFFE8DEF8)

// --- Tertiary: Maternal Soft Pink ---
val MedicalTertiaryLight = Color(0xFF7D5260)
val MedicalOnTertiaryLight = Color(0xFFFFFFFF)
val MedicalTertiaryContainerLight = Color(0xFFFFD8E4)
val MedicalOnTertiaryContainerLight = Color(0xFF31111D)

val MedicalTertiaryDark = Color(0xFFEFB8C8)
val MedicalOnTertiaryDark = Color(0xFF492532)
val MedicalTertiaryContainerDark = Color(0xFF633B48)
val MedicalOnTertiaryContainerDark = Color(0xFFFFD8E4)

// --- Neutral / Background: High Contrast & Clean ---
val MedicalBackgroundLight = Color(0xFFFEF7FF)
val MedicalOnBackgroundLight = Color(0xFF1D1B20)
val MedicalSurfaceLight = Color(0xFFFFFFFF)
val MedicalOnSurfaceLight = Color(0xFF1D1B20)
val MedicalSurfaceVariantLight = Color(0xFFE7E0EB)
val MedicalOnSurfaceVariantLight = Color(0xFF49454F)

val MedicalBackgroundDark = Color(0xFF000000) // Pure Black for OLED/Contrast
val MedicalOnBackgroundDark = Color(0xFFE6E1E5)
val MedicalSurfaceDark = Color(0xFF1C1B1F)
val MedicalOnSurfaceDark = Color(0xFFE6E1E5)
val MedicalSurfaceVariantDark = Color(0xFF49454F)
val MedicalOnSurfaceVariantDark = Color(0xFFCAC4D0)

// --- Alert / Emergency: High Visibility ---
val MedicalErrorLight = Color(0xFFB3261E)
val MedicalOnErrorLight = Color(0xFFFFFFFF)
val MedicalErrorContainerLight = Color(0xFFF9DEDC)
val MedicalOnErrorContainerLight = Color(0xFF410E0B)

val MedicalErrorDark = Color(0xFFF2B8B5)
val MedicalOnErrorDark = Color(0xFF601410)
val MedicalErrorContainerDark = Color(0xFF8C1D18)
val MedicalOnErrorContainerDark = Color(0xFFF9DEDC)

// --- Functional Accents ---
val SuccessGreen = Color(0xFF2E7D32)
val InfoBlue = Color(0xFF0288D1)
val WarningAmber = Color(0xFFFFA000)
>>>>>>> master
