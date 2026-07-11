package com.cooknivo.app

import com.cooknivo.app.model.Recipe
import com.cooknivo.app.util.RecipeCalc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeCalcTest {

    private fun recipe(p: Int?, c: Int?, r: Int?) =
        Recipe(id = "1", name = "x", categoryId = "cat", preparationTimeMinutes = p,
            cookingTimeMinutes = c, restingTimeMinutes = r)

    @Test
    fun totalSumsAllFields() {
        assertEquals(75, RecipeCalc.totalMinutes(recipe(15, 45, 15)))
    }

    @Test
    fun totalWithMissingFieldsCountsAsZero() {
        assertEquals(45, RecipeCalc.totalMinutes(recipe(null, 45, null)))
    }

    @Test
    fun totalNullWhenAllUnset() {
        assertNull(RecipeCalc.totalMinutes(recipe(null, null, null)))
        assertTrue(RecipeCalc.hasNoTime(recipe(null, null, null)))
    }

    @Test
    fun formatMinutesVariants() {
        assertEquals("Time not set", RecipeCalc.formatMinutes(null))
        assertEquals("Time not set", RecipeCalc.formatMinutes(0))
        assertEquals("25 min", RecipeCalc.formatMinutes(25))
        assertEquals("1 hr 20 min", RecipeCalc.formatMinutes(80))
        assertEquals("2 hr", RecipeCalc.formatMinutes(120))
    }

    @Test
    fun negativeMinutesTreatedSafely() {
        assertEquals("Time not set", RecipeCalc.formatMinutes(-10))
    }
}
