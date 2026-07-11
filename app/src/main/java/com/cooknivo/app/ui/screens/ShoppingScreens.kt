package com.cooknivo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooknivo.app.model.ShoppingCategory
import com.cooknivo.app.model.ShoppingGenerationMode
import com.cooknivo.app.model.ShoppingItem
import com.cooknivo.app.ui.components.CooknivoScaffold
import com.cooknivo.app.ui.components.EmptyState
import com.cooknivo.app.ui.components.PaperSection
import com.cooknivo.app.ui.navigation.BottomDestination
import com.cooknivo.app.ui.navigation.Routes
import com.cooknivo.app.ui.theme.DeepText
import com.cooknivo.app.ui.theme.DividerColor
import com.cooknivo.app.ui.theme.PaperWhite
import com.cooknivo.app.ui.theme.RecipeTerracotta
import com.cooknivo.app.ui.theme.SecondaryText
import com.cooknivo.app.ui.theme.ShoppingChecked
import com.cooknivo.app.viewmodel.CooknivoViewModel

@Composable
fun ShoppingListScreen(
    viewModel: CooknivoViewModel,
    onSelectDestination: (BottomDestination) -> Unit,
) {
    val data by viewModel.appData.collectAsStateWithLifecycle()
    val items = data.shoppingItems
    val unchecked = items.filterNot { it.checked }
    val checked = items.filter { it.checked }
    var showAdd by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }

    if (showAdd) {
        AddShoppingItemDialog(
            onConfirm = { title, qty, cat, note ->
                viewModel.addCustomShoppingItem(title, qty, cat, note); showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }
    if (showClear) {
        com.cooknivo.app.ui.components.ConfirmDialog(
            title = "Clear checked items?",
            message = "This removes all checked shopping items.",
            confirmLabel = "Clear",
            onConfirm = { viewModel.clearCheckedShoppingItems(); showClear = false },
            onDismiss = { showClear = false },
        )
    }

    CooknivoScaffold(
        currentRoute = Routes.SHOPPING,
        onSelectDestination = onSelectDestination,
        topBar = {
            CooknivoTopBar(title = "Shopping List", actions = {
                IconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add custom item")
                }
            })
        },
    ) { padding ->
        if (items.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                EmptyState(
                    icon = Icons.Filled.ShoppingCart,
                    title = "Your shopping list is empty.",
                    body = "Add a custom item, or add ingredients from a recipe.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                if (unchecked.isNotEmpty()) {
                    item { SlipHeader("To buy (${unchecked.size})") }
                    // Group by shopping category.
                    ShoppingCategory.entries.forEach { category ->
                        val group = unchecked.filter { it.category == category }
                        if (group.isNotEmpty()) {
                            item { CategoryLabel(category.displayName) }
                            items(group, key = { it.id }) { item ->
                                ShoppingRow(
                                    item = item,
                                    sourceLabel = sourceLabel(viewModel, data, item),
                                    onToggle = { viewModel.setShoppingChecked(item.id, it) },
                                    onDelete = { viewModel.deleteShoppingItem(item.id) },
                                )
                            }
                        }
                    }
                }
                if (checked.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            SlipHeader("Checked (${checked.size})")
                            TextButton(onClick = { showClear = true }) {
                                Text("Clear checked", color = RecipeTerracotta)
                            }
                        }
                    }
                    items(checked, key = { it.id }) { item ->
                        ShoppingRow(
                            item = item,
                            sourceLabel = sourceLabel(viewModel, data, item),
                            onToggle = { viewModel.setShoppingChecked(item.id, it) },
                            onDelete = { viewModel.deleteShoppingItem(item.id) },
                        )
                    }
                }
            }
        }
    }
}

private fun sourceLabel(
    viewModel: CooknivoViewModel,
    data: com.cooknivo.app.model.AppData,
    item: ShoppingItem,
): String {
    val recipeId = item.sourceRecipeId ?: return ""
    val recipe = viewModel.recipeById(data, recipeId)
    return recipe?.name ?: com.cooknivo.app.model.Fallbacks.DELETED_RECIPE
}

@Composable
private fun SlipHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = DeepText)
}

