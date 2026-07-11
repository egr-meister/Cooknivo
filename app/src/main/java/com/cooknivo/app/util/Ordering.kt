package com.cooknivo.app.util

/**
 * Deterministic ordering utilities. Works on any list of items that expose a
 * stable id and a sortOrder. All functions return NEW lists with normalized,
 * gap-free sortOrder values (0..n-1). They never throw and never rely on the
 * caller's list index after persistence.
 */
object Ordering {

    /** Sort by sortOrder, breaking ties by id for full determinism. */
    fun <T> sorted(items: List<T>, orderOf: (T) -> Int, idOf: (T) -> String): List<T> =
        items.sortedWith(compareBy({ orderOf(it) }, { idOf(it) }))

    /**
     * Normalize sort orders to 0..n-1 based on current (sorted) sequence.
     * Recovers cleanly from duplicate or gapped sort orders.
     */
    fun <T> normalize(
        items: List<T>,
        orderOf: (T) -> Int,
        idOf: (T) -> String,
        withOrder: (T, Int) -> T,
    ): List<T> = sorted(items, orderOf, idOf).mapIndexed { index, item -> withOrder(item, index) }

    private fun <T> moveByIndex(
        items: List<T>,
        idOf: (T) -> String,
        orderOf: (T) -> Int,
        withOrder: (T, Int) -> T,
        id: String,
        targetIndex: (current: Int, size: Int) -> Int,
    ): List<T> {
        val ordered = sorted(items, orderOf, idOf).toMutableList()
        val current = ordered.indexOfFirst { idOf(it) == id }
        if (current < 0) return items
        val dest = targetIndex(current, ordered.size).coerceIn(0, ordered.size - 1)
        if (dest == current) return normalize(ordered, orderOf, idOf, withOrder)
        val item = ordered.removeAt(current)
        ordered.add(dest, item)
        return ordered.mapIndexed { index, it -> withOrder(it, index) }
    }

    fun <T> moveUp(
        items: List<T>, id: String,
        idOf: (T) -> String, orderOf: (T) -> Int, withOrder: (T, Int) -> T,
    ): List<T> = moveByIndex(items, idOf, orderOf, withOrder, id) { c, _ -> c - 1 }

    fun <T> moveDown(
        items: List<T>, id: String,
        idOf: (T) -> String, orderOf: (T) -> Int, withOrder: (T, Int) -> T,
    ): List<T> = moveByIndex(items, idOf, orderOf, withOrder, id) { c, _ -> c + 1 }

    fun <T> moveToTop(
        items: List<T>, id: String,
        idOf: (T) -> String, orderOf: (T) -> Int, withOrder: (T, Int) -> T,
    ): List<T> = moveByIndex(items, idOf, orderOf, withOrder, id) { _, _ -> 0 }

    fun <T> moveToBottom(
        items: List<T>, id: String,
        idOf: (T) -> String, orderOf: (T) -> Int, withOrder: (T, Int) -> T,
    ): List<T> = moveByIndex(items, idOf, orderOf, withOrder, id) { _, size -> size - 1 }
}
