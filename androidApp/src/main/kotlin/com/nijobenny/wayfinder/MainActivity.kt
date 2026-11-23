package com.nijobenny.wayfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // System splash with the app icon; it hands over to the animated Compose intro.
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initWayfinder(applicationContext)

        setContent {
            App()
        }
    }
}
