package com.example.appguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.appguard.ui.navigation.AppNavGraph
import com.example.appguard.ui.theme.AppGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppGuardTheme {
                AppNavGraph()
            }
        }
    }
}