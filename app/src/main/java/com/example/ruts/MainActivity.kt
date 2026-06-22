package com.example.ruts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ruts.presentation.navigation.RutsNavHost
import com.example.ruts.ui.theme.RutsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RutsTheme {
                RutsNavHost()
            }
        }
    }
}
