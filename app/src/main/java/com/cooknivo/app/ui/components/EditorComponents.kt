package com.cooknivo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cooknivo.app.ui.theme.CardCream
import com.cooknivo.app.ui.theme.DeepText
import com.cooknivo.app.ui.theme.DisabledColor
import com.cooknivo.app.ui.theme.DividerColor
import com.cooknivo.app.ui.theme.PaperWhite
import com.cooknivo.app.ui.theme.RecipeTerracotta
import com.cooknivo.app.ui.theme.SecondaryText

/** Group header used to separate ingredient groups (e.g. Dough, Filling). */
@Composable
fun IngredientGroupHeader(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        style = MaterialTheme.typography.labelLarge,
        color = RecipeTerracotta,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp)
            .semantics { contentDescription = "Ingredient group: $name" },
    )
}

/** A single ingredient line: name — quantity, with optional note. */
@Composable
fun IngredientRow(
    name: String,
    quantityLabel: String,
    note: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val readout = buildString {
        append(name)
        if (quantityLabel.isNotBlank()) append(", $quantityLabel")
        if (note.isNotBlank()) append(". Note: $note")
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .semantics { contentDescription = readout },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .padding(end = 10.dp)
                .size(6.dp)
                .background(RecipeTerracotta, CircleShape),
        )
        Column(Modifier.weight(1f)) {
            Row {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DeepText,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (quantityLabel.isNotBlank()) {
                    Text(
                        text = " — $quantityLabel",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SecondaryText,
                    )
                }
            }
            if (note.isNotBlank()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                )
            }
        }
        trailing?.invoke()
    }
}

/** A numbered preparation step strip. */
@Composable
fun StepStrip(
    stepNumber: Int,
    title: String,
    instruction: String,
    timerLabel: String,
    note: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val readout = buildString {
        append("Step $stepNumber. ")
        if (title.isNotBlank()) append("$title. ")
        append(instruction)
        if (timerLabel.isNotBlank()) append(". Timer: $timerLabel")
        if (note.isNotBlank()) append(". Note: $note")
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .background(PaperWhite, RoundedCornerShape(8.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
            .padding(10.dp)
            .semantics { contentDescription = readout },
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(end = 10.dp)
                .size(26.dp)
                .background(RecipeTerracotta, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stepNumber.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = PaperWhite,
            )
        }
        Column(Modifier.weight(1f)) {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = DeepText,
                )
            }
            Text(
                text = instruction,
                style = MaterialTheme.typography.bodyMedium,
                color = DeepText,
            )
            if (timerLabel.isNotBlank()) {
                Text(
                    text = "Timer: $timerLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = RecipeTerracotta,
                )
            }
            if (note.isNotBlank()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                )
            }
        }
        trailing?.invoke()
    }
}

/** Stable Move Up/Down/Top/Bottom controls used for ingredients and steps. */
@Composable
fun ReorderControls(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveTop: () -> Unit,
    onMoveBottom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        ReorderIcon(Icons.Filled.KeyboardDoubleArrowUp, "Move to top", canMoveUp, onMoveTop)
        ReorderIcon(Icons.Filled.KeyboardArrowUp, "Move up", canMoveUp, onMoveUp)
        ReorderIcon(Icons.Filled.KeyboardArrowDown, "Move down", canMoveDown, onMoveDown)
        ReorderIcon(Icons.Filled.KeyboardDoubleArrowDown, "Move to bottom", canMoveDown, onMoveBottom)
    }
}

@Composable
private fun ReorderIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) DeepText else DisabledColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** A small colored pill/chip used for tags such as time, category, or serving. */
@Composable
fun InfoPill(text: String, color: Color = CardCream, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color, RoundedCornerShape(50))
            .border(1.dp, DividerColor, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = DeepText)
    }
}
