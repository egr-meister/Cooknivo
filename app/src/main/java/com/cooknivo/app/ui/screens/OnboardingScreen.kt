package com.cooknivo.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cooknivo.app.ui.Disclaimers
import com.cooknivo.app.ui.components.DisclaimerNote
import com.cooknivo.app.ui.components.PaperSection
import com.cooknivo.app.ui.theme.AppBackground
import com.cooknivo.app.ui.theme.DeepText
import com.cooknivo.app.ui.theme.PaperWhite
import com.cooknivo.app.ui.theme.RecipeTerracotta
import com.cooknivo.app.ui.theme.SecondaryText

/**
 * First-launch onboarding. Explains the personal recipe box, manual creation,
 * offline storage, and the no-online-recipes / no-nutrition disclaimers.
 * No mascot is used.
 */
@Composable
fun OnboardingScreen(
    onCreateFirstRecipe: () -> Unit,
    onExploreEmptyBox: () -> Unit,
) {
    Scaffold(containerColor = AppBackground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Cooknivo",
                style = MaterialTheme.typography.headlineMedium,
                color = RecipeTerracotta,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Build your own private recipe box.",
                style = MaterialTheme.typography.titleMedium,
                color = DeepText,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))

            PaperSection(title = "A personal recipe-card box") {
                Text(
                    "Save ingredients, steps, cooking times, and personal notes. " +
                        "Organize recipes with category dividers and favorites.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepText,
                )
            }
            Spacer(Modifier.height(10.dp))

            PaperSection(title = "How it works") {
                Column {
                    OnboardingPoint("Create recipes manually — you enter everything.")
                    OnboardingPoint("Add ingredients and ordered preparation steps.")
                    OnboardingPoint("Use category divider tabs and mark favorites.")
                    OnboardingPoint("Search your collection locally.")
                    OnboardingPoint("Create a shopping list from ingredients you entered.")
                    OnboardingPoint("Your recipes stay on this device.")
                }
            }
            Spacer(Modifier.height(10.dp))

            DisclaimerNote(
                "Cooknivo does not provide online recipes, diets, calorie calculations, " +
                    "nutritional advice, or medical guidance."
            )
            Spacer(Modifier.height(8.dp))
            DisclaimerNote(Disclaimers.MANUAL_RECIPE)

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onCreateFirstRecipe,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RecipeTerracotta,
                    contentColor = PaperWhite,
                ),
            ) { Text("Create First Recipe") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onExploreEmptyBox,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Explore Empty Box") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun OnboardingPoint(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = "•  $text",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText,
        )
    }
}
