package com.fitviet.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.fitviet.app.ui.navigation.FitVietNavHost
import com.fitviet.app.ui.theme.FitVietTheme

// AppCompatActivity (not plain ComponentActivity) so AppCompatDelegate.setApplicationLocales()
// actually recreates/relocalizes this activity on pre-API-33 devices.
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
