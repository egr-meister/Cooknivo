package com.cooknivo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooknivo.app.ui.navigation.BottomDestination
import com.cooknivo.app.ui.theme.CardCream
import com.cooknivo.app.ui.theme.DeepText
import com.cooknivo.app.ui.theme.DividerColor
import com.cooknivo.app.ui.theme.RecipeTerracotta
import com.cooknivo.app.ui.theme.SecondaryText

private fun iconFor(dest: BottomDestination): ImageVector = when (dest) {
    BottomDestination.Recipes -> Icons.Filled.Dashboard
    BottomDestination.Favorites -> Icons.Filled.Bookmark
    BottomDestination.Shopping -> Icons.Filled.ShoppingCart
    BottomDestination.Categories -> Icons.Filled.Style
    BottomDestination.Settings -> Icons.Filled.Settings
}

/** App scaffold with the bottom navigation bar shared by top-level tabs. */
@Composable
fun CooknivoScaffold(
    currentRoute: String,
    onSelectDestination: (BottomDestination) -> Unit,
    topBar: @Composable () -> Unit = {},
    floating: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = topBar,
        floatingActionButton = floating,
        bottomBar = {
            NavigationBar(containerColor = CardCream) {
                BottomDestination.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = { onSelectDestination(dest) },
                        icon = { Icon(iconFor(dest), contentDescription = dest.label) },
                        label = { Text(dest.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RecipeTerracotta,
                            selectedTextColor = RecipeTerracotta,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText,
                        ),
                    )
                }
            }
        },
        content = content,
    )
}

/** The narrow paper "shopping-list slip" shown below the recipe box. */
@Composable
fun ShoppingListSlip(
    remaining: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics {
                contentDescription = "Shopping list slip. $remaining items remaining. Opens shopping list."
            },
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = RecipeTerracotta,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text("Shopping List", style = MaterialTheme.typography.titleSmall, color = DeepText)
        }
        Text(
            text = if (remaining == 0) "All done" else "$remaining to buy",
            style = MaterialTheme.typography.labelLarge,
            color = if (remaining == 0) com.cooknivo.app.ui.theme.ShoppingChecked else com.cooknivo.app.ui.theme.ShoppingPending,
        )
    }
}
