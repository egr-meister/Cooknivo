package com.cooknivo.app

import com.cooknivo.app.util.Ordering
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderingTest {

    private data class Item(val id: String, val sortOrder: Int)

    private fun ordered(items: List<Item>) =
        Ordering.sorted(items, { it.sortOrder }, { it.id }).map { it.id }

    private fun move(
        items: List<Item>,
        op: (List<Item>, String, (Item) -> String, (Item) -> Int, (Item, Int) -> Item) -> List<Item>,
        id: String,
    ) = op(items, id, { it.id }, { it.sortOrder }, { i, o -> i.copy(sortOrder = o) })

    @Test
    fun sortedIsDeterministicWithDuplicateOrders() {
        val items = listOf(Item("b", 0), Item("a", 0), Item("c", 0))
        assertEquals(listOf("a", "b", "c"), ordered(items))
    }

    @Test
    fun normalizeRemovesGapsAndDuplicates() {
        val items = listOf(Item("a", 5), Item("b", 5), Item("c", 9))
        val normalized = Ordering.normalize(
            items, { it.sortOrder }, { it.id }, { i, o -> i.copy(sortOrder = o) },
        )
        assertEquals(listOf(0, 1, 2), normalized.map { it.sortOrder })
    }

    @Test
    fun moveUpAndDown() {
        val items = listOf(Item("a", 0), Item("b", 1), Item("c", 2))
        assertEquals(listOf("b", "a", "c"), move(items, Ordering::moveUp, "b").let { ordered(it) })
        assertEquals(listOf("a", "c", "b"), move(items, Ordering::moveDown, "b").let { ordered(it) })
    }

    @Test
    fun moveToTopAndBottom() {
        val items = listOf(Item("a", 0), Item("b", 1), Item("c", 2))
        assertEquals(listOf("c", "a", "b"), move(items, Ordering::moveToTop, "c").let { ordered(it) })
        assertEquals(listOf("b", "c", "a"), move(items, Ordering::moveToBottom, "a").let { ordered(it) })
    }

    @Test
    fun moveUnknownIdIsNoOp() {
        val items = listOf(Item("a", 0), Item("b", 1))
        assertEquals(listOf("a", "b"), move(items, Ordering::moveUp, "z").let { ordered(it) })
    }
}
