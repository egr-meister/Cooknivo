package com.cooknivo.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/**
 * Central JSON configuration. Lenient and tolerant so that missing/added fields
 * and empty payloads deserialize without crashing.
 */
object CooknivoJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
        explicitNulls = false
    }

    /**
     * Decode a JSON array string element-by-element, skipping any malformed items
     * instead of discarding the entire list. Returns empty list on blank/invalid
     * top-level input. Never throws.
     */
    inline fun <reified T> decodeArrayResilient(raw: String?): List<T> {
        if (raw.isNullOrBlank()) return emptyList()
        val root: JsonElement = try {
            instance.parseToJsonElement(raw)
        } catch (_: Exception) {
            return emptyList()
        }
        val array = root as? JsonArray ?: return emptyList()
        val out = ArrayList<T>(array.size)
        for (element in array) {
            try {
                out.add(instance.decodeFromJsonElement(element))
            } catch (_: Exception) {
                // Skip this malformed element; keep the valid ones.
            }
        }
        return out
    }

    inline fun <reified T> encode(value: T): String = instance.encodeToString(value)
}
