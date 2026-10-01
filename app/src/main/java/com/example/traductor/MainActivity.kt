package com.example.traductor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.traductor.ui.SplashScreen
import com.example.traductor.ui.TranslationScreen
import com.example.traductor.ui.TranslationViewModel
import com.example.traductor.ui.theme.TraductorTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TranslationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TraductorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        SplashScreen(onSplashFinished = { showSplash = false })
                    } else {
                        TranslationScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
