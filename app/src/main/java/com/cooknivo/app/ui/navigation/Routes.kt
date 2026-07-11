package com.cooknivo.app.ui.navigation

/** Central definition of navigation routes and argument keys. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val RECIPE_BOX = "recipe_box"
    const val FAVORITES = "favorites"
    const val SHOPPING = "shopping"
    const val CATEGORIES = "categories"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val STATISTICS = "statistics"
    const val ARCHIVE = "archive"

    const val ARG_RECIPE_ID = "recipeId"
    const val ARG_CATEGORY_ID = "categoryId"

    const val ADD_RECIPE = "add_recipe"
    fun addRecipe(categoryId: String? = null): String =
        if (categoryId == null) ADD_RECIPE else "$ADD_RECIPE?categoryId=$categoryId"

    const val EDIT_RECIPE = "edit_recipe"
    fun editRecipe(recipeId: String): String = "$EDIT_RECIPE/$recipeId"

    const val RECIPE_DETAIL = "recipe_detail"
    fun recipeDetail(recipeId: String): String = "$RECIPE_DETAIL/$recipeId"

    const val CATEGORY_DETAIL = "category_detail"
    fun categoryDetail(categoryId: String): String = "$CATEGORY_DETAIL/$categoryId"

    const val SHOPPING_PREVIEW = "shopping_preview"
    fun shoppingPreview(recipeId: String): String = "$SHOPPING_PREVIEW/$recipeId"
}

/** Bottom navigation destinations. */
enum class BottomDestination(val route: String, val label: String) {
    Recipes(Routes.RECIPE_BOX, "Recipes"),
    Favorites(Routes.FAVORITES, "Favorites"),
    Shopping(Routes.SHOPPING, "Shopping"),
    Categories(Routes.CATEGORIES, "Categories"),
    Settings(Routes.SETTINGS, "Settings"),
}
