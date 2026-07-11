package com.cooknivo.app.util

import com.cooknivo.app.model.RecipeIngredient
import com.cooknivo.app.model.ShoppingItem

/**
 * Represents one previewed shopping line derived from a user-entered ingredient.
 * [alreadyInList] indicates a case-insensitive title match with an existing item.
 */
data class ShoppingPreviewLine(
    val ingredientId: String,
    val title: String,
    val quantityLabel: String,
    val category: com.cooknivo.app.model.ShoppingCategory,
    val alreadyInList: Boolean,
)

/**
 * Shopping-list generation rules:
 *  - Uses ONLY user-entered ingredients; never invents ingredients.
 *  - Preserves quantity labels as plain text; no unit conversion or math.
 *  - Detects existing matches by case-insensitive trimmed title equality.
 *  - Prevents accidental duplicate generated lines within a single preview.
 */
object ShoppingGen {

    private fun key(s: String): String = s.trim().lowercase()

    /** Build a preview for the given ingredients against the current shopping list. */
    fun buildPreview(
        ingredients: List<RecipeIngredient>,
        existingItems: List<ShoppingItem>,
    ): List<ShoppingPreviewLine> {
        val existingKeys = existingItems.map { key(it.title) }.toSet()
        val seen = HashSet<String>()
        val result = ArrayList<ShoppingPreviewLine>()
        for (ing in ingredients) {
            val title = ing.name.trim()
            if (title.isEmpty()) continue
            val k = key(title)
            // Prevent duplicate generated lines for the same ingredient name in one preview.
            if (!seen.add(k)) continue
            result.add(
                ShoppingPreviewLine(
                    ingredientId = ing.id,
                    title = title,
                    quantityLabel = ing.quantityLabel.trim(),
                    category = ing.shoppingCategory,
                    alreadyInList = existingKeys.contains(k),
                )
            )
        }
        return result
    }

    /** The subset of preview lines that are NOT already present in the list. */
    fun missingOnly(preview: List<ShoppingPreviewLine>): List<ShoppingPreviewLine> =
        preview.filter { !it.alreadyInList }

    /**
     * Convert selected preview lines into new ShoppingItem records.
     * [allowDuplicates] = false skips lines already in the list.
     */
    fun toShoppingItems(
        lines: List<ShoppingPreviewLine>,
        sourceRecipeId: String,
        allowDuplicates: Boolean,
        nowIso: String,
        newId: () -> String,
    ): List<ShoppingItem> =
        lines
            .filter { allowDuplicates || !it.alreadyInList }
            .map { line ->
                ShoppingItem(
                    id = newId(),
                    title = line.title,
                    quantityLabel = line.quantityLabel,
                    category = line.category,
                    sourceRecipeId = sourceRecipeId,
                    sourceIngredientId = line.ingredientId,
                    checked = false,
                    note = "",
                    createdAt = nowIso,
                    updatedAt = nowIso,
                )
            }
}