@Composable
private fun CategoryLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = RecipeTerracotta,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun ShoppingRow(
    item: ShoppingItem,
    sourceLabel: String,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(PaperWhite, RoundedCornerShape(6.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.checked, onCheckedChange = onToggle)
        Column(Modifier.weight(1f)) {
            Row {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.checked) SecondaryText else DeepText,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.quantityLabel.isNotBlank()) {
                    Text(
                        " — ${item.quantityLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                    )
                }
            }
            if (sourceLabel.isNotBlank()) {
                Text("from $sourceLabel", style = MaterialTheme.typography.labelMedium, color = SecondaryText)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete item", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddShoppingItemDialog(
    onConfirm: (String, String, ShoppingCategory, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ShoppingCategory.Other) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Shopping Item") },
        text = {
            Column {
                OutlinedTextField(
                    value = title, onValueChange = { if (it.length <= 150) title = it },
                    label = { Text("Item *") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = qty, onValueChange = { if (it.length <= 80) qty = it },
                    label = { Text("Quantity") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                ShoppingCategoryDropdown(selected = category, onSelect = { category = it })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note, onValueChange = { if (it.length <= 300) note = it },
                    label = { Text("Note") }, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.trim().isNotEmpty()) onConfirm(title.trim(), qty, category, note) },
                enabled = title.trim().isNotEmpty(),
            ) { Text("Add", color = RecipeTerracotta) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---- Shopping Preview screen -------------------------------------------------

@Composable
fun ShoppingPreviewScreen(
    viewModel: CooknivoViewModel,
    recipeId: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val data by viewModel.appData.collectAsStateWithLifecycle()
    val recipe = viewModel.recipeById(data, recipeId)
    if (recipe == null) {
        MissingRecordScreen(message = "This recipe is no longer available.", onBack = onBack)
        return
    }
    val ingredients = viewModel.ingredientsFor(data, recipeId)
    val mode = data.settings.shoppingGenerationMode

    // Selection state, defaulted per generation mode / default flags.
    val selected = remember {
        mutableStateMapOf<String, Boolean>().apply {
            ingredients.forEach { ing ->
                this[ing.id] = when (mode) {
                    ShoppingGenerationMode.AddAll -> true
                    else -> ing.addToShoppingByDefault
                }
            }
        }
    }

    val selectedIds = selected.filterValues { it }.keys
    val preview = viewModel.buildShoppingPreview(data, recipeId, selectedIds)
    val missingCount = preview.count { !it.alreadyInList }

    Scaffold(
        topBar = { CooknivoTopBar(title = "Add to Shopping", onBack = onBack) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
            Spacer(Modifier.height(8.dp))
            PaperSection(title = recipe.name) {
                Text(
                    "Select the ingredients you entered to add to your shopping list. " +
                        "Quantities are kept as text; nothing is combined or converted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (ingredients.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.ShoppingCart,
                    title = "No ingredients to add",
                    body = "This recipe has no ingredients yet.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(ingredients, key = { it.id }) { ing ->
                        val already = data.shoppingItems.any {
                            it.title.trim().equals(ing.name.trim(), ignoreCase = true)
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(PaperWhite, RoundedCornerShape(6.dp))
                                .border(1.dp, DividerColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = selected[ing.id] ?: false,
                                onCheckedChange = { selected[ing.id] = it },
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    buildString {
                                        append(ing.name)
                                        if (ing.quantityLabel.isNotBlank()) append(" — ${ing.quantityLabel}")
                                    },
                                    style = MaterialTheme.typography.bodyLarge, color = DeepText,
                                )
                                Text(
                                    ing.shoppingCategory.displayName +
                                        if (already) " · already in list" else "",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (already) ShoppingChecked else SecondaryText,
                                )
                            }
                        }
                    }
                }

                Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            viewModel.confirmShoppingPreview(recipeId, com.cooknivo.app.util.ShoppingGen.missingOnly(preview), allowDuplicates = false)
                            onDone()
                        },
                        enabled = missingCount > 0,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RecipeTerracotta, contentColor = PaperWhite),
                    ) { Text("Add Missing ($missingCount)") }
                    OutlinedButton(
                        onClick = {
                            viewModel.confirmShoppingPreview(recipeId, preview, allowDuplicates = true)
                            onDone()
                        },
                        enabled = preview.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Add All (${preview.size})") }
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                }
            }
        }
    }
}
