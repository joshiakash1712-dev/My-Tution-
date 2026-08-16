package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.AppRepository
import com.example.data.DarkThemeMode

private val DarkColorScheme =
  darkColorScheme(
    primary = GeoPrimaryContainer,
    onPrimary = GeoOnPrimaryContainer,
    primaryContainer = GeoPrimary,
    onPrimaryContainer = Color.White,
    secondary = GeoSecondaryContainer,
    onSecondary = GeoOnSecondaryContainer,
    tertiary = GeoTertiaryContainer,
    onTertiary = GeoOnTertiaryContainer,
    background = Color(0xFF111827), // Deep slate dark backdrop
    onBackground = Color(0xFFF9FAFB),
    surface = Color(0xFF1F2937), // Lighter slate surface for cards and panels
    onSurface = Color(0xFFF9FAFB),
    surfaceVariant = Color(0xFF374151),
    onSurfaceVariant = Color(0xFFD1D5DB),
    outline = Color(0xFF4B5563),
    outlineVariant = Color(0xFF374151)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GeoPrimary,
    onPrimary = GeoOnPrimary,
    primaryContainer = GeoPrimaryContainer,
    onPrimaryContainer = GeoOnPrimaryContainer,
    secondary = GeoSecondary,
    onSecondary = GeoOnSecondary,
    secondaryContainer = GeoSecondaryContainer,
    onSecondaryContainer = GeoOnSecondaryContainer,
    tertiary = GeoTertiary,
    onTertiary = GeoOnTertiary,
    tertiaryContainer = GeoTertiaryContainer,
    onTertiaryContainer = GeoOnTertiaryContainer,
    background = GeoBackground,
    onBackground = GeoOnBackground,
    surface = GeoSurface,
    onSurface = GeoOnSurface,
    surfaceVariant = GeoSurfaceVariant,
    onSurfaceVariant = GeoOnSurfaceVariant,
    outline = GeoOutline,
    outlineVariant = GeoOutlineVariant
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean? = null,
  // Set dynamicColor to false by default to strictly enforce Geometric Balance aesthetic
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val darkThemeModeState by AppRepository.darkThemeMode.collectAsState()

  val isDark = when {
    darkTheme != null -> darkTheme
    else -> when (darkThemeModeState) {
      DarkThemeMode.LIGHT -> false
      DarkThemeMode.DARK -> true
      DarkThemeMode.SYSTEM -> systemDark
    }
  }

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      isDark -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

