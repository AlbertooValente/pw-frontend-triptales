package com.example.frontend_triptales

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

@Composable
fun Home(
    api: TripTalesApi,
    coroutineScope: CoroutineScope,
    navController: NavController,
    userViewModel: UserViewModel
){
    if(userViewModel.errorMessage == null){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = userViewModel.user?.username?.let { "Bacheca di $it" } ?: "Caricamento...",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(userViewModel.trips ?: emptyList()) { tripId ->
                    TripCard(tripId, api, navController)
                }
            }

            TripButtonMenu(api, coroutineScope, userViewModel)
        }
    }
    else{
        Text(
            text = userViewModel.errorMessage ?: "",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

//gestisce il menu del profilo
@Composable
fun ProfileMenu(
    navController: NavController,
    userViewModel: UserViewModel
){
    var showProfileMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopEnd)
    ) {
        IconButton(
            onClick = { showProfileMenu = !showProfileMenu }    //cliccando sull'immagine profilo compare un menu
        ) {
            if(userViewModel.user?.avatar != null){     //mostra l'immagine del profilo (se presente)
                AsyncImage(
                    model = Constants.BASE_URL + userViewModel.user?.avatar,
                    contentDescription = "Profilo",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
            else{
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
                    navController.navigate("edit_profile")
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            DropdownMenuItem(
                text = { Text("Logout") },
                onClick = {
                    showProfileMenu = false
                    handleLogout(navController, userViewModel)
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

//funzione che gestisce il logout
fun handleLogout(
    navController: NavController,
    userViewModel: UserViewModel
){
    AuthManager.token = null
    userViewModel.logout()

    navController.navigate("auth") {
        popUpTo("home") { inclusive = true }
    }
}

//pagina di modifica del profilo utente
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfile(
    api: TripTalesApi,
    coroutineScope: CoroutineScope,
    navController: NavController,
    userViewModel: UserViewModel
){
    val user = userViewModel.user
    var username by remember { mutableStateOf(user?.username ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf(user?.bio ?: "") }
    var avatarUri by remember { mutableStateOf<Uri?>(user?.avatar?.toUri()) }
    var isLoading by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        avatarUri = uri
    }

    //aggiorna lo stato delle variabili locali
    LaunchedEffect(user) {
        user?.let {
            username = it.username
            email = it.email
            bio = it.bio ?: ""
            it.avatar?.let { avatarPath ->
                avatarUri = avatarPath.toUri()
            }
        }
    }

    Scaffold(
        topBar = {
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
                navigationIcon = {
                    IconButton(onClick = { showConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true

                                val parts = mutableListOf<MultipartBody.Part>()

                                if(username.isNotBlank() && email.isNotBlank() && bio.isNotBlank()){
                                    if(username.isNotBlank()) parts.add(MultipartBody.Part.createFormData("username", username))
                                    if(email.isNotBlank()) parts.add(MultipartBody.Part.createFormData("email", email))
                                    if(password.isNotBlank()) parts.add(MultipartBody.Part.createFormData("password", password))
                                    if(bio.isNotBlank()) parts.add(MultipartBody.Part.createFormData("bio", bio))

                                    //aggiunge l'immagine se presente
                                    avatarUri?.let{ uri ->
                                        if (uri.scheme == "content") { //solo se è un'immagine selezionata dall'utente
                                            val file = createPngFileFromUri(context = navController.context, uri = uri)

                                            file?.let {
                                                val requestImageFile = it.asRequestBody("image/*".toMediaTypeOrNull())
                                                val avatarPart = MultipartBody.Part.createFormData("avatar", it.name, requestImageFile)
                                                parts.add(avatarPart)
                                            }
                                        }
                                    }

                                    try {
                                        val response = api.updateUser("Token ${AuthManager.token!!}", parts)

                                        if(response.isSuccessful){
                                            userViewModel.loadUser(api)
                                            showSuccessMessage = true
                                        }
                                        else{
                                            errorMessage = "Errore aggiornamento profilo"
                                        }
                                    }
                                    catch(e: Exception){
                                        errorMessage = "Errore di rete"
                                    }
                                    finally{
                                        isLoading = false
                                    }
                                }
                                else{
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Salva modifiche"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if(isLoading){    //caricamento
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center)
                )
            }
            else{
                //contenuto principale
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Modifica Profilo",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    //immagine profilo
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            if(avatarUri != null){
                                val imageModel = if(avatarUri.toString().contains("/avatars/"))
                                    Constants.BASE_URL + avatarUri.toString()
                                else
                                    avatarUri.toString()

                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = "Profilo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                            else{
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Profilo",
                                    modifier = Modifier.size(60.dp).align(Alignment.Center)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 8.dp, y = 8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Cambia foto",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Text(
                        text = "Cambia immagine profilo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    //campi del profilo
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        placeholder = { Text("Il tuo nome utente") },
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
                        placeholder = { Text("La tua email") },
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
                        placeholder = { Text("Modifica password") },
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
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Biografia") },
                        placeholder = { Text("Inserisci una breve biografia") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Bio Icon"
                            )
                        },
                        singleLine = false,
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4
                    )
                }
            }

            //messaggio di modifica riuscita
            if (showSuccessMessage) {
                Toast.makeText(context, "Profilo aggiornato con successo!", Toast.LENGTH_SHORT).show()
                showSuccessMessage = false
                navController.popBackStack()
            }

            //messaggio di errore
            errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                errorMessage = null
                navController.popBackStack()
            }
        }
    }

    //finestra di dialogo di uscita
    if(showConfirmDialog){
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Uscire senza salvare?") },
            text = { Text("Tutte le modifiche andranno perse. Sei sicuro di voler uscire?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false

                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                ) {
                    Text("Esci", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            delay(600)
            showSuccessMessage = false

            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }
    }
}


//gestisce il pulsante e il menu per aggiungere/creare un trip
@Composable
fun TripButtonMenu(
    api: TripTalesApi,
    coroutineScope: CoroutineScope,
    userViewModel: UserViewModel
){
    var expanded by remember { mutableStateOf(false) }
    var showCreateTripDialog by remember { mutableStateOf(false) }
    var showJoinTripDialog by remember { mutableStateOf(false) }

    //variabili per tracciare la posizione del pulsante
    var fabCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        contentAlignment = Alignment.BottomEnd,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier.onGloballyPositioned { coordinates -> fabCoordinates = coordinates }
        ) {
            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Aggiungi"
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(x = (-150).dp, y = (-185).dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .width(200.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("Crea trip") },
                    leadingIcon = { Icon(Icons.Default.Create, contentDescription = null) },
                    onClick = {
                        expanded = false
                        showCreateTripDialog = true
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                DropdownMenuItem(
                    text = { Text("Partecipa ad un trip") },
                    leadingIcon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                    onClick = {
                        expanded = false
                        showJoinTripDialog = true
                    }
                )
            }
        }
    }

    if(showCreateTripDialog){
        CreateTripDialog(api, coroutineScope, userViewModel, onDismiss = { showCreateTripDialog = false })
    }
    else if(showJoinTripDialog){
        JoinTripDialog(api, coroutineScope, userViewModel, onDismiss = { showJoinTripDialog = false })
    }
}

//dialog per creare un trip
@Composable
fun CreateTripDialog(
    api: TripTalesApi,
    coroutineScope: CoroutineScope,
    userViewModel: UserViewModel,
    onDismiss: () -> Unit
){
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crea un nuovo trip") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    placeholder = { Text("Inserisci il nome del trip") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Name Icon"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione") },
                    placeholder = { Text("Inserisci la descrizione del trip") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Description Icon"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        try{
                            val response = api.createTrip("Token ${AuthManager.token!!}", CreateTripRequest(name, description))

                            if(response.isSuccessful){
                                Toast.makeText(context, "Trip creato con successo!", Toast.LENGTH_SHORT).show()
                                userViewModel.loadUser(api) //ricarico le informazioni dell'utente
                                onDismiss()
                            }
                            else{   //DA TOGLIERE
                                Toast.makeText(context, "Errore: ${response.code()}", Toast.LENGTH_LONG).show()
                            }
                        }
                        catch(e: Exception) {
                            Toast.makeText(context, "Errore di rete", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ){
                Text("Crea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss){
                Text("Annulla")
            }
        }
    )
}

//dialog per partecipare ad un trip
@Composable
fun JoinTripDialog(
    api: TripTalesApi,
    coroutineScope: CoroutineScope,
    userViewModel: UserViewModel,
    onDismiss: () -> Unit
){
    var idText by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Partecipa ad un trip") },
        text = {
            OutlinedTextField(
                value = idText,
                onValueChange = { newValue ->   //filtra solo i numeri
                    if (newValue.all { it.isDigit() }) {
                        idText = newValue
                    }
                },
                label = { Text("Id trip") },
                placeholder = { Text("Inserisci l'id del trip") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = "Id Icon"
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        try{
                            val id = idText.toIntOrNull()

                            if(id != null){
                                val response = api.joinTrip("Token ${AuthManager.token!!}", id)

                                if(response.isSuccessful){
                                    val message = response.body()?.message
                                    println("$message")
                                    userViewModel.loadUser(api) //ricarico le informazioni dell'utente
                                    onDismiss()
                                }
                            }
                        }
                        catch(e: Exception) {
                            Toast.makeText(context, "Errore di rete: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ){
                Text("Partecipa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss){
                Text("Annulla")
            }
        }
    )
}

//elemento rappresentante un trip a cui l'utente è iscritto
@Composable
fun TripCard(
    tripId: Int,
    api: TripTalesApi,
    navController: NavController
){
    var trip by remember { mutableStateOf<Trip?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tripId) {
        try{
            val response = AuthManager.token?.let { token -> api.getTripInfo("Token $token", tripId) }

            if(response != null && response.isSuccessful){
                trip = response.body()
            }
            else{
                error = "Errore nel recupero del gruppo"
            }
        }
        catch(e: Exception){
            error = "Errore di rete"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                navController.navigate("trip/${tripId}")
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when {
                trip != null -> {
                    Text(
                        text = trip!!.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                error != null -> {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {
                    Text(
                        text = "Caricamento...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
