package com.cooknivo.app.util

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Local device date/time helpers. ISO-8601 timestamps, YYYY-MM-DD dates.
 * Never uses network time. Parsing failures degrade to safe fallbacks.
 */
object TimeStamps {

    /** Current instant as an ISO-8601 string (with offset), device local time. */
    fun nowIso(): String = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    /** Current date as YYYY-MM-DD. */
    fun today(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    /**
     * Parse an ISO timestamp into epoch milliseconds for sorting.
     * Returns [default] (0L by default) on any failure — never throws.
     */
    fun epochMillisOrZero(iso: String?, default: Long = 0L): Long {
        if (iso.isNullOrBlank()) return default
        return try {
            OffsetDateTime.parse(iso).toInstant().toEpochMilli()
        } catch (_: Exception) {
            try {
                // Fallback: a bare date.
                LocalDate.parse(iso).atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
            } catch (_: Exception) {
                default
            }
        }
    }

    /** Human-friendly "updated" date (YYYY-MM-DD) extracted safely from a timestamp. */
    fun dateLabel(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return try {
            OffsetDateTime.parse(iso).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            try {
                LocalDate.parse(iso).format(DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (_: Exception) {
                "—"
            }
        }
    }

    /** Extract the 4-digit year from a timestamp, or null on failure. */
    fun yearOrNull(iso: String?): Int? {
        if (iso.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(iso).year
        } catch (_: Exception) {
            try {
                LocalDate.parse(iso).year
            } catch (_: Exception) {
                null
            }
        }
    }
}
