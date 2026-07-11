package com.cooknivo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooknivo.app.model.RecipeCardDensity
import com.cooknivo.app.ui.components.AddRecipeCard
import com.cooknivo.app.ui.components.CategoryDividerTabs
import com.cooknivo.app.ui.components.CategoryTabData
import com.cooknivo.app.ui.components.CooknivoScaffold
import com.cooknivo.app.ui.components.EmptyState
import com.cooknivo.app.ui.components.RecipeIndexCard
import com.cooknivo.app.ui.components.ShoppingListSlip
import com.cooknivo.app.ui.navigation.BottomDestination
import com.cooknivo.app.ui.navigation.Routes
import com.cooknivo.app.ui.theme.DeepWood
import com.cooknivo.app.ui.theme.DividerColor
import com.cooknivo.app.ui.theme.SecondaryText
import com.cooknivo.app.ui.theme.WarmWood
import com.cooknivo.app.viewmodel.CooknivoViewModel

@Composable
fun RecipeBoxScreen(
    viewModel: CooknivoViewModel,
    onOpenRecipe: (String) -> Unit,
    onAddRecipe: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenShopping: () -> Unit,
    onSelectDestination: (BottomDestination) -> Unit,
) {
    val data by viewModel.appData.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.filters.collectAsStateWithLifecycle()

    val visibleCategories = viewModel.visibleCategories(data)
    val activeCounts = viewModel.activeRecipes(data).groupingBy { it.categoryId }.eachCount()
    val tabs = visibleCategories.map {
        CategoryTabData(it.id, it.name, it.colorKey, activeCounts[it.id] ?: 0)
    }

    val homeFilterCategory = selectedCategory.categoryId
    val activeRecipes = viewModel.activeRecipes(data)
        .let { list -> homeFilterCategory?.let { c -> list.filter { it.categoryId == c } } ?: list }
        .sortedByDescending { com.cooknivo.app.util.TimeStamps.epochMillisOrZero(it.updatedAt) }

    val recentlyOpened = if (data.settings.showRecentlyOpened)
        viewModel.recentlyOpened(data, 5) else emptyList()
    val remainingShopping = data.shoppingItems.count { !it.checked }
    val compact = data.settings.cardDensity == RecipeCardDensity.Compact

    CooknivoScaffold(
        currentRoute = Routes.RECIPE_BOX,
        onSelectDestination = onSelectDestination,
        topBar = {
            CooknivoTopBar(
                title = "Cooknivo",
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search recipes")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            CategoryDividerTabs(
                tabs = tabs,
                selectedId = homeFilterCategory,
                onSelect = { id -> viewModel.updateFilters { it.copy(categoryId = id) } },
            )
            // The card box frame (warm wood) that contains the recipe cards.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .background(WarmWood, RoundedCornerShape(14.dp))
                    .border(3.dp, DeepWood, RoundedCornerShape(14.dp))
                    .padding(8.dp),
            ) {
                if (activeRecipes.isEmpty()) {
                    EmptyBox(onAddRecipe = onAddRecipe, filtered = homeFilterCategory != null)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (recentlyOpened.isNotEmpty()) {
                            item { BoxSectionLabel("Recently Opened") }
                            items(recentlyOpened, key = { "recent_" + it.id }) { recipe ->
                                RecipeIndexCard(
                                    data = buildCardData(data, recipe),
                                    onClick = { onOpenRecipe(recipe.id) },
                                    raised = true,
                                    compact = true,
                                )
                            }
                            item { Spacer(Modifier.height(4.dp)) }
                            item { BoxSectionLabel("All Recipes") }
                        }
                        itemsIndexed(activeRecipes) { index, recipe ->
                            RecipeIndexCard(
                                data = buildCardData(data, recipe),
                                onClick = { onOpenRecipe(recipe.id) },
                                raised = index == 0,
                                compact = compact,
                            )
                        }
                        item {
                            Spacer(Modifier.height(4.dp))
                            AddRecipeCard(onClick = onAddRecipe)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            ShoppingListSlip(
                remaining = remainingShopping,
                onClick = onOpenShopping,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BoxSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = com.cooknivo.app.ui.theme.PaperWhite,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun EmptyBox(onAddRecipe: () -> Unit, filtered: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyState(
            icon = Icons.Filled.Inventory2,
            title = if (filtered) "No recipes in this category" else "Your recipe box is empty.",
            body = if (filtered) "Add a recipe here, or pick another divider tab."
            else "Create your first personal recipe.",
        )
        Spacer(Modifier.height(12.dp))
        AddRecipeCard(onClick = onAddRecipe, modifier = Modifier.padding(horizontal = 8.dp))
    }
}
