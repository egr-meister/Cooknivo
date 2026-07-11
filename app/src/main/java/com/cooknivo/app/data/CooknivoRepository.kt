package com.cooknivo.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cooknivo.app.model.AppData
import com.cooknivo.app.model.AppSettings
import com.cooknivo.app.model.PreparationStep
import com.cooknivo.app.model.Recipe
import com.cooknivo.app.model.RecipeCardDensity
import com.cooknivo.app.model.RecipeCategory
import com.cooknivo.app.model.RecipeIngredient
import com.cooknivo.app.model.ShoppingCategory
import com.cooknivo.app.model.ShoppingGenerationMode
import com.cooknivo.app.model.ShoppingItem
import com.cooknivo.app.util.Ids
import com.cooknivo.app.util.Ordering
import com.cooknivo.app.util.TextLimits
import com.cooknivo.app.util.TimeStamps
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DataStore instance scoped to the application context. */
private val Context.cooknivoDataStore: DataStore<Preferences> by preferencesDataStore(name = "cooknivo")

/**
 * Single local repository backed by DataStore Preferences with serialized JSON.
 * All operations are guarded and never crash on empty/corrupt/missing data.
 */
class CooknivoRepository(context: Context) {

    private val dataStore = context.applicationContext.cooknivoDataStore

    private object Keys {
        val RECIPES = stringPreferencesKey("recipes_json")
        val INGREDIENTS = stringPreferencesKey("recipe_ingredients_json")
        val STEPS = stringPreferencesKey("preparation_steps_json")
        val CATEGORIES = stringPreferencesKey("recipe_categories_json")
        val SHOPPING = stringPreferencesKey("shopping_items_json")
        val SETTINGS = stringPreferencesKey("settings_json")
    }

    // ---- Read ---------------------------------------------------------------

    /**
     * Observable app data. Safely decodes every key with item-level recovery.
     * If categories were never initialized, presents default categories as a
     * non-persisted fallback so the UI is never empty before init completes.
     */
    val appData: Flow<AppData> = dataStore.data.map { prefs ->
        val categoriesRaw = prefs[Keys.CATEGORIES]
        val categories = if (categoriesRaw == null) {
            DefaultData.buildDefaultCategories(TimeStamps.nowIso())
        } else {
            CooknivoJson.decodeArrayResilient<RecipeCategory>(categoriesRaw)
        }
        AppData(
            recipes = CooknivoJson.decodeArrayResilient(prefs[Keys.RECIPES]),
            ingredients = CooknivoJson.decodeArrayResilient(prefs[Keys.INGREDIENTS]),
            preparationSteps = CooknivoJson.decodeArrayResilient(prefs[Keys.STEPS]),
            categories = categories,
            shoppingItems = CooknivoJson.decodeArrayResilient(prefs[Keys.SHOPPING]),
            settings = decodeSettings(prefs[Keys.SETTINGS]),
        )
    }

    private fun decodeSettings(raw: String?): AppSettings {
        if (raw.isNullOrBlank()) return AppSettings()
        return try {
            CooknivoJson.instance.decodeFromString(AppSettings.serializer(), raw)
        } catch (_: Exception) {
            AppSettings()
        }
    }

    // ---- Internal read/write helpers ---------------------------------------

    private fun MutablePreferences.recipes(): List<Recipe> =
        CooknivoJson.decodeArrayResilient(this[Keys.RECIPES])

    private fun MutablePreferences.ingredients(): List<RecipeIngredient> =
        CooknivoJson.decodeArrayResilient(this[Keys.INGREDIENTS])

    private fun MutablePreferences.steps(): List<PreparationStep> =
        CooknivoJson.decodeArrayResilient(this[Keys.STEPS])

    private fun MutablePreferences.categoriesOrNull(): List<RecipeCategory>? {
        val raw = this[Keys.CATEGORIES] ?: return null
        return CooknivoJson.decodeArrayResilient(raw)
    }

    private fun MutablePreferences.shopping(): List<ShoppingItem> =
        CooknivoJson.decodeArrayResilient(this[Keys.SHOPPING])

    private fun MutablePreferences.settings(): AppSettings = decodeSettings(this[Keys.SETTINGS])

