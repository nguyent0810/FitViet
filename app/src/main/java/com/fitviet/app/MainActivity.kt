package com.fitviet.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.fitviet.app.ui.navigation.FitVietNavHost
import com.fitviet.app.ui.theme.FitVietTheme

// AppCompatActivity (not plain ComponentActivity) — AppCompatDelegate.setApplicationLocales()
// (see util/LocaleController.kt) only re-applies the chosen locale to a running activity via
// AppCompatActivity's attachBaseContext() override on API 26-32; AppLocalesMetadataHolderService
// alone only handles persisting the choice, not applying it to this activity's resources.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FitVietApp).container
        setContent {
            FitVietTheme {
                FitVietNavHost(container = container)
            }
        }
    }
}
