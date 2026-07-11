package com.cooknivo.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooknivo.app.ui.Disclaimers
import com.cooknivo.app.ui.components.ConfirmDialog
import com.cooknivo.app.ui.components.DetailLine
import com.cooknivo.app.ui.components.DisclaimerNote
import com.cooknivo.app.ui.components.InfoPill
import com.cooknivo.app.ui.components.IngredientGroupHeader
import com.cooknivo.app.ui.components.IngredientRow
import com.cooknivo.app.ui.components.PaperSection
import com.cooknivo.app.ui.components.StepStrip
import com.cooknivo.app.ui.theme.CardCream
import com.cooknivo.app.ui.theme.DeepText
import com.cooknivo.app.ui.theme.FavoriteBookmark
import com.cooknivo.app.ui.theme.RecipeTerracotta
import com.cooknivo.app.ui.theme.SecondaryText
import com.cooknivo.app.util.RecipeCalc
import com.cooknivo.app.util.TimeStamps
import com.cooknivo.app.viewmodel.CooknivoViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailScreen(
    viewModel: CooknivoViewModel,
    recipeId: String,
    onEdit: (String) -> Unit,
    onAddToShopping: (String) -> Unit,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val data by viewModel.appData.collectAsStateWithLifecycle()
    val recipe = data.recipes.firstOrNull { it.id == recipeId }

    // Mark opened once when the screen appears.
    LaunchedEffect(recipeId) { viewModel.markOpened(recipeId) }

    if (recipe == null) {
        MissingRecordScreen(
            message = "This recipe is no longer available. It may have been deleted.",
            onBack = onBack,
        )
        return
    }

    var showDelete by remember { mutableStateOf(false) }
    val ingredients = viewModel.ingredientsFor(data, recipeId)
    val steps = viewModel.stepsFor(data, recipeId)
    val categoryName = viewModel.categoryName(data, recipe)

    if (showDelete) {
        ConfirmDialog(
            title = "Delete this recipe?",
            message = "This will permanently remove the recipe, ingredients, preparation steps, and notes stored for it.",
            confirmLabel = "Delete",
            onConfirm = { viewModel.deleteRecipe(recipeId); onDeleted() },
            onDismiss = { showDelete = false },
        )
    }

    Scaffold(
        topBar = {
            CooknivoTopBar(
                title = "Recipe",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite(recipeId) }) {
                        Icon(
                            imageVector = if (recipe.favorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (recipe.favorite) "Remove favorite" else "Mark favorite",
                            tint = FavoriteBookmark,
                        )
                    }
                    IconButton(onClick = { onEdit(recipeId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit recipe")
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
            // Title card
            PaperSection(title = recipe.name.ifBlank { "Untitled Recipe" }) {
                Column {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoPill(categoryName, com.cooknivo.app.ui.theme.categoryColor(
                            data.categories.firstOrNull { it.id == recipe.categoryId }?.colorKey ?: "other"
                        ).copy(alpha = 0.35f))
                        if (recipe.favorite) InfoPill("★ Favorite", CardCream)
                        InfoPill(RecipeCalc.totalTimeLabel(recipe))
                    }
                    if (recipe.description.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(recipe.description, style = MaterialTheme.typography.bodyMedium, color = DeepText)
                    }
                }
            }

            // Times & serving
            PaperSection(title = "Times & Serving") {
                Column {
                    DetailLine("Preparation", RecipeCalc.formatMinutes(recipe.preparationTimeMinutes))
                    DetailLine("Cooking", RecipeCalc.formatMinutes(recipe.cookingTimeMinutes))
                    DetailLine("Resting", RecipeCalc.formatMinutes(recipe.restingTimeMinutes))
                    DetailLine("Total", RecipeCalc.totalTimeLabel(recipe))
                    DetailLine("Serving", recipe.servingLabel)
                }
            }

            // Ingredients grouped
            PaperSection(title = "Ingredients (${ingredients.size})") {
                if (ingredients.isEmpty()) {
                    Text("No ingredients added.", color = SecondaryText,
                        style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column {
                        var lastGroup: String? = null
                        ingredients.forEach { ing ->
                            val group = ing.groupName.trim()
                            if (group.isNotBlank() && group != lastGroup) {
                                IngredientGroupHeader(group)
                                lastGroup = group
                            } else if (group.isBlank()) {
                                lastGroup = null
                            }
                            IngredientRow(ing.name, ing.quantityLabel, ing.note)
                        }
                    }
                }
            }

            // Steps numbered
            PaperSection(title = "Preparation Steps (${steps.size})") {
                if (steps.isEmpty()) {
                    Text("No preparation steps added.", color = SecondaryText,
                        style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        steps.forEachIndexed { index, step ->
                            StepStrip(
                                stepNumber = index + 1,
                                title = step.title,
                                instruction = step.instruction,
                                timerLabel = step.timerLabel,
                                note = step.note,
                            )
                        }
                    }
                }
            }

            // Notes
            if (recipe.notes.isNotBlank()) {
                PaperSection(title = "Notes") {
                    Text(recipe.notes, style = MaterialTheme.typography.bodyMedium, color = DeepText)
                }
            }

            Text(
                "Updated ${TimeStamps.dateLabel(recipe.updatedAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = SecondaryText,
            )

            DisclaimerNote(Disclaimers.RECIPE_DETAIL)

            // Actions
            PaperSection(title = "Actions") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onEdit(recipeId) }, modifier = Modifier.weight(1f)) {
                            Text("Edit")
                        }
                        OutlinedButton(
                            onClick = { viewModel.duplicateRecipe(recipeId) },
                            modifier = Modifier.weight(1f),
                        ) { Text("Duplicate") }
                    }
                    OutlinedButton(
                        onClick = { onAddToShopping(recipeId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Add ingredients to shopping list") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.setArchived(recipeId, true); onBack() },
                            modifier = Modifier.weight(1f),
                        ) { Text("Archive") }
                        OutlinedButton(
                            onClick = { showDelete = true },
                            modifier = Modifier.weight(1f),
                        ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