    private fun MutablePreferences.setRecipes(list: List<Recipe>) {
        this[Keys.RECIPES] = CooknivoJson.encode(list)
    }

    private fun MutablePreferences.setIngredients(list: List<RecipeIngredient>) {
        this[Keys.INGREDIENTS] = CooknivoJson.encode(list)
    }

    private fun MutablePreferences.setSteps(list: List<PreparationStep>) {
        this[Keys.STEPS] = CooknivoJson.encode(list)
    }

    private fun MutablePreferences.setCategories(list: List<RecipeCategory>) {
        this[Keys.CATEGORIES] = CooknivoJson.encode(list)
    }

    private fun MutablePreferences.setShopping(list: List<ShoppingItem>) {
        this[Keys.SHOPPING] = CooknivoJson.encode(list)
    }

    private fun MutablePreferences.setSettings(settings: AppSettings) {
        this[Keys.SETTINGS] = CooknivoJson.encode(settings)
    }

    // ---- Initialization -----------------------------------------------------

    /**
     * Initialize default categories exactly once (first launch). Uses the presence
     * of the categories key as the "initialized" flag so relaunches never
     * duplicate defaults. Also sets an initial default category in settings.
     */
    suspend fun ensureInitialized() {
        dataStore.edit { prefs ->
            if (prefs.categoriesOrNull() == null) {
                val now = TimeStamps.nowIso()
                prefs.setCategories(DefaultData.buildDefaultCategories(now))
            }
            val settings = prefs.settings()
            if (settings.defaultCategoryId == null) {
                prefs.setSettings(
                    settings.copy(defaultCategoryId = DefaultData.FIRST_DEFAULT_CATEGORY_ID)
                )
            }
        }
    }

    // ---- Recipes ------------------------------------------------------------

