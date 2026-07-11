package com.cooknivo.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooknivo.app.ui.components.EmptyState
import com.cooknivo.app.ui.components.RecipeIndexCard
import com.cooknivo.app.util.RecipeSort
import com.cooknivo.app.viewmodel.CooknivoViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: CooknivoViewModel,
    onOpenRecipe: (String) -> Unit,
    onBack: () -> Unit,
) {
    val data by viewModel.appData.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val categories = viewModel.allCategoriesSorted(data)

    Scaffold(
        topBar = { CooknivoTopBar(title = "Search", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                label = { Text("Search recipes, ingredients, steps, notes") },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )

            // Filters
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = filters.favoritesOnly,
                    onClick = { viewModel.updateFilters { it.copy(favoritesOnly = !it.favoritesOnly) } },
                    label = { Text("Favorites") },
                )
                FilterChip(
                    selected = filters.includeArchived,
                    onClick = { viewModel.updateFilters { it.copy(includeArchived = !it.includeArchived) } },
                    label = { Text("Include archived") },
                )
                FilterChip(
                    selected = filters.hasTime,
                    onClick = { viewModel.updateFilters { it.copy(hasTime = !it.hasTime) } },
                    label = { Text("Has time") },
                )
                FilterChip(
                    selected = filters.hasIngredients,
                    onClick = { viewModel.updateFilters { it.copy(hasIngredients = !it.hasIngredients) } },
                    label = { Text("Has ingredients") },
                )
                FilterChip(
                    selected = filters.hasSteps,
                    onClick = { viewModel.updateFilters { it.copy(hasSteps = !it.hasSteps) } },
                    label = { Text("Has steps") },
                )
                FilterChip(
                    selected = filters.recentlyOpened,
                    onClick = { viewModel.updateFilters { it.copy(recentlyOpened = !it.recentlyOpened) } },
                    label = { Text("Recently opened") },
                )
            }

            // Category filter chips
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp)) {
                FilterChip(
                    selected = filters.categoryId == null,
                    onClick = { viewModel.updateFilters { it.copy(categoryId = null) } },
                    label = { Text("All") },
                    modifier = Modifier.padding(end = 6.dp),
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = filters.categoryId == cat.id,
                        onClick = {
                            viewModel.updateFilters {
                                it.copy(categoryId = if (it.categoryId == cat.id) null else cat.id)
                            }
                        },
                        label = { Text(cat.name) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }

            // Sort chips
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp)) {
                sortOptions.forEach { (label, sort) ->
                    FilterChip(
                        selected = filters.sort == sort,
                        onClick = { viewModel.updateFilters { it.copy(sort = sort) } },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            if (results.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = "No matching recipes.",
                    body = "Try a different search or adjust your filters.",
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(results, key = { it.id }) { recipe ->
                        RecipeIndexCard(
                            data = buildCardData(data, recipe),
                            onClick = { onOpenRecipe(recipe.id) },
                        )
                    }
                }
            }
        }
    }
}

private val sortOptions = listOf(
    "Recently updated" to RecipeSort.RecentlyUpdated,
    "Recently created" to RecipeSort.RecentlyCreated,
    "Recently opened" to RecipeSort.RecentlyOpened,
    "Name" to RecipeSort.Name,
    "Total time" to RecipeSort.TotalTime,
    "Ingredients" to RecipeSort.IngredientCount,
    "Steps" to RecipeSort.StepCount,
)
