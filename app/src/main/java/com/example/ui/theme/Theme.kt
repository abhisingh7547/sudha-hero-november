package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkHeroCrimson,
    onPrimary = Color(0xFF410002),
    primaryContainer = DarkHeroCrimsonContainer,
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFF94A3B8),
    onSecondary = DarkSurface,
    secondaryContainer = DarkCardElevated,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = AmberAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF451A03),
    onTertiaryContainer = AmberContainer,
    background = DarkSurface,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLow = DarkCard,
    surfaceContainer = DarkCardElevated,
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = HeroCrimson,
    onPrimary = Color.White,
    primaryContainer = HeroCrimsonContainer,
    onPrimaryContainer = HeroOnCrimsonContainer,
    secondary = SlateCharcoal,
    onSecondary = Color.White,
    secondaryContainer = SlateSurfaceVariant,
    onSecondaryContainer = SlateInk,
    tertiary = AmberAccent,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = SlateSurface,
    onBackground = SlateInk,
    surface = SlateSurface,
    onSurface = SlateInk,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF1F5F9),
    outline = Color(0xFFCBD5E1),
    outlineVariant = SlateBorder,
  )

private val AppShapes =
  Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun SudhaHeroTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = AppShapes,
    content = content,
  )
}
