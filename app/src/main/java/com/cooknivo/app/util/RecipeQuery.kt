package com.cooknivo.app.util

import com.cooknivo.app.model.PreparationStep
import com.cooknivo.app.model.Recipe
import com.cooknivo.app.model.RecipeIngredient

/** Sort options for recipe lists. */
enum class RecipeSort {
    Name, RecentlyUpdated, RecentlyCreated, RecentlyOpened,
    TotalTime, IngredientCount, StepCount
}

/**
 * Local, deterministic search & filtering over recipes. Case-insensitive,
 * international-character friendly (uses locale-independent lowercasing),
 * never mutates stored data, and never sends anything anywhere.
 */
object RecipeQuery {

    private fun norm(s: String): String = s.trim().lowercase()

    /**
     * Search across name, category name, description, ingredient names,
     * step text, and notes. Blank query returns all input recipes unchanged.
     */
    fun search(
        recipes: List<Recipe>,
        query: String,
        categoryNameOf: (Recipe) -> String,
        ingredientsOf: (String) -> List<RecipeIngredient>,
        stepsOf: (String) -> List<PreparationStep>,
    ): List<Recipe> {
        val q = norm(query)
        if (q.isEmpty()) return recipes
        return recipes.filter { r ->
            if (norm(r.name).contains(q)) return@filter true
            if (norm(categoryNameOf(r)).contains(q)) return@filter true
            if (norm(r.description).contains(q)) return@filter true
            if (norm(r.notes).contains(q)) return@filter true
            if (ingredientsOf(r.id).any { norm(it.name).contains(q) }) return@filter true
            if (stepsOf(r.id).any {
                    norm(it.instruction).contains(q) || norm(it.title).contains(q)
                }
            ) return@filter true
            false
        }
    }

    /** Apply sorting; when [favoritesFirst] favorites are grouped ahead of others. */
    fun sort(
        recipes: List<Recipe>,
        sort: RecipeSort,
        favoritesFirst: Boolean,
        ingredientCountOf: (String) -> Int,
        stepCountOf: (String) -> Int,
    ): List<Recipe> {
        val base: Comparator<Recipe> = when (sort) {
            RecipeSort.Name ->
                compareBy({ it.name.lowercase() }, { it.id })
            RecipeSort.RecentlyUpdated ->
                compareByDescending<Recipe> { TimeStamps.epochMillisOrZero(it.updatedAt) }
                    .thenBy { it.id }
            RecipeSort.RecentlyCreated ->
                compareByDescending<Recipe> { TimeStamps.epochMillisOrZero(it.createdAt) }
                    .thenBy { it.id }
            RecipeSort.RecentlyOpened ->
                compareByDescending<Recipe> { TimeStamps.epochMillisOrZero(it.lastOpenedAt) }
                    .thenBy { it.id }
            RecipeSort.TotalTime ->
                compareBy<Recipe> { RecipeCalc.totalMinutes(it) ?: Int.MAX_VALUE }
                    .thenBy { it.id }
            RecipeSort.IngredientCount ->
                compareByDescending<Recipe> { ingredientCountOf(it.id) }.thenBy { it.id }
            RecipeSort.StepCount ->
                compareByDescending<Recipe> { stepCountOf(it.id) }.thenBy { it.id }
        }
        val comparator = if (favoritesFirst) {
            compareByDescending<Recipe> { it.favorite }.then(base)
        } else base
        return recipes.sortedWith(comparator)
    }

    fun recentlyOpened(recipes: List<Recipe>, limit: Int): List<Recipe> =
        recipes.filter { !it.archived && it.lastOpenedAt.isNotBlank() }
            .sortedWith(
                compareByDescending<Recipe> { TimeStamps.epochMillisOrZero(it.lastOpenedAt) }
                    .thenBy { it.id }
            )
            .take(limit)
}
