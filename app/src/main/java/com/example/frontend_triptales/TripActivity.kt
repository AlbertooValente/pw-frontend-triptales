package com.example.frontend_triptales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope

@Composable
fun TripHome(
    api: TripTalesApi,
    coroutineScope: CoroutineScope,
    navController: NavController,
    userViewModel: UserViewModel,
    id: Int
){
    var trip by remember { mutableStateOf<Trip?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        try {
            val response = api.getTripInfo("Token ${AuthManager.token!!}", id)

            if(response.isSuccessful){
                trip = response.body()
            }
            else{
                error = "Errore nel recupero delle info del trip"
            }
        }
        catch(e: Exception){
            error = "Errore di rete: ${e.localizedMessage}"
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if(trip != null){
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = trip!!.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = { showDialog = true },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    //tutti i post


                }
            }
        }
        else if(error != null){
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        else{
            CircularProgressIndicator()
        }

        if(showDialog && trip != null){
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Chiudi")
                    }
                },
                title = {
                    Text(text = "Info Trip: ${trip!!.name}")
                },
                text = {
                    Column {
                        Text("Descrizione: ${trip!!.description}")
                        Spacer(modifier = Modifier.height(5.dp))
                        Text("Codice accesso: ${trip!!.id}")

                        /*
                        if(userViewModel.user?.username == trip!!.createdBy.username){
                            Text("Codice accesso: ${trip!!.id}")
                        }
                         */
                    }
                }
            )
        }
    }
}