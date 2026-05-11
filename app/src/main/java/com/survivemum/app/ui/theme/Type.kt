package com.survivemum.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

<<<<<<< fs-screens-database
val SurviveMumTypography = Typography(

    // Large titles — screen headers
=======
/**
 * SurviveMum Typography System
 * Optimized for readability in medical/emergency contexts.
 */
val Typography = Typography(
    // Headlines: High visibility for screen titles
>>>>>>> master
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),

    // Screen titles
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
<<<<<<< fs-screens-database
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),

    // Card titles and section headers
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),

    // Alert type labels — large, must be readable fast
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),

    // Patient name, section labels
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
=======
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),

    // Titles: For card headers and section titles
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),

    // Body: High readability for clinical notes and instructions
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
>>>>>>> master
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),

    // Form labels, card subtitles
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // Body text — clinical notes, descriptions
    // LARGER than default — TBAs must read in poor light
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp
    ),
<<<<<<< fs-screens-database

    // Secondary body — thinking traces, metadata
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),

    // Small details — timestamps, source citations
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp
    ),

    // Button text
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),

    // Badge labels — risk level, severity
    labelMedium = TextStyle(
=======
    bodyMedium = TextStyle(
>>>>>>> master
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),

    // Labels: For small metadata and vital sign units
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),

    // Tiny labels — offline status, captions
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
<<<<<<< fs-screens-database
=======

// Custom Extension for Emergency Alerts
val EmergencyAlertStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Black,
    fontSize = 20.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.5.sp
)
>>>>>>> master
