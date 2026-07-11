package com.cooknivo.app.model

import kotlinx.serialization.Serializable

/**
 * A personal recipe. All content is entered manually by the user.
 * Time values are nullable integer minutes; null means "not set".
 */
@Serializable
data class Recipe(
    val id: String,
    val name: String,
    val categoryId: String,
    val customCategoryName: String = "",
    val description: String = "",
    val preparationTimeMinutes: Int? = null,
    val cookingTimeMinutes: Int? = null,
    val restingTimeMinutes: Int? = null,
    val servingLabel: String = "",
    val favorite: Boolean = false,
    val archived: Boolean = false,
    val notes: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val lastOpenedAt: String = "",
)

/** An ordered ingredient belonging to a recipe. */
@Serializable
data class RecipeIngredient(
    val id: String,
    val recipeId: String,
    val name: String,
    val quantityLabel: String = "",
    val groupName: String = "",
    val shoppingCategory: ShoppingCategory = ShoppingCategory.Other,
    val addToShoppingByDefault: Boolean = true,
    val sortOrder: Int = 0,
    val note: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
)

/** An ordered preparation step belonging to a recipe. */
@Serializable
data class PreparationStep(
    val id: String,
    val recipeId: String,
    val stepNumber: Int = 1,
    val title: String = "",
    val instruction: String,
    val timerLabel: String = "",
    val note: String = "",
    val sortOrder: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
)

/** A recipe category (divider tab). Default categories may be hidden but not deleted. */
@Serializable
data class RecipeCategory(
    val id: String,
    val name: String,
    val colorKey: String = "other",
    val sortOrder: Int = 0,
    val isDefault: Boolean = false,
    val hiddenWhenEmpty: Boolean = false,
    val archived: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
)

/** A local shopping-list item. May be user-created or generated from an ingredient. */
@Serializable
data class ShoppingItem(
    val id: String,
    val title: String,
    val quantityLabel: String = "",
    val category: ShoppingCategory = ShoppingCategory.Other,
    val sourceRecipeId: String? = null,
    val sourceIngredientId: String? = null,
    val checked: Boolean = false,
    val note: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
)

/** User-adjustable application settings. Provides safe defaults for deserialization. */
@Serializable
data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val defaultCategoryId: String? = null,
    val cardDensity: RecipeCardDensity = RecipeCardDensity.Comfortable,
    val showRecentlyOpened: Boolean = true,
    val favoritesFirst: Boolean = false,
    val shoppingGenerationMode: ShoppingGenerationMode = ShoppingGenerationMode.PreviewFirst,
)

/**
 * Aggregate snapshot of all app data. Every list defaults to empty and settings
 * default to sane values, so partial/legacy JSON deserializes without crashing.
 */
@Serializable
data class AppData(
    val recipes: List<Recipe> = emptyList(),
    val ingredients: List<RecipeIngredient> = emptyList(),
    val preparationSteps: List<PreparationStep> = emptyList(),
    val categories: List<RecipeCategory> = emptyList(),
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val settings: AppSettings = AppSettings(),
)

/** Stable fallback labels used when a referenced record is missing. */
object Fallbacks {
    const val DELETED_RECIPE = "Deleted Recipe"
    const val UNCATEGORIZED = "Uncategorized"
    const val INGREDIENT_UNAVAILABLE = "Ingredient unavailable"
    const val STEP_UNAVAILABLE = "Step unavailable"
}
