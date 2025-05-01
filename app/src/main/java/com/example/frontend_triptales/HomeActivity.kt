package com.example.frontend_triptales

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(
    api: TripTalesApi,
    coroutineScope: CoroutineScope,
    navController: NavController
){
    var user by remember { mutableStateOf<User?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showProfileMenu by remember { mutableStateOf(false) }   //gestisce il menu del profilo
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)    //controlla se l'elemento drawer è aperto o chiuso

    //recupera i dati dell'utente
    LaunchedEffect(Unit) {
        val token = AuthManager.token

        if (token != null) {
            coroutineScope.launch {
                try {
                    val response = api.getUser("Token $token")

                    if (response.isSuccessful) {
                        user = response.body()
                    } else {
                        errorMessage = "Errore nel recupero del profilo"
                    }
                } catch (e: Exception) {
                    errorMessage = "Errore di rete: ${e.localizedMessage}"
                }
            }
        }
    }

    //menu laterale a comparsa
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {   //conenuto del menu laterale
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(   //titolo del menu laterale
                    "TripTales Menu",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider()

                Column {
                    //MODIFICAMI
                    Text(
                        text = "Home",
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable {
                                navController.navigate("home")
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    //aggiungi qui le voci del menu laterale
                }
                Spacer(Modifier.weight(1f))
            }
        }
    ) {
        Scaffold(
            topBar = {  //barra in alto nella pagina
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    title = {
                        Text(
                            "TripTales",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    navigationIcon = {  //gestione dell'icona del menu laterale e del click su di essa
                        IconButton(onClick = {
                            coroutineScope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier.wrapContentSize(Alignment.TopEnd)
                        ) {
                            IconButton(
                                onClick = { showProfileMenu = !showProfileMenu }    //cliccando sull'immagine profilo compare un menu
                            ) {
                                if (user?.avatar != null) {     //mostra l'immagine del profilo (se presente)
                                    AsyncImage(
                                        model = Constants.BASE_URL + user?.avatar,
                                        contentDescription = "Profilo",
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                                        contentScale = ContentScale.Crop,
                                        //error = painterResource(id = R.drawable.default_avatar)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Profilo",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            DropdownMenu(   //menu a discesa attivato dal click sull'immagine profilo
                                expanded = showProfileMenu,
                                onDismissRequest = { showProfileMenu = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .width(200.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Modifica profilo") },
                                    onClick = {
                                        showProfileMenu = false
                                        //navigazione alla schermata di modifica profilo (da implementare)
                                        //navController.navigate("edit_profile")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null
                                        )
                                    }
                                )
                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text("Logout") },
                                    onClick = {
                                        showProfileMenu = false
                                        handleLogout(navController)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            //contenuto principale della pagina
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(
                            text = user?.username?.let { "Bacheca di $it" } ?: "Caricamento...",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

//funzione che gestisce il logout
fun handleLogout(navController: NavController) {
    AuthManager.token = null

    navController.navigate("auth") {
        popUpTo("home") { inclusive = true }
    }
}