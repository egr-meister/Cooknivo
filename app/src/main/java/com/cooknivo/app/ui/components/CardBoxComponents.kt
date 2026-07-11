package com.cooknivo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cooknivo.app.ui.theme.CardCream
import com.cooknivo.app.ui.theme.DeepText
import com.cooknivo.app.ui.theme.DividerColor
import com.cooknivo.app.ui.theme.DividerPaper
import com.cooknivo.app.ui.theme.FavoriteBookmark
import com.cooknivo.app.ui.theme.PaperWhite
import com.cooknivo.app.ui.theme.SecondaryText
import com.cooknivo.app.ui.theme.categoryColor

/** Horizontal strip of category divider tabs. The active tab is raised/darker. */
@Composable
fun CategoryDividerTabs(
    tabs: List<CategoryTabData>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        DividerTab(
            label = "All",
            color = DividerPaper,
            selected = selectedId == null,
            onClick = { onSelect(null) },
        )
        tabs.forEach { tab ->
            DividerTab(
                label = if (tab.count > 0) "${tab.name} (${tab.count})" else tab.name,
                color = categoryColor(tab.colorKey),
                selected = selectedId == tab.id,
                onClick = { onSelect(tab.id) },
            )
        }
    }
}

data class CategoryTabData(val id: String, val name: String, val colorKey: String, val count: Int)

@Composable
private fun DividerTab(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val raise = if (selected) 0.dp else 6.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(raise))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(color)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) DeepText else DividerColor,
                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = if (selected) 10.dp else 8.dp)
                .semantics {
                    contentDescription =
                        if (selected) "$label category, selected" else "$label category"
                },
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = DeepText,
                maxLines = 1,
            )
        }
    }
}

/** A single physical-style index card for a recipe. */
@Composable
fun RecipeIndexCard(
    data: RecipeCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    raised: Boolean = false,
    compact: Boolean = false,
) {
    val contentDesc = buildString {
        append("Recipe ${data.name}. Category ${data.categoryName}. ")
        append("${data.totalTime}. ${data.ingredientCount} ingredients, ${data.stepCount} steps.")
        if (data.favorite) append(" Marked as favorite.")
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (raised) 0.dp else 4.dp)
            .background(if (raised) PaperWhite else CardCream, RoundedCornerShape(10.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = contentDesc },
    ) {
        // Left ruled margin line, evoking an index card.
        Box(
            Modifier
                .padding(start = 10.dp)
                .width(2.dp)
                .height(if (compact) 56.dp else 84.dp)
                .background(categoryColor(data.colorKey))
                .align(Alignment.CenterStart),
        )
        Column(Modifier.padding(start = 22.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = data.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (data.favorite) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = FavoriteBookmark,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = data.categoryName + "  •  " + data.totalTime,
                style = MaterialTheme.typography.labelMedium,
                color = SecondaryText,
            )
            if (!compact) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${data.ingredientCount} ingredients · ${data.stepCount} steps",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                )
                if (data.notePreview.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = data.notePreview,
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

data class RecipeCardData(
    val id: String,
    val name: String,
    val categoryName: String,
    val colorKey: String,
    val totalTime: String,
    val ingredientCount: Int,
    val stepCount: Int,
    val favorite: Boolean,
    val notePreview: String,
)

/** The "Add Recipe" card — an empty index card with a plus. */
@Composable
fun AddRecipeCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(PaperWhite, RoundedCornerShape(10.dp))
            .border(2.dp, categoryColor("main"), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
            .semantics { contentDescription = "Add a new recipe card" },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = DeepText)
            Spacer(Modifier.width(8.dp))
            Text(
                "Add Recipe",
                style = MaterialTheme.typography.titleMedium,
                color = DeepText,
            )
        }
    }
}
