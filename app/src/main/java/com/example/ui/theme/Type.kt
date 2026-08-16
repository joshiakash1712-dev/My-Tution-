package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Relative Units Utility for Jetpack Compose.
 * Maps CSS-like relative typography sizing directly to Jetpack Compose TextUnit.
 * This ensures that when the base size is scaled or dynamic context is updated,
 * the entire application UI scales proportionally to prevent text clipping and truncation on small devices.
 */
object RelativeUnits {
  // Base font size (1rem) defaults to 14.sp (optimized for high-density, compact mobile screens)
  var baseFontSize: TextUnit = 14.sp

  fun updateBaseSize(newBase: TextUnit) {
    baseFontSize = newBase
  }
}

// Relative units: rem (relative to base root font size)
val Number.rem: TextUnit
  get() = (RelativeUnits.baseFontSize.value * this.toDouble()).sp

// Relative units: em (proportional context size)
val Number.em: TextUnit
  get() = (RelativeUnits.baseFontSize.value * this.toDouble()).sp


// Set of Material typography styles to start with
val Typography =
  Typography(
    displayLarge = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.Light,
      fontSize = 2.5.rem,
      lineHeight = 3.2.rem,
      letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.Normal,
      fontSize = 2.1.rem,
      lineHeight = 2.7.rem,
      letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.Normal,
      fontSize = 1.8.rem,
      lineHeight = 2.4.rem,
      letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.SemiBold,
      fontSize = 1.6.rem,
      lineHeight = 2.1.rem,
      letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.SemiBold,
      fontSize = 1.4.rem,
      lineHeight = 1.9.rem,
      letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.SemiBold,
      fontSize = 1.25.rem,
      lineHeight = 1.7.rem,
      letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.Bold,
      fontSize = 1.15.rem,
      lineHeight = 1.5.rem,
      letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.Medium,
      fontSize = 1.0.rem,
      lineHeight = 1.35.rem,
      letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.Medium,
      fontSize = 0.85.rem,
      lineHeight = 1.15.rem,
      letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.Normal,
      fontSize = 1.0.rem,
      lineHeight = 1.4.rem,
      letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.Normal,
      fontSize = 0.85.rem,
      lineHeight = 1.2.rem,
      letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.Normal,
      fontSize = 0.75.rem,
      lineHeight = 1.0.rem,
      letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.Medium,
      fontSize = 0.85.rem,
      lineHeight = 1.2.rem,
      letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.Medium,
      fontSize = 0.7.rem,
      lineHeight = 1.0.rem,
      letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.Medium,
      fontSize = 0.6.rem,
      lineHeight = 0.85.rem,
      letterSpacing = 0.5.sp
    )
  )

