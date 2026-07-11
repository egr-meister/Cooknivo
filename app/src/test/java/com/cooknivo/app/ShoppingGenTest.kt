package com.cooknivo.app

import com.cooknivo.app.model.RecipeIngredient
import com.cooknivo.app.model.ShoppingCategory
import com.cooknivo.app.model.ShoppingItem
import com.cooknivo.app.util.ShoppingGen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingGenTest {

    private fun ing(id: String, name: String, qty: String = "", cat: ShoppingCategory = ShoppingCategory.Other) =
        RecipeIngredient(id = id, recipeId = "r1", name = name, quantityLabel = qty, shoppingCategory = cat)

    @Test
    fun previewPreservesQuantityText() {
        val preview = ShoppingGen.buildPreview(listOf(ing("1", "Flour", "2 cups")), emptyList())
        assertEquals(1, preview.size)
        assertEquals("2 cups", preview[0].quantityLabel)
    }

    @Test
    fun duplicateIngredientNamesCollapseInPreview() {
        val preview = ShoppingGen.buildPreview(
            listOf(ing("1", "Salt"), ing("2", "salt"), ing("3", "SALT")), emptyList(),
        )
        assertEquals(1, preview.size)
    }

    @Test
    fun existingItemsMarkedAlreadyInList() {
        val existing = listOf(ShoppingItem(id = "x", title = "Flour"))
        val preview = ShoppingGen.buildPreview(listOf(ing("1", "flour"), ing("2", "Sugar")), existing)
        assertTrue(preview.first { it.title == "flour" }.alreadyInList)
        assertFalse(preview.first { it.title == "Sugar" }.alreadyInList)
        assertEquals(1, ShoppingGen.missingOnly(preview).size)
    }

    @Test
    fun blankNamesSkipped() {
        val preview = ShoppingGen.buildPreview(listOf(ing("1", "  ")), emptyList())
        assertTrue(preview.isEmpty())
    }

    @Test
    fun toItemsSkipsExistingWhenNotAllowingDuplicates() {
        val existing = listOf(ShoppingItem(id = "x", title = "Flour"))
        val preview = ShoppingGen.buildPreview(listOf(ing("1", "Flour"), ing("2", "Eggs")), existing)
        val added = ShoppingGen.toShoppingItems(preview, "r1", allowDuplicates = false, "2026-01-01T00:00:00Z", { "new-" + it })
        assertEquals(1, added.size)
        assertEquals("Eggs", added[0].title)
    }

    @Test
    fun toItemsAllowsDuplicatesWhenRequested() {
        val existing = listOf(ShoppingItem(id = "x", title = "Flour"))
        val preview = ShoppingGen.buildPreview(listOf(ing("1", "Flour")), existing)
        val added = ShoppingGen.toShoppingItems(preview, "r1", allowDuplicates = true, "2026-01-01T00:00:00Z", { "new" })
        assertEquals(1, added.size)
    }
}
