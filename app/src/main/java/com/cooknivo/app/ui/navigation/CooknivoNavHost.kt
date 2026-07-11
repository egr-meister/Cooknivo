package com.cooknivo.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cooknivo.app.ui.screens.ArchiveScreen
import com.cooknivo.app.ui.screens.CategoriesScreen
import com.cooknivo.app.ui.screens.CategoryDetailScreen
import com.cooknivo.app.ui.screens.FavoritesScreen
import com.cooknivo.app.ui.screens.OnboardingScreen
import com.cooknivo.app.ui.screens.RecipeBoxScreen
import com.cooknivo.app.ui.screens.RecipeDetailScreen
import com.cooknivo.app.ui.screens.RecipeEditorScreen
import com.cooknivo.app.ui.screens.SearchScreen
import com.cooknivo.app.ui.screens.SettingsScreen
import com.cooknivo.app.ui.screens.ShoppingListScreen
import com.cooknivo.app.ui.screens.ShoppingPreviewScreen
import com.cooknivo.app.ui.screens.StatisticsScreen
import com.cooknivo.app.viewmodel.CooknivoViewModel

/** Navigate to a top-level bottom-nav destination, keeping a single back entry. */
private fun NavHostController.navigateTab(destination: BottomDestination) {
    navigate(destination.route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun CooknivoNavHost(viewModel: CooknivoViewModel) {
    val navController = rememberNavController()

    // Decide the start screen from the actual persisted data (avoids onboarding flicker).
    var startRoute by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val data = viewModel.initialAppData()
        startRoute = if (data.settings.onboardingCompleted) Routes.RECIPE_BOX else Routes.ONBOARDING
    }

    val start = startRoute ?: run {
        // Brief loading gate while the system splash is still visible.
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        )
        return
    }

    NavHost(navController = navController, startDestination = start) {

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onCreateFirstRecipe = {
                    viewModel.setOnboardingCompleted(true)
                    navController.navigate(Routes.addRecipe()) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
                onExploreEmptyBox = {
                    viewModel.setOnboardingCompleted(true)
                    navController.navigate(Routes.RECIPE_BOX) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.RECIPE_BOX) {
            RecipeBoxScreen(
                viewModel = viewModel,
                onOpenRecipe = { navController.navigate(Routes.recipeDetail(it)) },
                onAddRecipe = { navController.navigate(Routes.addRecipe()) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenShopping = { navController.navigateTab(BottomDestination.Shopping) },
                onSelectDestination = { navController.navigateTab(it) },
            )
        }

        composable(Routes.FAVORITES) {
            FavoritesScreen(
                viewModel = viewModel,
                onOpenRecipe = { navController.navigate(Routes.recipeDetail(it)) },
                onSelectDestination = { navController.navigateTab(it) },
            )
        }

        composable(Routes.SHOPPING) {
            ShoppingListScreen(
                viewModel = viewModel,
                onSelectDestination = { navController.navigateTab(it) },
            )
        }

        composable(Routes.CATEGORIES) {
            CategoriesScreen(
                viewModel = viewModel,
                onOpenCategory = { navController.navigate(Routes.categoryDetail(it)) },
                onSelectDestination = { navController.navigateTab(it) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onOpenStatistics = { navController.navigate(Routes.STATISTICS) },
                onOpenArchive = { navController.navigate(Routes.ARCHIVE) },
                onShowOnboarding = {
                    viewModel.setOnboardingCompleted(false)
                    navController.navigate(Routes.ONBOARDING)
                },
                onSelectDestination = { navController.navigateTab(it) },
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                viewModel = viewModel,
                onOpenRecipe = { navController.navigate(Routes.recipeDetail(it)) },
                onBack = { navController.popBackStack() },
            )
        }

        // Add recipe (optional category argument via query param).
        composable(
            route = "${Routes.ADD_RECIPE}?categoryId={categoryId}",
            arguments = listOf(navArgument("categoryId") {
                type = NavType.StringType; nullable = true; defaultValue = null
            }),
        ) { entry ->
            val categoryId = entry.arguments?.getString("categoryId")
            RecipeEditorScreen(
                viewModel = viewModel,
                editingRecipeId = null,
                initialCategoryId = categoryId,
                onSaved = { id ->
                    navController.navigate(Routes.recipeDetail(id)) {
                        popUpTo(Routes.RECIPE_BOX)
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = "${Routes.EDIT_RECIPE}/{${Routes.ARG_RECIPE_ID}}",
            arguments = listOf(navArgument(Routes.ARG_RECIPE_ID) { type = NavType.StringType }),
        ) { entry ->
            val recipeId = entry.arguments?.getString(Routes.ARG_RECIPE_ID)
            RecipeEditorScreen(
                viewModel = viewModel,
                editingRecipeId = recipeId,
                initialCategoryId = null,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = "${Routes.RECIPE_DETAIL}/{${Routes.ARG_RECIPE_ID}}",
            arguments = listOf(navArgument(Routes.ARG_RECIPE_ID) { type = NavType.StringType }),
        ) { entry ->
            val recipeId = entry.arguments?.getString(Routes.ARG_RECIPE_ID)
            if (recipeId == null) {
                com.cooknivo.app.ui.screens.MissingRecordScreen("Recipe not found.") { navController.popBackStack() }
            } else {
                RecipeDetailScreen(
                    viewModel = viewModel,
                    recipeId = recipeId,
                    onEdit = { navController.navigate(Routes.editRecipe(it)) },
                    onAddToShopping = { navController.navigate(Routes.shoppingPreview(it)) },
                    onBack = { navController.popBackStack() },
                    onDeleted = {
                        navController.popBackStack(Routes.RECIPE_BOX, inclusive = false)
                    },
                )
            }
        }

        composable(
            route = "${Routes.CATEGORY_DETAIL}/{${Routes.ARG_CATEGORY_ID}}",
            arguments = listOf(navArgument(Routes.ARG_CATEGORY_ID) { type = NavType.StringType }),
        ) { entry ->
            val categoryId = entry.arguments?.getString(Routes.ARG_CATEGORY_ID)
            if (categoryId == null) {
                com.cooknivo.app.ui.screens.MissingRecordScreen("Category not found.") { navController.popBackStack() }
            } else {
                CategoryDetailScreen(
                    viewModel = viewModel,
                    categoryId = categoryId,
                    onOpenRecipe = { navController.navigate(Routes.recipeDetail(it)) },
                    onAddRecipe = { navController.navigate(Routes.addRecipe(it)) },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(
            route = "${Routes.SHOPPING_PREVIEW}/{${Routes.ARG_RECIPE_ID}}",
            arguments = listOf(navArgument(Routes.ARG_RECIPE_ID) { type = NavType.StringType }),
        ) { entry ->
            val recipeId = entry.arguments?.getString(Routes.ARG_RECIPE_ID)
            if (recipeId == null) {
                com.cooknivo.app.ui.screens.MissingRecordScreen("Recipe not found.") { navController.popBackStack() }
            } else {
                ShoppingPreviewScreen(
                    viewModel = viewModel,
                    recipeId = recipeId,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(Routes.ARCHIVE) {
            ArchiveScreen(
                viewModel = viewModel,
                onOpenRecipe = { navController.navigate(Routes.recipeDetail(it)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.STATISTICS) {
            StatisticsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
