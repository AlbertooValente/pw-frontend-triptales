package com.example.frontend_triptales

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.frontend_triptales.ui.theme.FrontendtriptalesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrontendtriptalesTheme {
                App()
            }
        }
    }
}

//FUNZIONE AVVIO APP
@Composable
fun App() {

}


//PREVIEW
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FrontendtriptalesTheme {
        App()
    }
}