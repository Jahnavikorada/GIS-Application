package com.fieldassets.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Green = Color(0xFF1B7F4B)
private val Blue = Color(0xFF2563EB)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF3E6),
    onPrimaryContainer = Color(0xFF0B3D23),
    secondary = Blue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0EAFF),
    onSecondaryContainer = Color(0xFF12307A),
    background = Color(0xFFF5F7F9),
    onBackground = Color(0xFF14181C),
    surface = Color.White,
    onSurface = Color(0xFF14181C),
    surfaceVariant = Color(0xFFEEF1F4),
    onSurfaceVariant = Color(0xFF55606B),
    outlineVariant = Color(0xFFDDE2E7),
    error = Color(0xFFBA1A1A),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object StatusColors {
    val Active = Color(0xFF1E8E4E)
    val ActiveContainer = Color(0xFFDDF3E6)
    val Inactive = Color(0xFFB45309)
    val InactiveContainer = Color(0xFFFDEBD3)
}

@Composable
fun FieldAssetsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, shapes = AppShapes, content = content)
}
