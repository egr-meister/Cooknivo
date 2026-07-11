package com.cooknivo.app.util

import com.cooknivo.app.model.AppData

/** Neutral collection statistics. No health, diet, calorie, or quality scoring. */
data class CollectionStats(
    val totalActive: Int,
    val favorites: Int,
    val archived: Int,
    val withIngredients: Int,
    val withSteps: Int,
    val averageIngredients: Double,
    val averageSteps: Double,
    val recentlyAdded: Int,
    val shoppingRemaining: Int,
    val perCategory: List<CategoryCount>,
)

data class CategoryCount(val categoryId: String, val name: String, val count: Int)

object Stats {

    /** [recentWindowDays] counts recipes created within the given number of days. */
    fun compute(data: AppData, recentWindowDays: Int = 14): CollectionStats {
        val active = data.recipes.filter { !it.archived }
        val archived = data.recipes.filter { it.archived }

        val ingredientCountByRecipe = data.ingredients.groupingBy { it.recipeId }.eachCount()
        val stepCountByRecipe = data.preparationSteps.groupingBy { it.recipeId }.eachCount()

        val withIngredients = active.count { (ingredientCountByRecipe[it.id] ?: 0) > 0 }
        val withSteps = active.count { (stepCountByRecipe[it.id] ?: 0) > 0 }

        val avgIngredients = if (active.isEmpty()) 0.0
        else active.sumOf { (ingredientCountByRecipe[it.id] ?: 0) }.toDouble() / active.size
        val avgSteps = if (active.isEmpty()) 0.0
        else active.sumOf { (stepCountByRecipe[it.id] ?: 0) }.toDouble() / active.size

        val cutoff = System.currentTimeMillis() - recentWindowDays.toLong() * 24 * 60 * 60 * 1000
        val recentlyAdded = active.count { TimeStamps.epochMillisOrZero(it.createdAt) >= cutoff }

        val activeByCategory = active.groupingBy { it.categoryId }.eachCount()
        val perCategory = data.categories
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .map { CategoryCount(it.id, it.name, activeByCategory[it.id] ?: 0) }

        return CollectionStats(
            totalActive = active.size,
            favorites = active.count { it.favorite },
            archived = archived.size,
            withIngredients = withIngredients,
            withSteps = withSteps,
            averageIngredients = avgIngredients,
            averageSteps = avgSteps,
            recentlyAdded = recentlyAdded,
            shoppingRemaining = data.shoppingItems.count { !it.checked },
            perCategory = perCategory,
        )
    }
}
