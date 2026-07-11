package com.cooknivo.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material 3 system typography, tuned for recipe titles, category labels,
 * ingredient rows, and numbered steps. No custom font files are bundled — the
 * handmade feel comes from layout and card styling, not a font dependency.
 */
val CooknivoTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
        ),
        headlineSmall = headlineSmall.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
        ),
        titleLarge = titleLarge.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
        ),
        labelMedium = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp,
        ),
    )
}
