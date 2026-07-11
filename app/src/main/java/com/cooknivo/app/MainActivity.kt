package com.cooknivo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cooknivo.app.ui.navigation.CooknivoNavHost
import com.cooknivo.app.ui.theme.CooknivoTheme
import com.cooknivo.app.viewmodel.CooknivoViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CooknivoViewModel by viewModels {
        CooknivoViewModel.Factory((application as CooknivoApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CooknivoTheme {
                CooknivoNavHost(viewModel = viewModel)
            }
        }
    }
}