    /** Create a recipe. Returns the generated id. Name/category are required by callers. */
    suspend fun createRecipe(
        name: String,
        categoryId: String,
        description: String,
        preparationTimeMinutes: Int?,
        cookingTimeMinutes: Int?,
        restingTimeMinutes: Int?,
        servingLabel: String,
        notes: String,
        favorite: Boolean,
    ): String {
        val id = Ids.newId()
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val recipe = Recipe(
                id = id,
                name = TextLimits.clamp(name, TextLimits.RECIPE_NAME),
                categoryId = categoryId,
                description = TextLimits.clampMultiline(description, TextLimits.DESCRIPTION),
                preparationTimeMinutes = sanitizeMinutes(preparationTimeMinutes),
                cookingTimeMinutes = sanitizeMinutes(cookingTimeMinutes),
                restingTimeMinutes = sanitizeMinutes(restingTimeMinutes),
                servingLabel = servingLabel.trim(),
                favorite = favorite,
                archived = false,
                notes = TextLimits.clampMultiline(notes, TextLimits.RECIPE_NOTES),
                createdAt = now,
                updatedAt = now,
                lastOpenedAt = "",
            )
            prefs.setRecipes(prefs.recipes() + recipe)
        }
        return id
    }

    suspend fun updateRecipe(updated: Recipe) {
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val current = prefs.recipes()
            val existing = current.firstOrNull { it.id == updated.id } ?: return@edit
            val clean = updated.copy(
                name = TextLimits.clamp(updated.name, TextLimits.RECIPE_NAME),
                description = TextLimits.clampMultiline(updated.description, TextLimits.DESCRIPTION),
                notes = TextLimits.clampMultiline(updated.notes, TextLimits.RECIPE_NOTES),
                servingLabel = updated.servingLabel.trim(),
                preparationTimeMinutes = sanitizeMinutes(updated.preparationTimeMinutes),
                cookingTimeMinutes = sanitizeMinutes(updated.cookingTimeMinutes),
                restingTimeMinutes = sanitizeMinutes(updated.restingTimeMinutes),
                createdAt = existing.createdAt, // never change createdAt
                updatedAt = now,
            )
            prefs.setRecipes(current.map { if (it.id == clean.id) clean else it })
        }
    }

    suspend fun deleteRecipe(recipeId: String) {
        dataStore.edit { prefs ->
            prefs.setRecipes(prefs.recipes().filterNot { it.id == recipeId })
            prefs.setIngredients(prefs.ingredients().filterNot { it.recipeId == recipeId })
            prefs.setSteps(prefs.steps().filterNot { it.recipeId == recipeId })
            // Historical shopping items keep their sourceRecipeId; they are not deleted.
        }
    }

    /** Duplicate a recipe with new IDs for it and all its ingredients/steps. */
    suspend fun duplicateRecipe(recipeId: String): String? {
        var newRecipeId: String? = null
        dataStore.edit { prefs ->
            val recipes = prefs.recipes()
            val original = recipes.firstOrNull { it.id == recipeId } ?: return@edit
            val now = TimeStamps.nowIso()
            val newId = Ids.newId()
            newRecipeId = newId
            val copyName = TextLimits.clamp("${original.name} Copy", TextLimits.RECIPE_NAME)
            val copy = original.copy(
                id = newId,
                name = copyName,
                createdAt = now,
                updatedAt = now,
                lastOpenedAt = "",
                archived = false,
            )
            val newIngredients = prefs.ingredients()
                .filter { it.recipeId == recipeId }
                .map { it.copy(id = Ids.newId(), recipeId = newId, createdAt = now, updatedAt = now) }
            val newSteps = prefs.steps()
                .filter { it.recipeId == recipeId }
                .map { it.copy(id = Ids.newId(), recipeId = newId, createdAt = now, updatedAt = now) }
            prefs.setRecipes(recipes + copy)
            prefs.setIngredients(prefs.ingredients() + newIngredients)
            prefs.setSteps(prefs.steps() + newSteps)
        }
        return newRecipeId
    }

    suspend fun setArchived(recipeId: String, archived: Boolean) =
        mutateRecipe(recipeId) { it.copy(archived = archived) }

    suspend fun toggleFavorite(recipeId: String) =
        mutateRecipe(recipeId) { it.copy(favorite = !it.favorite) }

    suspend fun setFavorite(recipeId: String, favorite: Boolean) =
        mutateRecipe(recipeId) { it.copy(favorite = favorite) }

    suspend fun moveRecipeToCategory(recipeId: String, categoryId: String) =
        mutateRecipe(recipeId) { it.copy(categoryId = categoryId) }

    /** Update last-opened timestamp when the detail screen opens. */
    suspend fun markOpened(recipeId: String) {
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val recipes = prefs.recipes()
            if (recipes.none { it.id == recipeId }) return@edit
            prefs.setRecipes(
                recipes.map { if (it.id == recipeId) it.copy(lastOpenedAt = now) else it }
            )
        }
    }

    private suspend fun mutateRecipe(recipeId: String, transform: (Recipe) -> Recipe) {
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val recipes = prefs.recipes()
            if (recipes.none { it.id == recipeId }) return@edit
            prefs.setRecipes(
                recipes.map { if (it.id == recipeId) transform(it).copy(updatedAt = now) else it }
            )
        }
    }

    /**
     * Save an entire recipe draft in one atomic write: upserts the recipe and
     * fully replaces its ingredient and step sets. Assigns ids to new children,
     * normalizes sort orders, and renumbers steps. Used by the recipe editor so
     * form state is never partially persisted. Returns the recipe id.
     */
    suspend fun saveRecipeDraft(
        draft: Recipe,
        ingredients: List<RecipeIngredient>,
        steps: List<PreparationStep>,
    ): String {
        val now = TimeStamps.nowIso()
        val recipeId = draft.id.ifBlank { Ids.newId() }
        dataStore.edit { prefs ->
            val existing = prefs.recipes().firstOrNull { it.id == recipeId }
            val recipe = draft.copy(
                id = recipeId,
                name = TextLimits.clamp(draft.name, TextLimits.RECIPE_NAME),
                description = TextLimits.clampMultiline(draft.description, TextLimits.DESCRIPTION),
                notes = TextLimits.clampMultiline(draft.notes, TextLimits.RECIPE_NOTES),
                servingLabel = draft.servingLabel.trim(),
                preparationTimeMinutes = sanitizeMinutes(draft.preparationTimeMinutes),
                cookingTimeMinutes = sanitizeMinutes(draft.cookingTimeMinutes),
                restingTimeMinutes = sanitizeMinutes(draft.restingTimeMinutes),
                createdAt = existing?.createdAt ?: draft.createdAt.ifBlank { now },
                updatedAt = now,
                lastOpenedAt = existing?.lastOpenedAt ?: draft.lastOpenedAt,
            )
            // Upsert recipe.
            val recipes = prefs.recipes()
            val newRecipes = if (recipes.any { it.id == recipeId }) {
                recipes.map { if (it.id == recipeId) recipe else it }
            } else {
                recipes + recipe
            }
            prefs.setRecipes(newRecipes)

            // Replace ingredients for this recipe (normalized order).
            val cleanedIngredients = ingredients
                .filter { it.name.isNotBlank() }
                .mapIndexed { index, ing ->
                    ing.copy(
                        id = ing.id.ifBlank { Ids.newId() },
                        recipeId = recipeId,
                        name = TextLimits.clamp(ing.name, TextLimits.INGREDIENT_NAME),
                        quantityLabel = TextLimits.clamp(ing.quantityLabel, TextLimits.INGREDIENT_QUANTITY),
                        groupName = ing.groupName.trim(),
                        note = TextLimits.clampMultiline(ing.note, TextLimits.INGREDIENT_NOTE),
                        sortOrder = index,
                        createdAt = ing.createdAt.ifBlank { now },
                        updatedAt = now,
                    )
                }
            val otherIngredients = prefs.ingredients().filterNot { it.recipeId == recipeId }
            prefs.setIngredients(otherIngredients + cleanedIngredients)

            // Replace steps for this recipe (renumbered, blank instructions dropped).
            val cleanedSteps = steps
                .filter { it.instruction.isNotBlank() }
                .mapIndexed { index, step ->
                    step.copy(
                        id = step.id.ifBlank { Ids.newId() },
                        recipeId = recipeId,
                        title = TextLimits.clamp(step.title, TextLimits.STEP_TITLE),
                        instruction = TextLimits.clampMultiline(step.instruction, TextLimits.STEP_INSTRUCTION),
                        timerLabel = step.timerLabel.trim(),
                        note = TextLimits.clampMultiline(step.note, TextLimits.STEP_NOTE),
                        sortOrder = index,
                        stepNumber = index + 1,
                        createdAt = step.createdAt.ifBlank { now },
                        updatedAt = now,
                    )
                }
            val otherSteps = prefs.steps().filterNot { it.recipeId == recipeId }
            prefs.setSteps(otherSteps + cleanedSteps)
        }
        return recipeId
    }

    // ---- Ingredients --------------------------------------------------------

    suspend fun addIngredient(
        recipeId: String,
        name: String,
        quantityLabel: String,
        groupName: String,
        shoppingCategory: ShoppingCategory,
        addToShoppingByDefault: Boolean,
        note: String,
    ): String {
        val id = Ids.newId()
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val current = prefs.ingredients()
            val maxOrder = current.filter { it.recipeId == recipeId }
                .maxOfOrNull { it.sortOrder } ?: -1
            val ingredient = RecipeIngredient(
                id = id,
                recipeId = recipeId,
                name = TextLimits.clamp(name, TextLimits.INGREDIENT_NAME),
                quantityLabel = TextLimits.clamp(quantityLabel, TextLimits.INGREDIENT_QUANTITY),
                groupName = groupName.trim(),
                shoppingCategory = shoppingCategory,
                addToShoppingByDefault = addToShoppingByDefault,
                sortOrder = maxOrder + 1,
                note = TextLimits.clampMultiline(note, TextLimits.INGREDIENT_NOTE),
                createdAt = now,
                updatedAt = now,
            )
            prefs.setIngredients(current + ingredient)
        }
        return id
    }

    suspend fun updateIngredient(updated: RecipeIngredient) {
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val current = prefs.ingredients()
            if (current.none { it.id == updated.id }) return@edit
            val clean = updated.copy(
                name = TextLimits.clamp(updated.name, TextLimits.INGREDIENT_NAME),
                quantityLabel = TextLimits.clamp(updated.quantityLabel, TextLimits.INGREDIENT_QUANTITY),
                groupName = updated.groupName.trim(),
                note = TextLimits.clampMultiline(updated.note, TextLimits.INGREDIENT_NOTE),
                updatedAt = now,
            )
            prefs.setIngredients(current.map { if (it.id == clean.id) clean else it })
        }
    }

    suspend fun deleteIngredient(ingredientId: String) {
        dataStore.edit { prefs ->
            val current = prefs.ingredients()
            val target = current.firstOrNull { it.id == ingredientId } ?: return@edit
            val remaining = current.filterNot { it.id == ingredientId }
            prefs.setIngredients(normalizeIngredients(remaining, target.recipeId))
        }
    }

    private fun normalizeIngredients(
        all: List<RecipeIngredient>,
        recipeId: String,
    ): List<RecipeIngredient> {
        val forRecipe = all.filter { it.recipeId == recipeId }
        val others = all.filterNot { it.recipeId == recipeId }
        val normalized = Ordering.normalize(
            forRecipe, orderOf = { it.sortOrder }, idOf = { it.id },
            withOrder = { item, order -> item.copy(sortOrder = order) },
        )
        return others + normalized
    }

    private suspend fun reorderIngredients(
        recipeId: String,
        move: (List<RecipeIngredient>) -> List<RecipeIngredient>,
    ) {
        dataStore.edit { prefs ->
            val all = prefs.ingredients()
            val forRecipe = all.filter { it.recipeId == recipeId }
            val others = all.filterNot { it.recipeId == recipeId }
            prefs.setIngredients(others + move(forRecipe))
        }
    }

    suspend fun moveIngredientUp(recipeId: String, id: String) = reorderIngredients(recipeId) {
        Ordering.moveUp(it, id, { i -> i.id }, { i -> i.sortOrder }, { i, o -> i.copy(sortOrder = o) })
    }

    suspend fun moveIngredientDown(recipeId: String, id: String) = reorderIngredients(recipeId) {
        Ordering.moveDown(it, id, { i -> i.id }, { i -> i.sortOrder }, { i, o -> i.copy(sortOrder = o) })
    }

    suspend fun moveIngredientToTop(recipeId: String, id: String) = reorderIngredients(recipeId) {
        Ordering.moveToTop(it, id, { i -> i.id }, { i -> i.sortOrder }, { i, o -> i.copy(sortOrder = o) })
    }

    suspend fun moveIngredientToBottom(recipeId: String, id: String) = reorderIngredients(recipeId) {
        Ordering.moveToBottom(it, id, { i -> i.id }, { i -> i.sortOrder }, { i, o -> i.copy(sortOrder = o) })
    }

    // ---- Preparation steps --------------------------------------------------

    suspend fun addStep(
        recipeId: String,
        title: String,
        instruction: String,
        timerLabel: String,
        note: String,
    ): String? {
        val cleanInstruction = TextLimits.clampMultiline(instruction, TextLimits.STEP_INSTRUCTION)
        if (cleanInstruction.isBlank()) return null // never save blank steps
        val id = Ids.newId()
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val all = prefs.steps()
            val maxOrder = all.filter { it.recipeId == recipeId }.maxOfOrNull { it.sortOrder } ?: -1
            val step = PreparationStep(
                id = id,
                recipeId = recipeId,
                stepNumber = maxOrder + 2,
                title = TextLimits.clamp(title, TextLimits.STEP_TITLE),
                instruction = cleanInstruction,
                timerLabel = timerLabel.trim(),
                note = TextLimits.clampMultiline(note, TextLimits.STEP_NOTE),
                sortOrder = maxOrder + 1,
                createdAt = now,
                updatedAt = now,
            )
            prefs.setSteps(renumberSteps(all + step, recipeId))
        }
        return id
    }

    suspend fun updateStep(updated: PreparationStep) {
        val cleanInstruction = TextLimits.clampMultiline(updated.instruction, TextLimits.STEP_INSTRUCTION)
        if (cleanInstruction.isBlank()) return
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val all = prefs.steps()
            if (all.none { it.id == updated.id }) return@edit
            val clean = updated.copy(
                title = TextLimits.clamp(updated.title, TextLimits.STEP_TITLE),
                instruction = cleanInstruction,
                timerLabel = updated.timerLabel.trim(),
                note = TextLimits.clampMultiline(updated.note, TextLimits.STEP_NOTE),
                updatedAt = now,
            )
            prefs.setSteps(all.map { if (it.id == clean.id) clean else it })
        }
    }

    suspend fun deleteStep(stepId: String) {
        dataStore.edit { prefs ->
            val all = prefs.steps()
            val target = all.firstOrNull { it.id == stepId } ?: return@edit
            prefs.setSteps(renumberSteps(all.filterNot { it.id == stepId }, target.recipeId))
        }
    }

    suspend fun duplicateStep(stepId: String) {
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val all = prefs.steps()
            val original = all.firstOrNull { it.id == stepId } ?: return@edit
            val copy = original.copy(
                id = Ids.newId(),
                sortOrder = original.sortOrder, // placed adjacent, renumber fixes order
                createdAt = now,
                updatedAt = now,
            )
            // Insert just after the original by nudging sort orders.
            val bumped = all.map {
                if (it.recipeId == original.recipeId && it.sortOrder > original.sortOrder) {
                    it.copy(sortOrder = it.sortOrder + 1)
                } else it
            }
            val inserted = bumped + copy.copy(sortOrder = original.sortOrder + 1)
            prefs.setSteps(renumberSteps(inserted, original.recipeId))
        }
    }

    /** Recalculate sortOrder (0..n-1) and stepNumber (1..n) for one recipe's steps. */
    private fun renumberSteps(all: List<PreparationStep>, recipeId: String): List<PreparationStep> {
        val forRecipe = all.filter { it.recipeId == recipeId }
        val others = all.filterNot { it.recipeId == recipeId }
        val ordered = Ordering.sorted(forRecipe, orderOf = { it.sortOrder }, idOf = { it.id })
        val renumbered = ordered.mapIndexed { index, step ->
            step.copy(sortOrder = index, stepNumber = index + 1)
        }
        return others + renumbered
    }

    private suspend fun reorderSteps(
        recipeId: String,
        move: (List<PreparationStep>) -> List<PreparationStep>,
    ) {
        dataStore.edit { prefs ->
            val all = prefs.steps()
            val forRecipe = all.filter { it.recipeId == recipeId }
            val others = all.filterNot { it.recipeId == recipeId }
            val moved = move(forRecipe).mapIndexed { i, s -> s.copy(sortOrder = i, stepNumber = i + 1) }
            prefs.setSteps(others + moved)
        }
    }

    suspend fun moveStepUp(recipeId: String, id: String) = reorderSteps(recipeId) {
        Ordering.moveUp(it, id, { s -> s.id }, { s -> s.sortOrder }, { s, o -> s.copy(sortOrder = o) })
    }

    suspend fun moveStepDown(recipeId: String, id: String) = reorderSteps(recipeId) {
        Ordering.moveDown(it, id, { s -> s.id }, { s -> s.sortOrder }, { s, o -> s.copy(sortOrder = o) })
    }

    suspend fun moveStepToTop(recipeId: String, id: String) = reorderSteps(recipeId) {
        Ordering.moveToTop(it, id, { s -> s.id }, { s -> s.sortOrder }, { s, o -> s.copy(sortOrder = o) })
    }

    suspend fun moveStepToBottom(recipeId: String, id: String) = reorderSteps(recipeId) {
        Ordering.moveToBottom(it, id, { s -> s.id }, { s -> s.sortOrder }, { s, o -> s.copy(sortOrder = o) })
    }

    // ---- Categories ---------------------------------------------------------

    suspend fun addCustomCategory(name: String, colorKey: String): String {
        val id = Ids.newId()
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val current = prefs.categoriesOrNull()
                ?: DefaultData.buildDefaultCategories(now)
            val maxOrder = current.maxOfOrNull { it.sortOrder } ?: -1
            val category = RecipeCategory(
                id = id,
                name = TextLimits.clamp(name, TextLimits.CATEGORY_NAME),
                colorKey = colorKey,
                sortOrder = maxOrder + 1,
                isDefault = false,
                hiddenWhenEmpty = false,
                archived = false,
                createdAt = now,
                updatedAt = now,
            )
            prefs.setCategories(current + category)
        }
        return id
    }

    suspend fun renameCategory(categoryId: String, name: String, colorKey: String?) {
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val current = prefs.categoriesOrNull() ?: return@edit
            prefs.setCategories(
                current.map {
                    if (it.id == categoryId) it.copy(
                        name = TextLimits.clamp(name, TextLimits.CATEGORY_NAME),
                        colorKey = colorKey ?: it.colorKey,
                        updatedAt = now,
                    ) else it
                }
            )
        }
    }

    suspend fun setCategoryHiddenWhenEmpty(categoryId: String, hidden: Boolean) {
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val current = prefs.categoriesOrNull() ?: return@edit
            prefs.setCategories(
                current.map {
                    if (it.id == categoryId) it.copy(hiddenWhenEmpty = hidden, updatedAt = now) else it
                }
            )
        }
    }

    private suspend fun reorderCategoriesInternal(
        move: (List<RecipeCategory>) -> List<RecipeCategory>,
    ) {
        dataStore.edit { prefs ->
            val current = prefs.categoriesOrNull() ?: return@edit
            prefs.setCategories(move(current))
        }
    }

    suspend fun moveCategoryUp(id: String) = reorderCategoriesInternal {
        Ordering.moveUp(it, id, { c -> c.id }, { c -> c.sortOrder }, { c, o -> c.copy(sortOrder = o) })
    }

    suspend fun moveCategoryDown(id: String) = reorderCategoriesInternal {
        Ordering.moveDown(it, id, { c -> c.id }, { c -> c.sortOrder }, { c, o -> c.copy(sortOrder = o) })
    }

    /**
     * Delete a custom category only if unused. Default categories are never deleted.
     * Returns false when the category is a default or still has recipes.
     */
    suspend fun deleteUnusedCategory(categoryId: String): Boolean {
        var success = false
        dataStore.edit { prefs ->
            val current = prefs.categoriesOrNull() ?: return@edit
            val category = current.firstOrNull { it.id == categoryId } ?: return@edit
            if (category.isDefault) return@edit
            val used = prefs.recipes().any { it.categoryId == categoryId }
            if (used) return@edit
            prefs.setCategories(current.filterNot { it.id == categoryId })
            success = true
        }
        return success
    }

    /** Reassign all recipes from one category to another (used before deleting). */
    suspend fun reassignRecipes(fromCategoryId: String, toCategoryId: String) {
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val recipes = prefs.recipes()
            prefs.setRecipes(
                recipes.map {
                    if (it.categoryId == fromCategoryId)
                        it.copy(categoryId = toCategoryId, updatedAt = now)
                    else it
                }
            )
        }
    }

    /** Re-add any missing default categories without duplicating existing ones. */
    suspend fun restoreDefaultCategories() {
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val current = prefs.categoriesOrNull() ?: emptyList()
            val existingIds = current.map { it.id }.toSet()
            val toAdd = DefaultData.buildDefaultCategories(now)
                .filter { it.id !in existingIds }
            // Also un-hide restored defaults that already exist but were hidden.
            val unhidden = current.map {
                if (it.isDefault) it.copy(hiddenWhenEmpty = false) else it
            }
            prefs.setCategories(unhidden + toAdd)
        }
    }

    // ---- Shopping list ------------------------------------------------------

    suspend fun addShoppingItems(items: List<ShoppingItem>) {
        if (items.isEmpty()) return
        dataStore.edit { prefs ->
            prefs.setShopping(prefs.shopping() + items)
        }
    }

    suspend fun addCustomShoppingItem(
        title: String,
        quantityLabel: String,
        category: ShoppingCategory,
        note: String,
    ): String {
        val id = Ids.newId()
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val item = ShoppingItem(
                id = id,
                title = TextLimits.clamp(title, TextLimits.SHOPPING_TITLE),
                quantityLabel = TextLimits.clamp(quantityLabel, TextLimits.INGREDIENT_QUANTITY),
                category = category,
                sourceRecipeId = null,
                sourceIngredientId = null,
                checked = false,
                note = TextLimits.clampMultiline(note, TextLimits.INGREDIENT_NOTE),
                createdAt = now,
                updatedAt = now,
            )
            prefs.setShopping(prefs.shopping() + item)
        }
        return id
    }

    suspend fun updateShoppingItem(updated: ShoppingItem) {
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val current = prefs.shopping()
            if (current.none { it.id == updated.id }) return@edit
            val clean = updated.copy(
                title = TextLimits.clamp(updated.title, TextLimits.SHOPPING_TITLE),
                quantityLabel = TextLimits.clamp(updated.quantityLabel, TextLimits.INGREDIENT_QUANTITY),
                note = TextLimits.clampMultiline(updated.note, TextLimits.INGREDIENT_NOTE),
                updatedAt = now,
            )
            prefs.setShopping(current.map { if (it.id == clean.id) clean else it })
        }
    }

    suspend fun setShoppingChecked(itemId: String, checked: Boolean) {
        val now = TimeStamps.nowIso()
        dataStore.edit { prefs ->
            val current = prefs.shopping()
            prefs.setShopping(
                current.map { if (it.id == itemId) it.copy(checked = checked, updatedAt = now) else it }
            )
        }
    }

    suspend fun deleteShoppingItem(itemId: String) {
        dataStore.edit { prefs ->
            prefs.setShopping(prefs.shopping().filterNot { it.id == itemId })
        }
    }

    suspend fun clearCheckedShoppingItems() {
        dataStore.edit { prefs ->
            prefs.setShopping(prefs.shopping().filterNot { it.checked })
        }
    }

    suspend fun deleteAllShoppingItems() {
        dataStore.edit { prefs -> prefs.setShopping(emptyList()) }
    }

    // ---- Settings -----------------------------------------------------------

    private suspend fun mutateSettings(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs -> prefs.setSettings(transform(prefs.settings())) }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) =
        mutateSettings { it.copy(onboardingCompleted = completed) }

    suspend fun setDefaultCategory(categoryId: String?) =
        mutateSettings { it.copy(defaultCategoryId = categoryId) }

    suspend fun setCardDensity(density: RecipeCardDensity) =
        mutateSettings { it.copy(cardDensity = density) }

    suspend fun setShowRecentlyOpened(show: Boolean) =
        mutateSettings { it.copy(showRecentlyOpened = show) }

    suspend fun setFavoritesFirst(value: Boolean) =
        mutateSettings { it.copy(favoritesFirst = value) }

    suspend fun setShoppingGenerationMode(mode: ShoppingGenerationMode) =
        mutateSettings { it.copy(shoppingGenerationMode = mode) }

    // ---- Destructive operations --------------------------------------------

    suspend fun deleteAllRecipes() {
        dataStore.edit { prefs ->
            prefs.setRecipes(emptyList())
            prefs.setIngredients(emptyList())
            prefs.setSteps(emptyList())
        }
    }

    /** Permanently remove all archived recipes and their content. */
    suspend fun deleteAllArchivedRecipes() {
        dataStore.edit { prefs ->
            val archivedIds = prefs.recipes().filter { it.archived }.map { it.id }.toSet()
            if (archivedIds.isEmpty()) return@edit
            prefs.setRecipes(prefs.recipes().filterNot { it.id in archivedIds })
            prefs.setIngredients(prefs.ingredients().filterNot { it.recipeId in archivedIds })
            prefs.setSteps(prefs.steps().filterNot { it.recipeId in archivedIds })
        }
    }

    /** Full reset: clears all keys and re-seeds default categories. */
    suspend fun resetAllData() {
        dataStore.edit { prefs ->
            prefs.clear()
            val now = TimeStamps.nowIso()
            prefs.setCategories(DefaultData.buildDefaultCategories(now))
            prefs.setSettings(AppSettings(defaultCategoryId = DefaultData.FIRST_DEFAULT_CATEGORY_ID))
        }
    }

    // ---- Helpers ------------------------------------------------------------

    private fun sanitizeMinutes(value: Int?): Int? {
        if (value == null) return null
        return value.coerceIn(0, com.cooknivo.app.util.TimeInput.MAX_MINUTES)
    }
}
