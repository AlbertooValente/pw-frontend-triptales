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

    var trips by mutableStateOf<List<Int>?>(null)

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var token by mutableStateOf<String?>(null)
        private set


    fun setUserToken(strToken: String){
        token = strToken
    }

    fun loadUser(api: TripTalesApi) {
        viewModelScope.launch {
            try {
                val response = api.getUser("Token $token")

                if(response.isSuccessful){
                    user = response.body()
                    errorMessage = null
                    loadTrips(api)
                }
                else{
                    errorMessage = "Errore nel recupero del profilo"
                }
            }
            catch(e: Exception){
                errorMessage = "Errore di rete"
            }
        }
    }

    private fun loadTrips(api: TripTalesApi){
        viewModelScope.launch {
            try {
                val response = api.getTrips("Token $token")

                if(response.isSuccessful){
                    trips = response.body()
                    errorMessage = null
                }
                else{
                    errorMessage = "Errore nel recupero dei trip"
                }
            }
            catch(e: Exception){
                errorMessage = "Errore di rete"
            }
        }
    }


    fun logout() {
        user = null
        errorMessage = null
        token = null
    }
}