package com.cooknivo.app

import android.app.Application
import com.cooknivo.app.data.CooknivoRepository

/**
 * Application class. Holds the single repository instance for the app lifetime.
 * No DI framework is used intentionally (see README architecture notes).
 */
class CooknivoApp : Application() {
    val repository: CooknivoRepository by lazy { CooknivoRepository(this) }
}
