package com.cooknivo.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shopping categories used by ingredients and shopping items.
 * Values are stored by name; [safeValueOf] recovers gracefully from unknown values.
 */
@Serializable
enum class ShoppingCategory(val displayName: String) {
    @SerialName("Produce") Produce("Produce"),
    @SerialName("Dairy") Dairy("Dairy"),
    @SerialName("MeatAndFish") MeatAndFish("Meat and Fish"),
    @SerialName("Bakery") Bakery("Bakery"),
    @SerialName("Pantry") Pantry("Pantry"),
    @SerialName("Frozen") Frozen("Frozen"),
    @SerialName("Drinks") Drinks("Drinks"),
    @SerialName("Spices") Spices("Spices"),
    @SerialName("Household") Household("Household"),
    @SerialName("Other") Other("Other");

    companion object {
        fun safeValueOf(raw: String?): ShoppingCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: Other
    }
}

/** Recipe card display density on the card-box screen. */
@Serializable
enum class RecipeCardDensity {
    @SerialName("Comfortable") Comfortable,
    @SerialName("Compact") Compact;

    companion object {
        fun safeValueOf(raw: String?): RecipeCardDensity =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: Comfortable
    }
}

/** How ingredients are added to the shopping list. */
@Serializable
enum class ShoppingGenerationMode {
    @SerialName("PreviewFirst") PreviewFirst,
    @SerialName("AddMissing") AddMissing,
    @SerialName("AddAll") AddAll;

    companion object {
        fun safeValueOf(raw: String?): ShoppingGenerationMode =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PreviewFirst
    }
}
