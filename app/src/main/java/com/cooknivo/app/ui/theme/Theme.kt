package com.cooknivo.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Cooknivo uses a single warm light scheme intentionally (a physical recipe box
 * has one look). We keep it stable across system dark mode to preserve the
 * paper/wood identity while maintaining readable contrast.
 */
private val CooknivoColorScheme = lightColorScheme(
    primary = RecipeTerracotta,
    onPrimary = PaperWhite,
    primaryContainer = DeepTerracotta,
    onPrimaryContainer = PaperWhite,
    secondary = WarmWood,
    onSecondary = PaperWhite,
    secondaryContainer = DividerPaper,
    onSecondaryContainer = DeepText,
    tertiary = BreakfastHoney,
    onTertiary = DeepText,
    background = AppBackground,
    onBackground = DeepText,
    surface = SurfaceWhite,
    onSurface = DeepText,
    surfaceVariant = CardCream,
    onSurfaceVariant = SecondaryText,
    outline = DividerColor,
    error = ErrorRed,
    onError = PaperWhite,
)

@Composable
fun CooknivoTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = CooknivoColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            val lightBars = colorScheme.background.luminance() > 0.5f
            controller.isAppearanceLightStatusBars = lightBars
            controller.isAppearanceLightNavigationBars = lightBars
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = CooknivoTypography,
        content = content,
    )
}
