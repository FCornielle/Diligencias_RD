package com.diligenciard.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.diligenciard.app.ui.home.HomeScreen
import com.diligenciard.app.ui.theme.DiligenciaRDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiligenciaRDTheme {
                HomeScreen()
            }
        }
    }
}
