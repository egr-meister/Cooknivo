package com.cooknivo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooknivo.app.model.Recipe
import com.cooknivo.app.ui.components.ConfirmDialog
import com.cooknivo.app.ui.components.EmptyState
import com.cooknivo.app.ui.theme.CardCream
import com.cooknivo.app.ui.theme.DeepText
import com.cooknivo.app.ui.theme.DividerColor
import com.cooknivo.app.ui.theme.SecondaryText
import com.cooknivo.app.util.RecipeCalc
import com.cooknivo.app.viewmodel.CooknivoViewModel

@Composable
fun ArchiveScreen(
    viewModel: CooknivoViewModel,
    onOpenRecipe: (String) -> Unit,
    onBack: () -> Unit,
) {
    val data by viewModel.appData.collectAsStateWithLifecycle()
    val archived = viewModel.archivedRecipes(data)
        .sortedByDescending { com.cooknivo.app.util.TimeStamps.epochMillisOrZero(it.updatedAt) }
    var deleteTarget by remember { mutableStateOf<Recipe?>(null) }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "Delete this archived recipe permanently?",
            message = "This action cannot be undone.",
            confirmLabel = "Delete permanently",
            onConfirm = { viewModel.deleteRecipe(target.id); deleteTarget = null },
            onDismiss = { deleteTarget = null },
        )
    }

    Scaffold(topBar = { CooknivoTopBar(title = "Archive", onBack = onBack) }) { padding ->
        if (archived.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                EmptyState(
                    icon = Icons.Filled.Archive,
                    title = "No archived recipes.",
                    body = "Archived recipes appear here and can be restored.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(archived, key = { it.id }) { recipe ->
                    val cat = viewModel.categoryName(data, recipe)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(CardCream, RoundedCornerShape(10.dp))
                            .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                    ) {
                        Text(recipe.name.ifBlank { "Untitled" },
                            style = MaterialTheme.typography.titleMedium, color = DeepText)
                        Text("$cat · ${RecipeCalc.totalTimeLabel(recipe)}",
                            style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(onClick = { viewModel.setArchived(recipe.id, false) },
                                modifier = Modifier.weight(1f)) { Text("Restore") }
                            OutlinedButton(onClick = { viewModel.duplicateRecipe(recipe.id) },
                                modifier = Modifier.weight(1f)) { Text("Duplicate") }
                        }
                        OutlinedButton(
                            onClick = { deleteTarget = recipe },
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        ) { Text("Delete permanently", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}
