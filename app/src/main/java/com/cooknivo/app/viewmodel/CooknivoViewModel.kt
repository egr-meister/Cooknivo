package com.cooknivo.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cooknivo.app.data.CooknivoRepository
import com.cooknivo.app.model.AppData
import com.cooknivo.app.model.Fallbacks
import com.cooknivo.app.model.PreparationStep
import com.cooknivo.app.model.Recipe
import com.cooknivo.app.model.RecipeCardDensity
import com.cooknivo.app.model.RecipeCategory
import com.cooknivo.app.model.RecipeIngredient
import com.cooknivo.app.model.ShoppingCategory
import com.cooknivo.app.model.ShoppingGenerationMode
import com.cooknivo.app.model.ShoppingItem
import com.cooknivo.app.util.CollectionStats
import com.cooknivo.app.util.Ids
import com.cooknivo.app.util.Ordering
import com.cooknivo.app.util.RecipeCalc
import com.cooknivo.app.util.RecipeQuery
import com.cooknivo.app.util.ShoppingGen
import com.cooknivo.app.util.ShoppingPreviewLine
import com.cooknivo.app.util.Stats
import com.cooknivo.app.util.TimeStamps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Single app-wide ViewModel. Intentionally chosen over a per-screen ViewModel
 * fan-out for release stability (see README architecture notes). It exposes the
 * observable [AppData] snapshot and thin, guarded action wrappers over the
 * repository. Search is debounced and filtered off the main thread.
 */
@OptIn(FlowPreview::class)
class CooknivoViewModel(private val repository: CooknivoRepository) : ViewModel() {

