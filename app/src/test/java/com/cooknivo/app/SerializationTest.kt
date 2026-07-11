package com.cooknivo.app

import com.cooknivo.app.data.CooknivoJson
import com.cooknivo.app.data.DefaultData
import com.cooknivo.app.model.AppSettings
import com.cooknivo.app.model.Recipe
import com.cooknivo.app.model.ShoppingCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SerializationTest {

    @Test
    fun roundTripRecipe() {
        val r = Recipe(id = "1", name = "Soup", categoryId = "c", preparationTimeMinutes = 10)
        val json = CooknivoJson.encode(listOf(r))
        val back = CooknivoJson.decodeArrayResilient<Recipe>(json)
        assertEquals(r, back.single())
    }

    @Test
    fun corruptedTopLevelReturnsEmpty() {
        assertTrue(CooknivoJson.decodeArrayResilient<Recipe>("not json at all").isEmpty())
        assertTrue(CooknivoJson.decodeArrayResilient<Recipe>("").isEmpty())
        assertTrue(CooknivoJson.decodeArrayResilient<Recipe>(null).isEmpty())
    }

    @Test
    fun malformedElementIsSkippedButValidKept() {
        // Second element is missing required "name" and "categoryId".
        val json = """[{"id":"1","name":"Ok","categoryId":"c"},{"id":"2"},{"id":"3","name":"Also","categoryId":"c"}]"""
        val back = CooknivoJson.decodeArrayResilient<Recipe>(json)
        assertEquals(listOf("1", "3"), back.map { it.id })
    }

    @Test
    fun unknownKeysIgnored() {
        val json = """[{"id":"1","name":"Ok","categoryId":"c","legacyField":true}]"""
        val back = CooknivoJson.decodeArrayResilient<Recipe>(json)
        assertEquals("1", back.single().id)
    }

    @Test
    fun settingsDefaultsAreStable() {
        val s = AppSettings()
        assertEquals(false, s.onboardingCompleted)
        assertEquals(com.cooknivo.app.model.ShoppingGenerationMode.PreviewFirst, s.shoppingGenerationMode)
    }

    @Test
    fun defaultCategoriesHaveStableUniqueIds() {
        val cats = DefaultData.buildDefaultCategories("2026-01-01T00:00:00Z")
        assertEquals(11, cats.size)
        assertEquals(cats.size, cats.map { it.id }.toSet().size)
        assertEquals(cats.map { it.id }, DefaultData.buildDefaultCategories("later").map { it.id })
    }

    @Test
    fun unknownShoppingCategoryFallsBackToOther() {
        assertEquals(ShoppingCategory.Other, ShoppingCategory.safeValueOf("Nonexistent"))
        assertEquals(ShoppingCategory.Dairy, ShoppingCategory.safeValueOf("dairy"))
    }
}
