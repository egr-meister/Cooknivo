package com.cooknivo.app

import com.cooknivo.app.model.PreparationStep
import com.cooknivo.app.model.Recipe
import com.cooknivo.app.model.RecipeIngredient
import com.cooknivo.app.util.RecipeQuery
import com.cooknivo.app.util.RecipeSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {

    private val soup = Recipe(id = "1", name = "Tomato Soup", categoryId = "soups",
        description = "warm and cozy", favorite = true, updatedAt = "2026-01-02T10:00:00Z")
    private val cake = Recipe(id = "2", name = "Carrot Cake", categoryId = "desserts",
        notes = "use walnuts", updatedAt = "2026-01-03T10:00:00Z")

    private val recipes = listOf(soup, cake)
    private val ingredients = listOf(
        RecipeIngredient(id = "i1", recipeId = "1", name = "Tomatoes"),
        RecipeIngredient(id = "i2", recipeId = "2", name = "Carrots"),
    )
    private val steps = listOf(
        PreparationStep(id = "s1", recipeId = "1", instruction = "Simmer gently"),
        PreparationStep(id = "s2", recipeId = "2", instruction = "Bake at 180"),
    )

    private fun search(q: String) = RecipeQuery.search(
        recipes, q,
        categoryNameOf = { it.categoryId },
        ingredientsOf = { id -> ingredients.filter { it.recipeId == id } },
        stepsOf = { id -> steps.filter { it.recipeId == id } },
    ).map { it.id }

    @Test
    fun searchByName() {
        assertEquals(listOf("1"), search("tomato soup"))
    }

    @Test
    fun searchByIngredient() {
        assertEquals(listOf("2"), search("carrots"))
    }

    @Test
    fun searchByStep() {
        assertEquals(listOf("1"), search("simmer"))
    }

    @Test
    fun searchByNotes() {
        assertEquals(listOf("2"), search("walnuts"))
    }

    @Test
    fun blankReturnsAll() {
        assertEquals(listOf("1", "2"), search(""))
    }

    @Test
    fun sortByNameIsDeterministic() {
        val sorted = RecipeQuery.sort(recipes, RecipeSort.Name, false, { 0 }, { 0 }).map { it.id }
        assertEquals(listOf("2", "1"), sorted) // Carrot before Tomato
    }

    @Test
    fun favoritesFirst() {
        val sorted = RecipeQuery.sort(recipes, RecipeSort.Name, true, { 0 }, { 0 }).map { it.id }
        assertEquals(listOf("1", "2"), sorted) // soup is favorite -> first
    }

    @Test
    fun recentlyOpenedSorting() {
        val opened = listOf(
            soup.copy(lastOpenedAt = "2026-05-01T10:00:00Z"),
            cake.copy(lastOpenedAt = "2026-05-02T10:00:00Z"),
        )
        val result = RecipeQuery.recentlyOpened(opened, 5).map { it.id }
        assertEquals(listOf("2", "1"), result)
        assertTrue(RecipeQuery.recentlyOpened(recipes, 5).isEmpty())
    }
}
