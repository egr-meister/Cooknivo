package com.cooknivo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooknivo.app.model.PreparationStep
import com.cooknivo.app.model.Recipe
import com.cooknivo.app.model.RecipeIngredient
import com.cooknivo.app.model.ShoppingCategory
import com.cooknivo.app.ui.Disclaimers
import com.cooknivo.app.ui.components.DisclaimerNote
import com.cooknivo.app.ui.components.PaperSection
import com.cooknivo.app.ui.components.ReorderControls
import com.cooknivo.app.ui.theme.DividerColor
import com.cooknivo.app.ui.theme.PaperWhite
import com.cooknivo.app.ui.theme.RecipeTerracotta
import com.cooknivo.app.ui.theme.SecondaryText
import com.cooknivo.app.util.Ids
import com.cooknivo.app.util.TextLimits
import com.cooknivo.app.util.TimeInput
import com.cooknivo.app.viewmodel.CooknivoViewModel

/**
 * One scrollable editor with clearly separated paper-card sections, used for
 * both Add and Edit. All child state (ingredients/steps) is held locally and
 * persisted atomically on Save so nothing is lost between sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorScreen(
    viewModel: CooknivoViewModel,
    editingRecipeId: String?,
    initialCategoryId: String?,
    onSaved: (String) -> Unit,
    onBack: () -> Unit,
) {
    val data by viewModel.appData.collectAsStateWithLifecycle()
    val categories = viewModel.allCategoriesSorted(data)

    val existing = editingRecipeId?.let { id -> data.recipes.firstOrNull { it.id == id } }
    // Missing record fallback while editing.
    if (editingRecipeId != null && existing == null) {
        MissingRecordScreen(
            message = "This recipe is no longer available. It may have been deleted.",
            onBack = onBack,
        )
        return
    }

    val defaultCategory = initialCategoryId
        ?: data.settings.defaultCategoryId
        ?: categories.firstOrNull()?.id
        ?: ""

    // ---- Local form state (loaded once) ----
    var loaded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf(defaultCategory) }
    var description by remember { mutableStateOf("") }
    var prep by remember { mutableStateOf("") }
    var cook by remember { mutableStateOf("") }
    var rest by remember { mutableStateOf("") }
    var serving by remember { mutableStateOf("") }
    var favorite by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    val ingredients = remember { mutableStateListOf<RecipeIngredient>() }
    val steps = remember { mutableStateListOf<PreparationStep>() }

    if (!loaded) {
        if (existing != null) {
            name = existing.name
            categoryId = existing.categoryId.ifBlank { defaultCategory }
            description = existing.description
            prep = existing.preparationTimeMinutes?.toString() ?: ""
            cook = existing.cookingTimeMinutes?.toString() ?: ""
            rest = existing.restingTimeMinutes?.toString() ?: ""
            serving = existing.servingLabel
            favorite = existing.favorite
            notes = existing.notes
            ingredients.clear()
            ingredients.addAll(viewModel.ingredientsFor(data, existing.id))
            steps.clear()
            steps.addAll(viewModel.stepsFor(data, existing.id))
        }
        loaded = true
    }

    val nameValid = name.trim().isNotEmpty()
    val categoryValid = categoryId.isNotBlank() && categories.any { it.id == categoryId }
    val canSave = nameValid && categoryValid

    fun save() {
        val draft = Recipe(
            id = existing?.id ?: "",
            name = name,
            categoryId = categoryId,
            customCategoryName = categories.firstOrNull { it.id == categoryId }?.name ?: "",
            description = description,
            preparationTimeMinutes = TimeInput.parseMinutes(prep),
            cookingTimeMinutes = TimeInput.parseMinutes(cook),
            restingTimeMinutes = TimeInput.parseMinutes(rest),
            servingLabel = serving,
            favorite = favorite,
            archived = existing?.archived ?: false,
            notes = notes,
            createdAt = existing?.createdAt ?: "",
            updatedAt = "",
            lastOpenedAt = existing?.lastOpenedAt ?: "",
        )
        viewModel.saveRecipeDraft(draft, ingredients.toList(), steps.toList()) { id -> onSaved(id) }
    }

    Scaffold(
        topBar = {
            CooknivoTopBar(
                title = if (existing == null) "Add Recipe" else "Edit Recipe",
                onBack = onBack,
                actions = {
                    TextButton(onClick = { if (canSave) save() }, enabled = canSave) {
                        Text("Save", color = if (canSave) RecipeTerracotta else SecondaryText)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ---- Section 1: Recipe details ----
            PaperSection(title = "Recipe Details") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= TextLimits.RECIPE_NAME) name = it },
                        label = { Text("Recipe name *") },
                        isError = !nameValid,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CategoryDropdown(
                        categories = categories.map { it.id to it.name },
                        selectedId = categoryId,
                        onSelect = { categoryId = it },
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= TextLimits.DESCRIPTION) description = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MinuteField("Prep (min)", prep, { prep = it }, Modifier.weight(1f))
                        MinuteField("Cook (min)", cook, { cook = it }, Modifier.weight(1f))
                        MinuteField("Rest (min)", rest, { rest = it }, Modifier.weight(1f))
                    }
                    OutlinedTextField(
                        value = serving,
                        onValueChange = { if (it.length <= 80) serving = it },
                        label = { Text("Serving label (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = favorite, onCheckedChange = { favorite = it })
                        Spacer(Modifier.width(8.dp))
                        Text("Mark as favorite", color = com.cooknivo.app.ui.theme.DeepText)
                    }
                }
            }

            // ---- Section 2: Ingredients ----
            PaperSection(
                title = "Ingredients",
                trailing = {
                    TextButton(onClick = {
                        ingredients.add(
                            RecipeIngredient(id = Ids.newId(), recipeId = "", name = "")
                        )
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add ingredient")
                        Text("Add")
                    }
                },
            ) {
                if (ingredients.isEmpty()) {
                    Text("No ingredients yet.", color = SecondaryText,
                        style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ingredients.forEachIndexed { index, ing ->
                            IngredientEditor(
                                ingredient = ing,
                                index = index,
                                count = ingredients.size,
                                onChange = { ingredients[index] = it },
                                onDelete = { ingredients.removeAt(index) },
                                onMoveUp = { if (index > 0) ingredients.add(index - 1, ingredients.removeAt(index)) },
                                onMoveDown = { if (index < ingredients.size - 1) ingredients.add(index + 1, ingredients.removeAt(index)) },
                                onMoveTop = { ingredients.add(0, ingredients.removeAt(index)) },
                                onMoveBottom = { ingredients.add(ingredients.removeAt(index)) },
                            )
                        }
                    }
                }
            }

            // ---- Section 3: Preparation steps ----
            PaperSection(
                title = "Preparation Steps",
                trailing = {
                    TextButton(onClick = {
                        steps.add(PreparationStep(id = Ids.newId(), recipeId = "", instruction = ""))
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add step")
                        Text("Add")
                    }
                },
            ) {
                if (steps.isEmpty()) {
                    Text("No steps yet.", color = SecondaryText,
                        style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        steps.forEachIndexed { index, step ->
                            StepEditor(
                                step = step,
                                index = index,
                                count = steps.size,
                                onChange = { steps[index] = it },
                                onDelete = { steps.removeAt(index) },
                                onDuplicate = {
                                    steps.add(index + 1, step.copy(id = Ids.newId()))
                                },
                                onMoveUp = { if (index > 0) steps.add(index - 1, steps.removeAt(index)) },
                                onMoveDown = { if (index < steps.size - 1) steps.add(index + 1, steps.removeAt(index)) },
                                onMoveTop = { steps.add(0, steps.removeAt(index)) },
                                onMoveBottom = { steps.add(steps.removeAt(index)) },
                            )
                        }
                    }
                }
            }

            // ---- Section 4: Notes and save ----
            PaperSection(title = "Notes and Save") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { if (it.length <= TextLimits.RECIPE_NOTES) notes = it },
                        label = { Text("Personal notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                    Text(
                        "${notes.length} / ${TextLimits.RECIPE_NOTES} characters",
                        style = MaterialTheme.typography.labelMedium,
                        color = SecondaryText,
                        modifier = Modifier.align(Alignment.End),
                    )
                    DisclaimerNote(Disclaimers.MANUAL_RECIPE)
                    Button(
                        onClick = { if (canSave) save() },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RecipeTerracotta, contentColor = PaperWhite,
                        ),
                    ) { Text(if (existing == null) "Save Recipe" else "Update Recipe") }
                    if (!canSave) {
                        Text(
                            "A recipe name and category are required.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MinuteField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() }.take(5)
            onChange(filtered)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.first == selectedId }?.second ?: "Select category *"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { (id, catName) ->
                DropdownMenuItem(
                    text = { Text(catName) },
                    onClick = { onSelect(id); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientEditor(
    ingredient: RecipeIngredient,
    index: Int,
    count: Int,
    onChange: (RecipeIngredient) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveTop: () -> Unit,
    onMoveBottom: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(PaperWhite, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("#${index + 1}", style = MaterialTheme.typography.labelLarge, color = RecipeTerracotta)
            Spacer(Modifier.weight(1f))
            ReorderControls(
                canMoveUp = index > 0, canMoveDown = index < count - 1,
                onMoveUp = onMoveUp, onMoveDown = onMoveDown,
                onMoveTop = onMoveTop, onMoveBottom = onMoveBottom,
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ingredient",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
        OutlinedTextField(
            value = ingredient.name,
            onValueChange = { if (it.length <= TextLimits.INGREDIENT_NAME) onChange(ingredient.copy(name = it)) },
            label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ingredient.quantityLabel,
                onValueChange = { if (it.length <= TextLimits.INGREDIENT_QUANTITY) onChange(ingredient.copy(quantityLabel = it)) },
                label = { Text("Quantity") }, singleLine = true, modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = ingredient.groupName,
                onValueChange = { if (it.length <= 60) onChange(ingredient.copy(groupName = it)) },
                label = { Text("Group") }, singleLine = true, modifier = Modifier.weight(1f),
            )
        }
        ShoppingCategoryDropdown(
            selected = ingredient.shoppingCategory,
            onSelect = { onChange(ingredient.copy(shoppingCategory = it)) },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = ingredient.addToShoppingByDefault,
                onCheckedChange = { onChange(ingredient.copy(addToShoppingByDefault = it)) },
            )
            Spacer(Modifier.width(8.dp))
            Text("Add to shopping by default",
                style = MaterialTheme.typography.bodyMedium,
                color = com.cooknivo.app.ui.theme.DeepText)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepEditor(
    step: PreparationStep,
    index: Int,
    count: Int,
    onChange: (PreparationStep) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveTop: () -> Unit,
    onMoveBottom: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(PaperWhite, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Step ${index + 1}", style = MaterialTheme.typography.labelLarge, color = RecipeTerracotta)
            Spacer(Modifier.weight(1f))
            ReorderControls(
                canMoveUp = index > 0, canMoveDown = index < count - 1,
                onMoveUp = onMoveUp, onMoveDown = onMoveDown,
                onMoveTop = onMoveTop, onMoveBottom = onMoveBottom,
            )
        }
        OutlinedTextField(
            value = step.title,
            onValueChange = { if (it.length <= TextLimits.STEP_TITLE) onChange(step.copy(title = it)) },
            label = { Text("Title (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = step.instruction,
            onValueChange = { if (it.length <= TextLimits.STEP_INSTRUCTION) onChange(step.copy(instruction = it)) },
            label = { Text("Instruction *") }, modifier = Modifier.fillMaxWidth(), minLines = 2,
        )
        OutlinedTextField(
            value = step.timerLabel,
            onValueChange = { if (it.length <= 60) onChange(step.copy(timerLabel = it)) },
            label = { Text("Timer label (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Row {
            OutlinedButton(onClick = onDuplicate) { Text("Duplicate") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingCategoryDropdown(
    selected: ShoppingCategory,
    onSelect: (ShoppingCategory) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Shopping category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ShoppingCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName) },
                    onClick = { onSelect(category); expanded = false },
                )
            }
        }
    }
}
