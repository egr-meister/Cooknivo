package com.cooknivo.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooknivo.app.ui.components.CooknivoScaffold
import com.cooknivo.app.ui.components.EmptyState
import com.cooknivo.app.ui.components.RecipeIndexCard
import com.cooknivo.app.ui.navigation.BottomDestination
import com.cooknivo.app.ui.navigation.Routes
import com.cooknivo.app.viewmodel.CooknivoViewModel

@Composable
fun FavoritesScreen(
    viewModel: CooknivoViewModel,
    onOpenRecipe: (String) -> Unit,
    onSelectDestination: (BottomDestination) -> Unit,
) {
    val data by viewModel.appData.collectAsStateWithLifecycle()
    val favorites = viewModel.favorites(data)
        .sortedByDescending { com.cooknivo.app.util.TimeStamps.epochMillisOrZero(it.updatedAt) }

    CooknivoScaffold(
        currentRoute = Routes.FAVORITES,
        onSelectDestination = onSelectDestination,
        topBar = { CooknivoTopBar(title = "Favorites") },
    ) { padding ->
        if (favorites.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                EmptyState(
                    icon = Icons.Filled.BookmarkBorder,
                    title = "No favorite recipes yet.",
                    body = "Open a recipe and tap the bookmark to add it here.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
            ) {
                items(favorites, key = { it.id }) { recipe ->
                    RecipeIndexCard(
                        data = buildCardData(data, recipe),
                        onClick = { onOpenRecipe(recipe.id) },
                    )
                }
            }
        }
    }
}
