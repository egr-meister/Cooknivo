package com.cooknivo.app

import com.cooknivo.app.data.DefaultData
import com.cooknivo.app.model.AppData
import com.cooknivo.app.model.PreparationStep
import com.cooknivo.app.model.Recipe
import com.cooknivo.app.model.RecipeIngredient
import com.cooknivo.app.util.Stats
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsTest {

    @Test
    fun computesNeutralCollectionStats() {
        val cats = DefaultData.buildDefaultCategories("2026-01-01T00:00:00Z")
        val data = AppData(
            recipes = listOf(
                Recipe(id = "1", name = "A", categoryId = "default_soups", favorite = true),
                Recipe(id = "2", name = "B", categoryId = "default_soups"),
                Recipe(id = "3", name = "C", categoryId = "default_desserts", archived = true),
            ),
            ingredients = listOf(
                RecipeIngredient(id = "i1", recipeId = "1", name = "x"),
                RecipeIngredient(id = "i2", recipeId = "1", name = "y"),
            ),
            preparationSteps = listOf(
                PreparationStep(id = "s1", recipeId = "2", instruction = "do"),
            ),
            categories = cats,
        )
        val stats = Stats.compute(data)
        assertEquals(2, stats.totalActive)
        assertEquals(1, stats.favorites)
        assertEquals(1, stats.archived)
        assertEquals(1, stats.withIngredients)
        assertEquals(1, stats.withSteps)
        assertEquals(1.0, stats.averageIngredients, 0.001)
        assertEquals(0.5, stats.averageSteps, 0.001)
        // soups category should show 2 active recipes
        assertEquals(2, stats.perCategory.first { it.categoryId == "default_soups" }.count)
    }

    @Test
    fun emptyDataDoesNotThrow() {
        val stats = Stats.compute(AppData())
        assertEquals(0, stats.totalActive)
        assertEquals(0.0, stats.averageIngredients, 0.001)
    }
}
