package com.cooknivo.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cooknivo.app.model.AppData
import com.cooknivo.app.model.Recipe
import com.cooknivo.app.ui.components.RecipeCardData
import com.cooknivo.app.ui.theme.CardCream
import com.cooknivo.app.ui.theme.DeepText
import com.cooknivo.app.util.RecipeCalc

/** Build display data for a recipe index card from the current data snapshot. */
fun buildCardData(data: AppData, recipe: Recipe): RecipeCardData {
    val category = data.categories.firstOrNull { it.id == recipe.categoryId }
    val ingredientCount = data.ingredients.count { it.recipeId == recipe.id }
    val stepCount = data.preparationSteps.count { it.recipeId == recipe.id }
    return RecipeCardData(
        id = recipe.id,
        name = recipe.name.ifBlank { "Untitled Recipe" },
        categoryName = category?.name
            ?: recipe.customCategoryName.ifBlank { com.cooknivo.app.model.Fallbacks.UNCATEGORIZED },
        colorKey = category?.colorKey ?: "other",
        totalTime = RecipeCalc.totalTimeLabel(recipe),
        ingredientCount = ingredientCount,
        stepCount = stepCount,
        favorite = recipe.favorite,
        notePreview = RecipeCalc.notePreview(recipe.notes),
    )
}

/** Standard top app bar with an optional back button and actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CooknivoTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = { Row { actions() } },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CardCream,
            titleContentColor = DeepText,
            navigationIconContentColor = DeepText,
            actionIconContentColor = DeepText,
        ),
    )
}

/** A friendly fallback shown when a requested record is missing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingRecordScreen(message: String, onBack: () -> Unit) {
    androidx.compose.material3.Scaffold(
        topBar = { CooknivoTopBar(title = "Not found", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text(message, style = MaterialTheme.typography.titleMedium, color = DeepText)
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.Button(onClick = onBack) { Text("Go back") }
        }
    }
}
