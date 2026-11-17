package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val McDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianVoid,
    primaryContainer = NeonCyanDim,
    onPrimaryContainer = NeonCyan,
    secondary = NeonEmerald,
    onSecondary = ObsidianVoid,
    secondaryContainer = NeonEmeraldDim,
    onSecondaryContainer = NeonEmerald,
    tertiary = NeonViolet,
    onTertiary = TextPrimary,
    tertiaryContainer = NeonVioletDim,
    onTertiaryContainer = NeonViolet,
    error = NeonCoral,
    onError = TextPrimary,
    errorContainer = NeonCoralDim,
    onErrorContainer = NeonCoral,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianVoid,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianCard,
    surfaceContainerHigh = ObsidianElevated,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorder.copy(alpha = 0.5f)
)

private val McShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun McAssistantTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = McDarkColorScheme,
        typography = Typography,
        shapes = McShapes,
        content = content
    )
}
