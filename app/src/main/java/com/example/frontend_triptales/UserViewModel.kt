package com.example.frontend_triptales

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() { //per tenere salvati i dati dell'utente
    var user by mutableStateOf<User?>(null)
        private set     //fuori dalla classe posso solo leggerlo

    var errorMessage by mutableStateOf<String?>(null)
        private set

    //metto ? solo per non generare errore, anche se la funzione viene sempre richiamata quando il token non è null
    fun loadUser(api: TripTalesApi, token: String?) {
        viewModelScope.launch {
            try {
                val response = api.getUser("Token $token")
                if (response.isSuccessful) {
                    user = response.body()
                    errorMessage = null
                } else {
                    errorMessage = "Errore nel recupero del profilo"
                }
            } catch (e: Exception) {
                errorMessage = "Errore di rete"
            }
        }
    }

    fun logout() {
        user = null
        errorMessage = null
    }
}