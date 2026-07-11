package com.cooknivo.app.data

import com.cooknivo.app.model.RecipeCategory

/**
 * Neutral default categories. IDs are STABLE so re-initialization never creates
 * duplicates across relaunches. No health-based categories are included.
 */
object DefaultData {

    data class Seed(val id: String, val name: String, val colorKey: String)

    val seeds: List<Seed> = listOf(
        Seed("default_breakfast", "Breakfast", "breakfast"),
        Seed("default_main_dishes", "Main Dishes", "main"),
        Seed("default_side_dishes", "Side Dishes", "main"),
        Seed("default_soups", "Soups", "soup"),
        Seed("default_salads", "Salads", "salad"),
        Seed("default_baking", "Baking", "baking"),
        Seed("default_desserts", "Desserts", "dessert"),
        Seed("default_snacks", "Snacks", "other"),
        Seed("default_drinks", "Drinks", "drink"),
        Seed("default_sauces", "Sauces", "soup"),
        Seed("default_other", "Other", "other"),
    )

    val defaultCategoryIds: Set<String> = seeds.map { it.id }.toSet()

    fun buildDefaultCategories(nowIso: String): List<RecipeCategory> =
        seeds.mapIndexed { index, seed ->
            RecipeCategory(
                id = seed.id,
                name = seed.name,
                colorKey = seed.colorKey,
                sortOrder = index,
                isDefault = true,
                hiddenWhenEmpty = false,
                archived = false,
                createdAt = nowIso,
                updatedAt = nowIso,
            )
        }

    /** The first default category id used as an initial fallback selection. */
    const val FIRST_DEFAULT_CATEGORY_ID = "default_breakfast"
}
