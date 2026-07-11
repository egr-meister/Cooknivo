package com.cooknivo.app.util

/** Recommended text length limits (characters). Enforced by editors and repository. */
object TextLimits {
    const val RECIPE_NAME = 120
    const val DESCRIPTION = 1000
    const val INGREDIENT_NAME = 150
    const val INGREDIENT_QUANTITY = 80
    const val INGREDIENT_NOTE = 300
    const val STEP_TITLE = 100
    const val STEP_INSTRUCTION = 2000
    const val STEP_NOTE = 500
    const val RECIPE_NOTES = 3000
    const val CATEGORY_NAME = 60
    const val SHOPPING_TITLE = 150

    /** Trim leading/trailing whitespace and hard-cap the length. Preserves inner content. */
    fun clamp(value: String, max: Int): String {
        val trimmed = value.trim()
        return if (trimmed.length <= max) trimmed else trimmed.substring(0, max)
    }

    /** Like [clamp] but preserves internal line breaks (only trims edges). */
    fun clampMultiline(value: String, max: Int): String {
        val trimmed = value.trim()
        return if (trimmed.length <= max) trimmed else trimmed.substring(0, max)
    }
}

/** Maximum minutes allowed per time field (7 days). Guards against overflow. */
object TimeInput {
    const val MAX_MINUTES = 10080

    /**
     * Parse free-text minute input safely.
     * Blank -> null (not set). Non-numeric -> null. Negative -> 0. Over max -> max.
     */
    fun parseMinutes(raw: String): Int? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val parsed = trimmed.toLongOrNull() ?: return null
        val bounded = parsed.coerceIn(0L, MAX_MINUTES.toLong())
        return bounded.toInt()
    }
}
