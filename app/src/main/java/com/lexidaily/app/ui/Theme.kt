package com.lexidaily.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class SemanticColors(val reward: Color, val mastered: Color, val learning: Color,
    val danger: Color, val hero: Color, val onHero: Color, val onReward: Color)
private val LocalSemanticColors = staticCompositionLocalOf {
    SemanticColors(Color(0xFFF5A742), Color(0xFF16796C), Color(0xFF5B5CE2), Color(0xFFE75A64), Color(0xFF26265B), Color.White, Color(0xFF553100))
}
object AppColors {
    val primary @Composable get() = MaterialTheme.colorScheme.primary
    val reward @Composable get() = LocalSemanticColors.current.reward
    val mastered @Composable get() = LocalSemanticColors.current.mastered
    val learning @Composable get() = LocalSemanticColors.current.learning
    val danger @Composable get() = LocalSemanticColors.current.danger
    val surfaceElevated @Composable get() = MaterialTheme.colorScheme.surfaceContainer
    val hero @Composable get() = LocalSemanticColors.current.hero
    val onHero @Composable get() = LocalSemanticColors.current.onHero
    val onReward @Composable get() = LocalSemanticColors.current.onReward
}
object Spacing {
    val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp
    val screen = 20.dp; val section = 24.dp; val xl = 32.dp; val xxl = 48.dp
}
private val Light = lightColorScheme(
    primary = Color(0xFF5B5CE2), onPrimary = Color.White, primaryContainer = Color(0xFFE9E8FF), onPrimaryContainer = Color(0xFF3536A8),
    secondary = Color(0xFF16796C), secondaryContainer = Color(0xFFD5F4EB), onSecondaryContainer = Color(0xFF075349),
    background = Color(0xFFF7F8FC), onBackground = Color(0xFF171926),
    surface = Color.White, onSurface = Color(0xFF171926), surfaceContainer = Color(0xFFEEEFF8),
    surfaceContainerLow = Color(0xFFF2F3FA), onSurfaceVariant = Color(0xFF5C6075),
    outline = Color(0xFF75798E), outlineVariant = Color(0xFFDCE0EE), error = Color(0xFFB3263D)
)
private val Dark = darkColorScheme(
    primary = Color(0xFFBEBEFF), onPrimary = Color(0xFF252674), primaryContainer = Color(0xFF35366B), onPrimaryContainer = Color(0xFFE5E5FF),
    secondary = Color(0xFF6CD7C3), secondaryContainer = Color(0xFF124B43), onSecondaryContainer = Color(0xFFB5F4E8),
    background = Color(0xFF101117), onBackground = Color(0xFFF4F5F8),
    surface = Color(0xFF181A22), onSurface = Color(0xFFF4F5F8), surfaceContainer = Color(0xFF20232D),
    surfaceContainerLow = Color(0xFF1C1E28), onSurfaceVariant = Color(0xFFBDC1D3),
    outline = Color(0xFF9298AC), outlineVariant = Color(0xFF373C4B), error = Color(0xFFFFB1BA)
)
@Composable fun LexiTheme(mode: String = "SYSTEM", content: @Composable () -> Unit) {
    val dark = mode == "DARK" || mode == "SYSTEM" && isSystemInDarkTheme()
    val semantic = if (dark) SemanticColors(Color(0xFFFFC36B), Color(0xFF6CD7C3), Color(0xFFBEBEFF), Color(0xFFFFB1BA), Color(0xFF242550), Color(0xFFF4F5FF), Color(0xFFFFD493)) else LocalSemanticColors.current
    CompositionLocalProvider(LocalSemanticColors provides semantic) {
        MaterialTheme(colorScheme = if (dark) Dark else Light,
            typography = Typography(), shapes = Shapes(small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(14.dp),
                large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp)), content = content)
    }
}
