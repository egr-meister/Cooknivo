package com.cooknivo.app.util

import java.util.UUID

/** Locally generated identifiers. No network, no external service. */
object Ids {
    fun newId(): String = UUID.randomUUID().toString()
}
