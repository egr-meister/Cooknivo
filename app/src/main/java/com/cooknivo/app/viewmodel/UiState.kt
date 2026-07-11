package com.cooknivo.app.viewmodel

import com.cooknivo.app.util.RecipeSort

/** Filters applied on the search screen. */
data class SearchFilters(
    val categoryId: String? = null,
    val favoritesOnly: Boolean = false,
    val includeArchived: Boolean = false,
    val hasTime: Boolean = false,
    val hasIngredients: Boolean = false,
    val hasSteps: Boolean = false,
    val recentlyEdited: Boolean = false,
    val recentlyOpened: Boolean = false,
    val sort: RecipeSort = RecipeSort.RecentlyUpdated,
)

/** Transient one-shot user messages (snackbars). */
data class UserMessage(val id: Long, val text: String)
