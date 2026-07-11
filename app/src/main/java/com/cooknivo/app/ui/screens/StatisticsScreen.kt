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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooknivo.app.ui.components.PaperSection
import com.cooknivo.app.ui.theme.CardCream
import com.cooknivo.app.ui.theme.DeepText
import com.cooknivo.app.ui.theme.RecipeTerracotta
import com.cooknivo.app.ui.theme.SecondaryText
import com.cooknivo.app.viewmodel.CooknivoViewModel
import java.util.Locale

@Composable
fun StatisticsScreen(
    viewModel: CooknivoViewModel,
    onBack: () -> Unit,
) {
    val data by viewModel.appData.collectAsStateWithLifecycle()
    val stats = viewModel.stats(data)

    Scaffold(topBar = { CooknivoTopBar(title = "Statistics", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PaperSection(title = "Collection") {
                Column {
                    StatLine("Total active recipes", stats.totalActive.toString())
                    StatLine("Favorites", stats.favorites.toString())
                    StatLine("Archived", stats.archived.toString())
                    StatLine("With ingredients", stats.withIngredients.toString())
                    StatLine("With preparation steps", stats.withSteps.toString())
                    StatLine("Average ingredients", String.format(Locale.US, "%.1f", stats.averageIngredients))
                    StatLine("Average steps", String.format(Locale.US, "%.1f", stats.averageSteps))
                    StatLine("Recently added (14 days)", stats.recentlyAdded.toString())
                    StatLine("Shopping items remaining", stats.shoppingRemaining.toString())
                }
            }

            PaperSection(title = "Recipes by Category") {
                val maxCount = (stats.perCategory.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    stats.perCategory.forEach { cat ->
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cat.name, style = MaterialTheme.typography.bodyMedium, color = DeepText)
                                Text(cat.count.toString(), style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
                            }
                            // Compose-built count column (no chart library).
                            Box(
                                Modifier.fillMaxWidth().height(8.dp)
                                    .background(CardCream, RoundedCornerShape(4.dp)),
                            ) {
                                val fraction = cat.count.toFloat() / maxCount.toFloat()
                                Box(
                                    Modifier
                                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                        .height(8.dp)
                                        .background(RecipeTerracotta, RoundedCornerShape(4.dp)),
                                )
                            }
                        }
                    }
                }
            }

            PaperSection(title = "Favorite ratio") {
                val ratio = if (stats.totalActive == 0) 0f
                else stats.favorites.toFloat() / stats.totalActive.toFloat()
                Box(
                    Modifier.fillMaxWidth().height(14.dp).background(CardCream, RoundedCornerShape(7.dp)),
                ) {
                    Box(
                        Modifier.fillMaxWidth(ratio.coerceIn(0f, 1f)).height(14.dp)
                            .background(com.cooknivo.app.ui.theme.FavoriteBookmark, RoundedCornerShape(7.dp)),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${stats.favorites} of ${stats.totalActive} recipes are favorites",
                    style = MaterialTheme.typography.labelMedium, color = SecondaryText,
                )
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = DeepText)
        Text(value, style = MaterialTheme.typography.titleSmall, color = RecipeTerracotta)
    }
}
