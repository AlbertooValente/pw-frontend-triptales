package com.example.frontend_triptales

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun Home(){
    val token = AuthManager.token

    Text("Benvenuto nella Home!\nToken: $token")
}