    val appData: StateFlow<AppData> = repository.appData
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppData())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    init {
        viewModelScope.launch { repository.ensureInitialized() }
    }

    // ---- Derived read helpers (pure, safe) ----------------------------------

    fun activeRecipes(data: AppData): List<Recipe> = data.recipes.filter { !it.archived }

    fun archivedRecipes(data: AppData): List<Recipe> = data.recipes.filter { it.archived }

    fun categoryName(data: AppData, recipe: Recipe): String {
        val cat = data.categories.firstOrNull { it.id == recipe.categoryId }
        return cat?.name ?: recipe.customCategoryName.ifBlank { Fallbacks.UNCATEGORIZED }
    }

    fun ingredientsFor(data: AppData, recipeId: String): List<RecipeIngredient> =
        Ordering.sorted(
            data.ingredients.filter { it.recipeId == recipeId },
            orderOf = { it.sortOrder }, idOf = { it.id },
        )

    fun stepsFor(data: AppData, recipeId: String): List<PreparationStep> =
        Ordering.sorted(
            data.preparationSteps.filter { it.recipeId == recipeId },
            orderOf = { it.sortOrder }, idOf = { it.id },
        )

    fun recipeById(data: AppData, id: String?): Recipe? =
        if (id == null) null else data.recipes.firstOrNull { it.id == id }

    fun categoryById(data: AppData, id: String?): RecipeCategory? =
        if (id == null) null else data.categories.firstOrNull { it.id == id }

    fun activeCountForCategory(data: AppData, categoryId: String): Int =
        data.recipes.count { it.categoryId == categoryId && !it.archived }

    fun favoriteCountForCategory(data: AppData, categoryId: String): Int =
        data.recipes.count { it.categoryId == categoryId && it.favorite && !it.archived }

    fun archivedCountForCategory(data: AppData, categoryId: String): Int =
        data.recipes.count { it.categoryId == categoryId && it.archived }

    fun visibleCategories(data: AppData): List<RecipeCategory> {
        val counts = activeRecipes(data).groupingBy { it.categoryId }.eachCount()
        return data.categories
            .filter { !it.archived }
            .filterNot { it.hiddenWhenEmpty && (counts[it.id] ?: 0) == 0 }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
    }

    fun allCategoriesSorted(data: AppData): List<RecipeCategory> =
        data.categories.filter { !it.archived }.sortedWith(compareBy({ it.sortOrder }, { it.id }))

    fun favorites(data: AppData): List<Recipe> =
        activeRecipes(data).filter { it.favorite }

    fun recentlyOpened(data: AppData, limit: Int = 5): List<Recipe> =
        RecipeQuery.recentlyOpened(data.recipes, limit)

    fun stats(data: AppData): CollectionStats = Stats.compute(data)

    // ---- Debounced, off-main-thread search results --------------------------

    val searchResults: StateFlow<List<Recipe>> = combine(
        appData,
        _searchQuery.debounce(200).distinctUntilChanged(),
        _filters,
    ) { data, query, filters ->
        computeResults(data, query, filters)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun computeResults(data: AppData, query: String, filters: SearchFilters): List<Recipe> {
        val ingredientCountByRecipe = data.ingredients.groupingBy { it.recipeId }.eachCount()
        val stepCountByRecipe = data.preparationSteps.groupingBy { it.recipeId }.eachCount()

        var pool = data.recipes.filter { r ->
            (filters.includeArchived || !r.archived)
        }
        filters.categoryId?.let { catId -> pool = pool.filter { it.categoryId == catId } }
        if (filters.favoritesOnly) pool = pool.filter { it.favorite }
        if (filters.hasTime) pool = pool.filter { !RecipeCalc.hasNoTime(it) }
        if (filters.hasIngredients) pool = pool.filter { (ingredientCountByRecipe[it.id] ?: 0) > 0 }
        if (filters.hasSteps) pool = pool.filter { (stepCountByRecipe[it.id] ?: 0) > 0 }
        if (filters.recentlyOpened) pool = pool.filter { it.lastOpenedAt.isNotBlank() }

        val searched = RecipeQuery.search(
            recipes = pool,
            query = query,
            categoryNameOf = { r -> categoryName(data, r) },
            ingredientsOf = { id -> data.ingredients.filter { it.recipeId == id } },
            stepsOf = { id -> data.preparationSteps.filter { it.recipeId == id } },
        )

        return RecipeQuery.sort(
            recipes = searched,
            sort = filters.sort,
            favoritesFirst = data.settings.favoritesFirst,
            ingredientCountOf = { id -> ingredientCountByRecipe[id] ?: 0 },
            stepCountOf = { id -> stepCountByRecipe[id] ?: 0 },
        )
    }

    /** One-shot read of the persisted data (used to choose the start screen). */
    suspend fun initialAppData(): AppData = repository.appData.first()

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun updateFilters(transform: (SearchFilters) -> SearchFilters) {
        _filters.value = transform(_filters.value)
    }
    fun clearSearch() { _searchQuery.value = ""; _filters.value = SearchFilters() }

    // ---- Actions (all guarded via repository) -------------------------------

    fun createRecipe(
        name: String, categoryId: String, description: String,
        prep: Int?, cook: Int?, rest: Int?, servingLabel: String,
        notes: String, favorite: Boolean, onCreated: (String) -> Unit = {},
    ) = launchWithResult(onCreated) {
        repository.createRecipe(name, categoryId, description, prep, cook, rest, servingLabel, notes, favorite)
    }

    fun updateRecipe(recipe: Recipe) = launch { repository.updateRecipe(recipe) }
    fun saveRecipeDraft(
        draft: Recipe,
        ingredients: List<RecipeIngredient>,
        steps: List<PreparationStep>,
        onSaved: (String) -> Unit = {},
    ) = launchWithResult(onSaved) { repository.saveRecipeDraft(draft, ingredients, steps) }
    fun deleteRecipe(id: String) = launch { repository.deleteRecipe(id) }
    fun duplicateRecipe(id: String, onDone: (String?) -> Unit = {}) =
        launchWithResult(onDone) { repository.duplicateRecipe(id) }
    fun setArchived(id: String, archived: Boolean) = launch { repository.setArchived(id, archived) }
    fun toggleFavorite(id: String) = launch { repository.toggleFavorite(id) }
    fun moveRecipeToCategory(id: String, categoryId: String) =
        launch { repository.moveRecipeToCategory(id, categoryId) }
    fun markOpened(id: String) = launch { repository.markOpened(id) }

    fun addIngredient(
        recipeId: String, name: String, quantity: String, group: String,
        category: ShoppingCategory, addToShopping: Boolean, note: String,
    ) = launch { repository.addIngredient(recipeId, name, quantity, group, category, addToShopping, note) }
    fun updateIngredient(ingredient: RecipeIngredient) = launch { repository.updateIngredient(ingredient) }
    fun deleteIngredient(id: String) = launch { repository.deleteIngredient(id) }
    fun moveIngredientUp(recipeId: String, id: String) = launch { repository.moveIngredientUp(recipeId, id) }
    fun moveIngredientDown(recipeId: String, id: String) = launch { repository.moveIngredientDown(recipeId, id) }
    fun moveIngredientToTop(recipeId: String, id: String) = launch { repository.moveIngredientToTop(recipeId, id) }
    fun moveIngredientToBottom(recipeId: String, id: String) = launch { repository.moveIngredientToBottom(recipeId, id) }

    fun addStep(recipeId: String, title: String, instruction: String, timer: String, note: String) =
        launch { repository.addStep(recipeId, title, instruction, timer, note) }
    fun updateStep(step: PreparationStep) = launch { repository.updateStep(step) }
    fun deleteStep(id: String) = launch { repository.deleteStep(id) }
    fun duplicateStep(id: String) = launch { repository.duplicateStep(id) }
    fun moveStepUp(recipeId: String, id: String) = launch { repository.moveStepUp(recipeId, id) }
    fun moveStepDown(recipeId: String, id: String) = launch { repository.moveStepDown(recipeId, id) }
    fun moveStepToTop(recipeId: String, id: String) = launch { repository.moveStepToTop(recipeId, id) }
    fun moveStepToBottom(recipeId: String, id: String) = launch { repository.moveStepToBottom(recipeId, id) }

    fun addCustomCategory(name: String, colorKey: String) = launch { repository.addCustomCategory(name, colorKey) }
    fun renameCategory(id: String, name: String, colorKey: String?) =
        launch { repository.renameCategory(id, name, colorKey) }
    fun setCategoryHiddenWhenEmpty(id: String, hidden: Boolean) =
        launch { repository.setCategoryHiddenWhenEmpty(id, hidden) }
    fun moveCategoryUp(id: String) = launch { repository.moveCategoryUp(id) }
    fun moveCategoryDown(id: String) = launch { repository.moveCategoryDown(id) }
    fun deleteUnusedCategory(id: String, onResult: (Boolean) -> Unit = {}) =
        launchWithResult(onResult) { repository.deleteUnusedCategory(id) }
    fun reassignRecipes(from: String, to: String) = launch { repository.reassignRecipes(from, to) }
    fun restoreDefaultCategories() = launch { repository.restoreDefaultCategories() }

    fun addShoppingItems(items: List<ShoppingItem>) = launch { repository.addShoppingItems(items) }
    fun addCustomShoppingItem(title: String, quantity: String, category: ShoppingCategory, note: String) =
        launch { repository.addCustomShoppingItem(title, quantity, category, note) }
    fun updateShoppingItem(item: ShoppingItem) = launch { repository.updateShoppingItem(item) }
    fun setShoppingChecked(id: String, checked: Boolean) = launch { repository.setShoppingChecked(id, checked) }
    fun deleteShoppingItem(id: String) = launch { repository.deleteShoppingItem(id) }
    fun clearCheckedShoppingItems() = launch { repository.clearCheckedShoppingItems() }
    fun deleteAllShoppingItems() = launch { repository.deleteAllShoppingItems() }

    fun setOnboardingCompleted(done: Boolean) = launch { repository.setOnboardingCompleted(done) }
    fun setDefaultCategory(id: String?) = launch { repository.setDefaultCategory(id) }
    fun setCardDensity(density: RecipeCardDensity) = launch { repository.setCardDensity(density) }
    fun setShowRecentlyOpened(show: Boolean) = launch { repository.setShowRecentlyOpened(show) }
    fun setFavoritesFirst(value: Boolean) = launch { repository.setFavoritesFirst(value) }
    fun setShoppingGenerationMode(mode: ShoppingGenerationMode) =
        launch { repository.setShoppingGenerationMode(mode) }

    fun deleteAllRecipes() = launch { repository.deleteAllRecipes() }
    fun deleteAllArchivedRecipes() = launch { repository.deleteAllArchivedRecipes() }
    fun resetAllData() = launch { repository.resetAllData() }

    // ---- Shopping preview ---------------------------------------------------

    fun buildShoppingPreview(
        data: AppData, recipeId: String, selectedIngredientIds: Set<String>,
    ): List<ShoppingPreviewLine> {
        val ingredients = ingredientsFor(data, recipeId).filter { it.id in selectedIngredientIds }
        return ShoppingGen.buildPreview(ingredients, data.shoppingItems)
    }

    fun confirmShoppingPreview(
        recipeId: String, lines: List<ShoppingPreviewLine>, allowDuplicates: Boolean,
    ) = launch {
        val items = ShoppingGen.toShoppingItems(
            lines, recipeId, allowDuplicates, TimeStamps.nowIso(), { Ids.newId() },
        )
        repository.addShoppingItems(items)
    }

    // ---- coroutine helpers --------------------------------------------------

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun <T> launchWithResult(onResult: (T) -> Unit, block: suspend () -> T) {
        viewModelScope.launch {
            val result = block()
            onResult(result)
        }
    }

    class Factory(private val repository: CooknivoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CooknivoViewModel::class.java)) {
                return CooknivoViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
