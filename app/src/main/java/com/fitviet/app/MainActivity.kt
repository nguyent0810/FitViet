package com.fitviet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fitviet.app.ui.navigation.FitVietNavHost
import com.fitviet.app.ui.theme.FitVietTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FitVietApp).container
        setContent {
            FitVietTheme {
                FitVietNavHost(onboardingRepository = container.onboardingRepository)
            }
        }
    }
}
