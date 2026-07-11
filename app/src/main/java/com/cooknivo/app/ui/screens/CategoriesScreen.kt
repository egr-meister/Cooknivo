package com.cooknivo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooknivo.app.model.RecipeCategory
import com.cooknivo.app.ui.components.CooknivoScaffold
import com.cooknivo.app.ui.components.ConfirmDialog
import com.cooknivo.app.ui.navigation.BottomDestination
import com.cooknivo.app.ui.navigation.Routes
import com.cooknivo.app.ui.theme.CardCream
import com.cooknivo.app.ui.theme.DeepText
import com.cooknivo.app.ui.theme.DisabledColor
import com.cooknivo.app.ui.theme.DividerColor
import com.cooknivo.app.ui.theme.RecipeTerracotta
import com.cooknivo.app.ui.theme.SecondaryText
import com.cooknivo.app.ui.theme.categoryColor
import com.cooknivo.app.ui.theme.categoryColorKeys
import com.cooknivo.app.viewmodel.CooknivoViewModel

@Composable
fun CategoriesScreen(
    viewModel: CooknivoViewModel,
    onOpenCategory: (String) -> Unit,
    onSelectDestination: (BottomDestination) -> Unit,
) {
    val data by viewModel.appData.collectAsStateWithLifecycle()
    val categories = viewModel.allCategoriesSorted(data)
    var showAdd by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<RecipeCategory?>(null) }
    var deleteTarget by remember { mutableStateOf<RecipeCategory?>(null) }

    if (showAdd) {
        CategoryEditDialog(
            title = "Add Category",
            initialName = "",
            initialColorKey = "other",
            onConfirm = { name, color -> viewModel.addCustomCategory(name, color); showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
    renameTarget?.let { target ->
        CategoryEditDialog(
            title = "Rename Category",
            initialName = target.name,
            initialColorKey = target.colorKey,
            allowNameEdit = !target.isDefault,
            onConfirm = { name, color -> viewModel.renameCategory(target.id, name, color); renameTarget = null },
            onDismiss = { renameTarget = null },
        )
    }
    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "Delete category?",
            message = "\"${target.name}\" has no recipes and will be removed.",
            confirmLabel = "Delete",
            onConfirm = { viewModel.deleteUnusedCategory(target.id); deleteTarget = null },
            onDismiss = { deleteTarget = null },
        )
    }

    CooknivoScaffold(
        currentRoute = Routes.CATEGORIES,
        onSelectDestination = onSelectDestination,
        topBar = {
            CooknivoTopBar(title = "Categories", actions = {
                IconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add category")
                }
            })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
        ) {
            items(categories, key = { it.id }) { category ->
                val active = viewModel.activeCountForCategory(data, category.id)
                val fav = viewModel.favoriteCountForCategory(data, category.id)
                val archived = viewModel.archivedCountForCategory(data, category.id)
                CategoryDividerRow(
                    category = category,
                    activeCount = active,
                    favoriteCount = fav,
                    archivedCount = archived,
                    onOpen = { onOpenCategory(category.id) },
                    onRename = { renameTarget = category },
                    onMoveUp = { viewModel.moveCategoryUp(category.id) },
                    onMoveDown = { viewModel.moveCategoryDown(category.id) },
                    onToggleHide = { viewModel.setCategoryHiddenWhenEmpty(category.id, it) },
                    onDelete = if (!category.isDefault && active == 0 && archived == 0) {
                        { deleteTarget = category }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun CategoryDividerRow(
    category: RecipeCategory,
    activeCount: Int,
    favoriteCount: Int,
    archivedCount: Int,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleHide: (Boolean) -> Unit,
    onDelete: (() -> Unit)?,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(CardCream, RoundedCornerShape(10.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(16.dp).background(categoryColor(category.colorKey), CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.name, style = MaterialTheme.typography.titleMedium, color = DeepText)
                    if (category.isDefault) {
                        Spacer(Modifier.width(6.dp))
                        Text("default", style = MaterialTheme.typography.labelMedium, color = SecondaryText)
                    }
                }
                Text(
                    "$activeCount recipes · $favoriteCount favorite · $archivedCount archived",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                )
            }
            IconButton(onClick = onMoveUp) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
            }
            IconButton(onClick = onMoveDown) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onRename) { Text("Rename") }
            Spacer(Modifier.weight(1f))
            Text("Hide if empty", style = MaterialTheme.typography.labelMedium, color = SecondaryText)
            Switch(checked = category.hiddenWhenEmpty, onCheckedChange = onToggleHide)
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete category",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditDialog(
    title: String,
    initialName: String,
    initialColorKey: String,
    allowNameEdit: Boolean = true,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableStateOf(initialColorKey) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 60) name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    enabled = allowNameEdit,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text("Tab color", style = MaterialTheme.typography.labelLarge, color = SecondaryText)
                Spacer(Modifier.height(6.dp))
                Row {
                    categoryColorKeys.forEach { key ->
                        Box(
                            Modifier
                                .padding(end = 8.dp)
                                .size(28.dp)
                                .background(categoryColor(key), CircleShape)
                                .border(
                                    width = if (color == key) 3.dp else 1.dp,
                                    color = if (color == key) DeepText else DividerColor,
                                    shape = CircleShape,
                                )
                                .clickable { color = key },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.trim().isNotEmpty()) onConfirm(name.trim(), color) },
                enabled = name.trim().isNotEmpty(),
            ) { Text("Save", color = RecipeTerracotta) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
