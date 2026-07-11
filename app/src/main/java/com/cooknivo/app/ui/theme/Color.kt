package com.cooknivo.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Cooknivo palette: "Kitchen Index Card Box" ----

// Primary
val RecipeTerracotta = Color(0xFFB9684A)
val DeepTerracotta = Color(0xFF8E4C37)
val CardCream = Color(0xFFFFF8E9)
val PaperWhite = Color(0xFFFFFCF5)

// Box
val WarmWood = Color(0xFF9C704D)
val DeepWood = Color(0xFF6F4B34)
val BoxShadow = Color(0xFF4C392C)
val DividerPaper = Color(0xFFE8D8BD)

// Category tabs
val BreakfastHoney = Color(0xFFDDA342)
val MainDishSage = Color(0xFF768F68)
val SoupRust = Color(0xFFB76648)
val SaladGreen = Color(0xFF6F9A72)
val BakingSand = Color(0xFFC39A68)
val DessertRose = Color(0xFFBD7884)
val DrinkBlue = Color(0xFF688AA4)
val OtherGray = Color(0xFF7C8285)

// Neutral
val AppBackground = Color(0xFFF1EBE1)
val SurfaceWhite = Color(0xFFFFFFFF)
val DeepText = Color(0xFF2D2925)
val SecondaryText = Color(0xFF706A63)
val DividerColor = Color(0xFFD8CCBA)
val DisabledColor = Color(0xFFAAA39A)

// State
val FavoriteBookmark = Color(0xFFC9942F)
val ShoppingPending = Color(0xFFB77734)
val ShoppingChecked = Color(0xFF5F8368)
val ErrorRed = Color(0xFFB34C47)

/** Map a category colorKey to its tab color. Unknown keys fall back to gray. */
fun categoryColor(colorKey: String): Color = when (colorKey.lowercase()) {
    "breakfast" -> BreakfastHoney
    "main" -> MainDishSage
    "soup" -> SoupRust
    "salad" -> SaladGreen
    "baking" -> BakingSand
    "dessert" -> DessertRose
    "drink" -> DrinkBlue
    else -> OtherGray
}

/** Ordered options offered when creating/renaming a custom category. */
val categoryColorKeys: List<String> =
    listOf("breakfast", "main", "soup", "salad", "baking", "dessert", "drink", "other")
