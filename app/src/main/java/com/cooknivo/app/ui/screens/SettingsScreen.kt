package com.cooknivo.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.cooknivo.app.model.RecipeCardDensity
import com.cooknivo.app.model.ShoppingGenerationMode
import com.cooknivo.app.ui.Disclaimers
import com.cooknivo.app.ui.components.CooknivoScaffold
import com.cooknivo.app.ui.components.ConfirmDialog
import com.cooknivo.app.ui.components.DisclaimerNote
import com.cooknivo.app.ui.components.PaperSection
import com.cooknivo.app.ui.navigation.BottomDestination
import com.cooknivo.app.ui.navigation.Routes
import com.cooknivo.app.ui.theme.DeepText
import com.cooknivo.app.ui.theme.RecipeTerracotta
import com.cooknivo.app.ui.theme.SecondaryText
import com.cooknivo.app.viewmodel.CooknivoViewModel

@Composable
fun SettingsScreen(
    viewModel: CooknivoViewModel,
    onOpenStatistics: () -> Unit,
    onOpenArchive: () -> Unit,
    onShowOnboarding: () -> Unit,
    onSelectDestination: (BottomDestination) -> Unit,
) {
    val data by viewModel.appData.collectAsStateWithLifecycle()
    val settings = data.settings
    val categories = viewModel.allCategoriesSorted(data)

    var confirm by remember { mutableStateOf<ConfirmAction?>(null) }

    confirm?.let { action ->
        ConfirmDialog(
            title = action.title,
            message = action.message,
            confirmLabel = action.confirmLabel,
            onConfirm = { action.run(); confirm = null },
            onDismiss = { confirm = null },
        )
    }

    CooknivoScaffold(
        currentRoute = Routes.SETTINGS,
        onSelectDestination = onSelectDestination,
        topBar = { CooknivoTopBar(title = "Settings") },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ---- Preferences ----
            PaperSection(title = "Preferences") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DefaultCategoryPref(
                        categories = categories.map { it.id to it.name },
                        selectedId = settings.defaultCategoryId,
                        onSelect = { viewModel.setDefaultCategory(it) },
                    )
                    Divider()
                    Text("Recipe card density", style = MaterialTheme.typography.labelLarge, color = SecondaryText)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.cardDensity == RecipeCardDensity.Comfortable,
                            onClick = { viewModel.setCardDensity(RecipeCardDensity.Comfortable) },
                            label = { Text("Comfortable") },
                        )
                        FilterChip(
                            selected = settings.cardDensity == RecipeCardDensity.Compact,
                            onClick = { viewModel.setCardDensity(RecipeCardDensity.Compact) },
                            label = { Text("Compact") },
                        )
                    }
                    Divider()
                    SwitchRow("Show recently opened section", settings.showRecentlyOpened) {
                        viewModel.setShowRecentlyOpened(it)
                    }
                    SwitchRow("Favorites-first sorting", settings.favoritesFirst) {
                        viewModel.setFavoritesFirst(it)
                    }
                    Divider()
                    Text("Default shopping generation", style = MaterialTheme.typography.labelLarge, color = SecondaryText)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShoppingModeChip("Preview First", ShoppingGenerationMode.PreviewFirst, settings.shoppingGenerationMode) {
                            viewModel.setShoppingGenerationMode(it)
                        }
                        ShoppingModeChip("Add Missing", ShoppingGenerationMode.AddMissing, settings.shoppingGenerationMode) {
                            viewModel.setShoppingGenerationMode(it)
                        }
                        ShoppingModeChip("Add All", ShoppingGenerationMode.AddAll, settings.shoppingGenerationMode) {
                            viewModel.setShoppingGenerationMode(it)
                        }
                    }
                }
            }

            // ---- More ----
            PaperSection(title = "More") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onOpenStatistics, modifier = Modifier.fillMaxWidth()) {
                        Text("Collection Statistics")
                    }
                    OutlinedButton(onClick = onOpenArchive, modifier = Modifier.fillMaxWidth()) {
                        Text("Archived Recipes")
                    }
                    OutlinedButton(onClick = onShowOnboarding, modifier = Modifier.fillMaxWidth()) {
                        Text("Show onboarding again")
                    }
                    OutlinedButton(
                        onClick = { viewModel.restoreDefaultCategories() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Restore default categories") }
                }
            }

            // ---- Data management ----
            PaperSection(title = "Data") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.clearCheckedShoppingItems() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Clear checked shopping items") }
                    DangerButton("Delete archived recipes") {
                        confirm = ConfirmAction(
                            "Delete archived recipes?",
                            "This permanently removes all archived recipes and their content.",
                            "Delete",
                        ) { viewModel.deleteAllArchivedRecipes() }
                    }
                    DangerButton("Delete shopping list") {
                        confirm = ConfirmAction(
                            "Delete shopping list?",
                            "This removes every item from your shopping list.",
                            "Delete",
                        ) { viewModel.deleteAllShoppingItems() }
                    }
                    DangerButton("Delete all recipes") {
                        confirm = ConfirmAction(
                            "Delete all recipes?",
                            "This permanently removes every recipe, ingredient, and preparation step.",
                            "Delete all",
                        ) { viewModel.deleteAllRecipes() }
                    }
                    DangerButton("Reset all local data") {
                        confirm = ConfirmAction(
                            "Reset all local data?",
                            "This will permanently remove every recipe, ingredient, preparation step, " +
                                "category, favorite, note, shopping item, and setting stored by Cooknivo.",
                            "Reset everything",
                        ) { viewModel.resetAllData() }
                    }
                }
            }

            // ---- About & disclaimers ----
            PaperSection(title = "About Cooknivo") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Cooknivo — a personal, offline recipe organizer. Version 1.0.0.",
                        style = MaterialTheme.typography.bodyMedium, color = DeepText,
                    )
                    DisclaimerNote(Disclaimers.MANUAL_RECIPE)
                    DisclaimerNote(Disclaimers.INGREDIENT_SAFETY)
                    Text("Privacy", style = MaterialTheme.typography.labelLarge, color = SecondaryText)
                    DisclaimerNote(Disclaimers.PRIVACY)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private data class ConfirmAction(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val run: () -> Unit,
)

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = DeepText, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ShoppingModeChip(
    label: String,
    mode: ShoppingGenerationMode,
    current: ShoppingGenerationMode,
    onSelect: (ShoppingGenerationMode) -> Unit,
) {
    FilterChip(selected = current == mode, onClick = { onSelect(mode) }, label = { Text(label) })
}

@Composable
private fun DangerButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun DefaultCategoryPref(
    categories: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.first == selectedId }?.second ?: "None"
    Row(
        Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Default category", style = MaterialTheme.typography.bodyMedium, color = DeepText)
        Column {
            TextButton(onClick = { expanded = true }) {
                Text(selectedName, color = RecipeTerracotta)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                categories.forEach { (id, name) ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false })
                }
            }
        }
    }
}
