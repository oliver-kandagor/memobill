package com.example.core.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.core.model.MemoThemeMode

@Immutable
data class MemoSemanticColors(
    val backgroundPrimary: Color,
    val surfaceCard: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderColor: Color,
    val accentPrimary: Color,
    val accentSubtle: Color,
    val isDark: Boolean
)

val DarkMemoSemanticColors = MemoSemanticColors(
    backgroundPrimary = MemoDarkBackground,
    surfaceCard = MemoDarkSurfaceCard,
    surfaceElevated = MemoDarkSurfaceElevated,
    textPrimary = MemoDarkTextPrimary,
    textSecondary = MemoDarkTextSecondary,
    textMuted = MemoDarkTextMuted,
    borderColor = MemoDarkBorder,
    accentPrimary = MemoDarkAccent,
    accentSubtle = MemoDarkAccentSubtle,
    isDark = true
)

val LightMemoSemanticColors = MemoSemanticColors(
    backgroundPrimary = MemoLightBackground,
    surfaceCard = MemoLightSurfaceCard,
    surfaceElevated = MemoLightSurfaceElevated,
    textPrimary = MemoLightTextPrimary,
    textSecondary = MemoLightTextSecondary,
    textMuted = MemoLightTextMuted,
    borderColor = MemoLightBorder,
    accentPrimary = MemoLightAccent,
    accentSubtle = MemoLightAccentSubtle,
    isDark = false
)

val LocalMemoColors = staticCompositionLocalOf { DarkMemoSemanticColors }

private val DarkColorScheme = darkColorScheme(
    primary = MemoDarkAccent,
    onPrimary = MemoDarkBackground,
    primaryContainer = MemoDarkSurfaceElevated,
    onPrimaryContainer = MemoDarkTextPrimary,
    secondary = MemoDarkTextSecondary,
    onSecondary = MemoDarkBackground,
    secondaryContainer = MemoDarkSurfaceElevated,
    onSecondaryContainer = MemoDarkTextPrimary,
    tertiary = MemoDarkTextPrimary,
    onTertiary = MemoDarkBackground,
    tertiaryContainer = MemoDarkSurfaceElevated,
    onTertiaryContainer = MemoDarkTextPrimary,
    background = MemoDarkBackground,
    onBackground = MemoDarkTextPrimary,
    surface = MemoDarkSurfaceCard,
    onSurface = MemoDarkTextPrimary,
    surfaceVariant = MemoDarkSurfaceElevated,
    onSurfaceVariant = MemoDarkTextSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = MemoLightSurfaceCard,
    inverseOnSurface = MemoLightTextPrimary,
    outline = MemoDarkBorder,
    outlineVariant = MemoDarkAccentSubtle,
    surfaceBright = MemoDarkSurfaceElevated,
    surfaceDim = MemoDarkBackground,
    surfaceContainerLowest = MemoDarkBackground,
    surfaceContainerLow = MemoDarkSurfaceCard,
    surfaceContainer = MemoDarkSurfaceCard,
    surfaceContainerHigh = MemoDarkSurfaceElevated,
    surfaceContainerHighest = MemoDarkAccentSubtle
)

private val LightColorScheme = lightColorScheme(
    primary = MemoLightAccent,
    onPrimary = MemoLightSurfaceCard,
    primaryContainer = MemoLightSurfaceElevated,
    onPrimaryContainer = MemoLightTextPrimary,
    secondary = MemoLightTextSecondary,
    onSecondary = MemoLightSurfaceCard,
    secondaryContainer = MemoLightSurfaceElevated,
    onSecondaryContainer = MemoLightTextPrimary,
    tertiary = MemoLightTextPrimary,
    onTertiary = MemoLightSurfaceCard,
    tertiaryContainer = MemoLightSurfaceElevated,
    onTertiaryContainer = MemoLightTextPrimary,
    background = MemoLightBackground,
    onBackground = MemoLightTextPrimary,
    surface = MemoLightSurfaceCard,
    onSurface = MemoLightTextPrimary,
    surfaceVariant = MemoLightSurfaceElevated,
    onSurfaceVariant = MemoLightTextSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = MemoDarkSurfaceCard,
    inverseOnSurface = MemoDarkTextPrimary,
    outline = MemoLightBorder,
    outlineVariant = MemoLightAccentSubtle,
    surfaceBright = MemoLightSurfaceCard,
    surfaceDim = MemoLightBackground,
    surfaceContainerLowest = MemoLightSurfaceCard,
    surfaceContainerLow = MemoLightBackground,
    surfaceContainer = MemoLightSurfaceCard,
    surfaceContainerHigh = MemoLightSurfaceElevated,
    surfaceContainerHighest = MemoLightAccentSubtle
)

val MemoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object MemoTheme {
    val colors: MemoSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMemoColors.current
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun MemoBillsTheme(
    themeMode: MemoThemeMode = MemoThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        MemoThemeMode.SYSTEM -> isSystemInDarkTheme()
        MemoThemeMode.DARK -> true
        MemoThemeMode.LIGHT -> false
    }
    val semanticColors = if (useDark) DarkMemoSemanticColors else LightMemoSemanticColors
    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !useDark
            insetsController.isAppearanceLightNavigationBars = !useDark
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalMemoColors provides semanticColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MemoBillsTypography,
            shapes = MemoShapes,
            content = content
        )
    }
}
