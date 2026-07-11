package com.cooknivo.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooknivo.app.ui.components.AddRecipeCard
import com.cooknivo.app.ui.components.EmptyState
import com.cooknivo.app.ui.components.PaperSection
import com.cooknivo.app.ui.components.RecipeIndexCard
import com.cooknivo.app.ui.theme.DeepText
import com.cooknivo.app.ui.theme.SecondaryText
import com.cooknivo.app.viewmodel.CooknivoViewModel

@Composable
fun CategoryDetailScreen(
    viewModel: CooknivoViewModel,
    categoryId: String,
    onOpenRecipe: (String) -> Unit,
    onAddRecipe: (String) -> Unit,
    onBack: () -> Unit,
) {
    val data by viewModel.appData.collectAsStateWithLifecycle()
    val category = viewModel.categoryById(data, categoryId)

    if (category == null) {
        MissingRecordScreen(message = "This category is no longer available.", onBack = onBack)
        return
    }

    val recipes = viewModel.activeRecipes(data)
        .filter { it.categoryId == categoryId }
        .sortedByDescending { com.cooknivo.app.util.TimeStamps.epochMillisOrZero(it.updatedAt) }
    val activeCount = viewModel.activeCountForCategory(data, categoryId)
    val favCount = viewModel.favoriteCountForCategory(data, categoryId)
    val archivedCount = viewModel.archivedCountForCategory(data, categoryId)

    Scaffold(
        topBar = {
            CooknivoTopBar(title = category.name, onBack = onBack, actions = {
                IconButton(onClick = { onAddRecipe(categoryId) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add recipe to category")
                }
            })
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
            Spacer(Modifier.height(8.dp))
            PaperSection(title = "Overview") {
                Text(
                    "$activeCount active recipes · $favCount favorite · $archivedCount archived",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (recipes.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = "No recipes here yet",
                    body = "Add a recipe to this category.",
                )
                Spacer(Modifier.height(8.dp))
                AddRecipeCard(onClick = { onAddRecipe(categoryId) })
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(recipes, key = { it.id }) { recipe ->
                        RecipeIndexCard(
                            data = buildCardData(data, recipe),
                            onClick = { onOpenRecipe(recipe.id) },
                        )
                    }
                    item {
                        Spacer(Modifier.height(4.dp))
                        AddRecipeCard(onClick = { onAddRecipe(categoryId) })
                    }
                }
            }
        }
    }
}
