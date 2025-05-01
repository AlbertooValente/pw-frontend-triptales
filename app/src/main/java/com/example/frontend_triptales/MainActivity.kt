package com.example.frontend_triptales

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.frontend_triptales.ui.theme.FrontendtriptalesTheme
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrontendtriptalesTheme (
                dynamicColor = false
            ){
                AppNav()
            }
        }
    }
}

//mantiene loggato l'utente per tutta la durata della sessione di utilizzo dell'app
object AuthManager {
    var token: String? = null
}

@Composable
fun AppNav(){
    val navController = rememberNavController()
    val tripTalesApi = RetrofitInstance.api
    val coroutineScope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            AppLogin(
                api = tripTalesApi,
                coroutineScope = coroutineScope,
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            Home()
        }
    }
}


@Composable
fun AppLogin(
    api: TripTalesApi,
    coroutineScope: CoroutineScope,
    onLoginSuccess: () -> Unit
) {
    //tiene traccia del tab selezionato (0 = login, 1 = registrazione)
    var selectedTab by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.Center)
                .padding(16.dp)
        ) {
            Text(
                text = "TripTales",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(40.dp))

            //menu tab per la selezione tra login e registrazione
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                //tab login
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 12.dp),

                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Accedi",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (selectedTab == 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(3.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(1.5.dp)
                                    )
                            )
                        }
                    }
                }

                //tab registrazione
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 12.dp),

                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Registrati",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (selectedTab == 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(3.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(1.5.dp)
                                    )
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            when (selectedTab) {
                0 -> LoginForm(
                    coroutineScope,
                    api,
                    onLoginSuccess = onLoginSuccess
                )
                1 -> RegistrationForm(
                    coroutineScope,
                    api,
                    onLoginSuccess = onLoginSuccess
                )
            }
        }
    }
}

@Composable
fun LoginForm(
    coroutineScope: CoroutineScope,
    apiService: TripTalesApi,
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            placeholder = { Text("Inserisci il tuo username") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Username Icon"
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            placeholder = { Text("Inserisci la tua password") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Password Icon"
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                coroutineScope.launch{
                    try{
                        val response = apiService.login(LoginRequest(username, password))

                        if(response.isSuccessful){
                            val token = response.body()?.token

                            if(!token.isNullOrEmpty()){
                                AuthManager.token = token

                                onLoginSuccess()
                            }
                        }
                        else{
                            errorMessage = "Credenziali non valide"
                        }
                    }
                    catch(e: Exception){
                        errorMessage = "Errore di rete"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),

            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                "ACCEDI",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, color = Color.Red)
        }
    }
}

@Composable
fun RegistrationForm(
    coroutineScope: CoroutineScope,
    apiService: TripTalesApi,
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            placeholder = { Text("Scegli uno username") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Username Icon"
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            placeholder = { Text("Inserisci la tua email") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email Icon"
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            placeholder = { Text("Crea una password") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Password Icon"
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    try{
                        val response = apiService.register(User(username, email, password, "", null))

                        if(response.isSuccessful){
                            val token = response.body()?.token

                            if(!token.isNullOrEmpty()){
                                AuthManager.token = token

                                onLoginSuccess()
                            }
                        }
                        else{
                            errorMessage = "Credenziali non valide"
                        }
                    }
                    catch(e: Exception){
                        errorMessage = "Errore di rete: ${e.localizedMessage}"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                "REGISTRATI",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, color = Color.Red)
        }
    }
}


//PREVIEW
@Preview(showBackground = true)
@Composable
fun TripTalesPreview() {
    FrontendtriptalesTheme(
        darkTheme = false,
        dynamicColor = false
    ) {
        AppNav()
    }
}