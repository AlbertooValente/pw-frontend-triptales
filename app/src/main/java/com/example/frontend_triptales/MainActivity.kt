package com.example.frontend_triptales

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.frontend_triptales.ui.theme.FrontendtriptalesTheme
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

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

//gestisce la navigazione tra le varie pagine dell'applicazione
@Composable
fun AppNav(){
    val navController = rememberNavController()
    val api = RetrofitInstance.api
    val coroutineScope = rememberCoroutineScope()
    val user: UserViewModel = viewModel()

    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            AppLogin(
                api = api,
                coroutineScope = coroutineScope,
                onLoginSuccess = {
                    user.loadUser(api)

                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            MainLayout(api, navController, user, coroutineScope) {
                Home(api, coroutineScope, navController, user)
            }
        }

        composable("edit_profile"){
            EditProfile(api, coroutineScope, navController, user)
        }

        composable("trip/{tripId}"){ backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId")?.toIntOrNull()

            MainLayout(api, navController, user, coroutineScope){
                TripHome(api, coroutineScope, navController, user, tripId!!)
            }
        }

        composable("create_post/{tripId}"){ backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId")?.toIntOrNull()

            CreatePostScreen(api, coroutineScope, navController, tripId!!)
        }

        composable("postDetailPage/{postId}"){backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId")?.toIntOrNull()

            PostDetailPage(api, postId!!, navController, coroutineScope, user)
        }
    }
}

//layout principale dell'applicazione
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    api: TripTalesApi,
    navController: NavController,
    userViewModel: UserViewModel,
    coroutineScope: CoroutineScope,
    content: @Composable (PaddingValues) -> Unit
){
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)    //controlla se l'elemento drawer è aperto o chiuso

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
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item{
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
                        }

                        items(userViewModel.trips ?: emptyList()) { tripId ->
                            TripDrawerItem(tripId, api, navController)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
            }
        }
    ) {
        Scaffold(
            topBar = {  //barra in alto nella pagina
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
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
                    actions = {
                        ProfileMenu(navController, userViewModel)
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
                content(innerPadding)
            }
        }
    }
}

@Composable
fun TripDrawerItem(
    tripId: Int,
    api: TripTalesApi,
    navController: NavController
) {
    var tripName by remember { mutableStateOf("Trip $tripId") }

    LaunchedEffect(tripId) {
        try {
            val response = api.getTripInfo("Token ${AuthManager.token!!}", tripId)

            if(response.isSuccessful){
                tripName = response.body()?.name ?: "Trip $tripId"
            }
        }
        catch (_: Exception) {
            //prende il nome generico (es. trip 1)
        }
    }

    Text(
        text = tripName,
        modifier = Modifier
            .padding(16.dp)
            .clickable {
                navController.navigate("trip/$tripId")
            },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

//pagina di login/registrazione
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
                                fontWeight = if(selectedTab == 0)
                                    FontWeight.Bold
                                else
                                    FontWeight.Normal
                            ),
                            color = if(selectedTab == 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        if(selectedTab == 0){
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
                                fontWeight = if(selectedTab == 1)
                                    FontWeight.Bold
                                else
                                    FontWeight.Normal
                            ),
                            color = if(selectedTab == 1)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if(selectedTab == 1){
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

            when(selectedTab){
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
            Text(text = it, color = MaterialTheme.colorScheme.error)
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
    var bio by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    //per ottenere l'immagine avatar
    val context = LocalContext.current
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        avatarUri = uri
    }

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
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Biografia") },
            placeholder = { Text("Inserisci una breve biografia") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            shape = RoundedCornerShape(12.dp),
            maxLines = 4
        )
        Spacer(modifier = Modifier.height(16.dp))

        //box per inserire l'immagine avatar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { launcher.launch("image/*") }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if(avatarUri != null){    //se l'utente ha selezionato un'immagine
                Image(
                    painter = rememberAsyncImagePainter(avatarUri),
                    contentDescription = "Avatar Selezionato",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            else{
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Immagine profilo",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tocca per selezionare",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    try{
                        //prepara i campi di testo come RequestBody
                        val usernamePart = username.toRequestBody("text/plain".toMediaTypeOrNull())
                        val emailPart = email.toRequestBody("text/plain".toMediaTypeOrNull())
                        val passwordPart = password.toRequestBody("text/plain".toMediaTypeOrNull())
                        val bioPart = bio.toRequestBody("text/plain".toMediaTypeOrNull())

                        //prepara l'avatar solo se selezionato
                        val avatarPart = if(avatarUri != null){
                            val file = createPngFileFromUri(context, avatarUri!!)

                            if(file == null){
                                errorMessage = "Errore nel caricare l'immagine."
                                return@launch
                            }

                            val requestImageFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                            MultipartBody.Part.createFormData("avatar", file.name, requestImageFile)
                        }
                        else{
                            null    //nessuna immagine selezionata
                        }

                        //effettua la richiesta con o senza avatar
                        val response = apiService.register(
                            usernamePart,
                            emailPart,
                            passwordPart,
                            bioPart,
                            avatarPart
                        )

                        //gestione risposta
                        if(response.isSuccessful){
                            val token = response.body()?.token

                            if(!token.isNullOrEmpty()){
                                AuthManager.token = token
                                onLoginSuccess()
                            }
                        }
                        else{
                            errorMessage = "Registrazione fallita"
                        }
                    }
                    catch(e: Exception){
                        errorMessage = "Errore di rete: ${e.localizedMessage}"  //modifica a fine progetto
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
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}

//crea il file PNG dall'URI dell'immagine selezionata
fun createPngFileFromUri(context: Context, uri: Uri): File? {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return null
    val tempFile = File(context.cacheDir, "avatar.png")

    tempFile.outputStream().use { output ->
        inputStream.copyTo(output)
    }

    return tempFile
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