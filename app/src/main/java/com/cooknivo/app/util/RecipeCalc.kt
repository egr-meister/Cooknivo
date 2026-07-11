package com.cooknivo.app.util

import com.cooknivo.app.model.PreparationStep
import com.cooknivo.app.model.Recipe
import com.cooknivo.app.model.RecipeIngredient

/**
 * Pure recipe calculation helpers. These NEVER throw into Compose UI, always
 * return safe fallbacks, and perform no nutrition calculations of any kind.
 */
object RecipeCalc {

    /** True if every time field is null/unset. */
    fun hasNoTime(recipe: Recipe): Boolean =
        recipe.preparationTimeMinutes == null &&
            recipe.cookingTimeMinutes == null &&
            recipe.restingTimeMinutes == null

    /**
     * Total time in minutes. Missing fields count as zero. Uses Long math and
     * clamps to a safe maximum to prevent overflow. Returns null when nothing set.
     */
    fun totalMinutes(recipe: Recipe): Int? {
        if (hasNoTime(recipe)) return null
        val sum = (recipe.preparationTimeMinutes ?: 0).toLong() +
            (recipe.cookingTimeMinutes ?: 0).toLong() +
            (recipe.restingTimeMinutes ?: 0).toLong()
        return sum.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }

    /** Format minutes as "25 min", "1 hr 20 min", "2 hr". Negative/zero handled. */
    fun formatMinutes(minutes: Int?): String {
        if (minutes == null) return "Time not set"
        val m = minutes.coerceAtLeast(0)
        if (m == 0) return "Time not set"
        val hours = m / 60
        val mins = m % 60
        return when {
            hours == 0 -> "$mins min"
            mins == 0 -> "$hours hr"
            else -> "$hours hr $mins min"
        }
    }

    /** Human-readable total-time label for a recipe. */
    fun totalTimeLabel(recipe: Recipe): String = formatMinutes(totalMinutes(recipe))

    fun ingredientCount(ingredients: List<RecipeIngredient>, recipeId: String): Int =
        ingredients.count { it.recipeId == recipeId }

    fun stepCount(steps: List<PreparationStep>, recipeId: String): Int =
        steps.count { it.recipeId == recipeId }

    /** Short single-line preview of notes, safe for card display. */
    fun notePreview(notes: String, maxChars: Int = 80): String {
        val oneLine = notes.replace("\n", " ").trim()
        if (oneLine.isEmpty()) return ""
        return if (oneLine.length <= maxChars) oneLine else oneLine.substring(0, maxChars).trimEnd() + "…"
    }
}
