package com.example.frontend_triptales

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import kotlin.coroutines.resumeWithException
import android.Manifest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun TripHome(
    api: TripTalesApi,
    coroutineScope: CoroutineScope,
    navController: NavController,
    user: UserViewModel,
    id: Int
){
    var trip by remember { mutableStateOf<Trip?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = bacheca, 1 = mappa, 2 = classifica

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
            error = "Errore di rete"
        }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0 && trip != null) {
                FloatingActionButton(
                    onClick = { navController.navigate("create_post/${id}") },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Aggiungi"
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Bacheca") },
                    label = { Text("Bacheca") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Place, contentDescription = "Mappa") },
                    label = { Text("Mappa") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Star, contentDescription = "Classifica") },
                    label = { Text("Classifica") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                trip != null -> {
                    when (selectedTab) {
                        0 -> {
                            Bacheca(trip, api, navController)
                        }
                        1 -> {
                            Text("Mappa (...)", modifier = Modifier.padding(24.dp))
                        }
                        2 -> {
                            Text("Classifica (...)", modifier = Modifier.padding(24.dp))
                        }
                    }
                }
                error != null -> {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun Bacheca(
    trip: Trip?,
    api: TripTalesApi,
    navController: NavController
) {
    var showDialog by remember { mutableStateOf(false) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }

    LaunchedEffect(trip?.id) {
        if (trip != null) {
            val response = api.getPosts("Token ${AuthManager.token}", trip.id)

            if (response.isSuccessful) {
                posts = response.body() ?: emptyList()
            }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        //trip header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = trip!!.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { showDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Trip Info",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Optional: Add a subtle description or additional info
                    trip?.description?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        //sezione dei post
        items(posts) { post ->
            PostItem(api, post, navController)
        }

        //se non ci sono post
        if (posts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ancora nessun post..",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    //dialog con le info del trip
    if (showDialog && trip != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Close")
                }
            },
            title = {
                Text(
                    text = "Trip Details: ${trip.name}",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "Description: ${trip.description}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Access Code: ${trip.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

//pagina creazione post
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    api: TripTalesApi,
    coroutineScope: CoroutineScope,
    navController: NavController,
    tripId: Int
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val imageUri = remember { mutableStateOf<Uri?>(null) }
    val postDescription = remember { mutableStateOf("") }
    val imageDescription = remember { mutableStateOf("") }
    val title = remember { mutableStateOf("") }

    //prepara il file URI dove verrà salvata l'immagine
    val photoFile = remember {
        File.createTempFile(
            "photo_${System.currentTimeMillis()}_",
            ".jpg",
            context.cacheDir
        )
    }

    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
    }

    //camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ){ success ->
        if(success){
            imageUri.value = photoUri
        }
    }

    //richiesta permessi utilizzo fotocamera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if(isGranted){
            try{
                cameraLauncher.launch(photoUri)
            }
            catch(e: Exception){
                errorMessage = "Errore nell'avvio della fotocamera: ${e.localizedMessage}"
            }
        }
        else{
            Toast.makeText(
                context,
                "Permesso fotocamera necessario per scattare foto",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    //richiesta permessi utilizzo posizione
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if(!isGranted){
            Toast.makeText(
                context,
                "Permesso di localizzazione necessario per ottenere la posizione",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    //pagina creazione post
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuovo Post") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title.value,
                        onValueChange = { title.value = it },
                        label = { Text("Titolo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = postDescription.value,
                        onValueChange = { postDescription.value = it },
                        label = { Text("Descrizione") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }

                //preview dell'immagine e pulsante per scattare foto combinati
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                //richiedi permesso fotocamera quando l'utente clicca sul box
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if(imageUri.value != null){
                            AsyncImage(
                                model = imageUri.value,
                                contentDescription = "Anteprima immagine",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else{
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Scatta foto",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    "Tocca per scattare una foto",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                //messaggio di errore
                errorMessage?.let {
                    item {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (title.value.isBlank()) {
                                errorMessage = "Inserisci un titolo"
                                return@Button
                            }

                            if (imageUri.value == null) {
                                errorMessage = "Scatta una foto"
                                return@Button
                            }

                            isLoading = true
                            errorMessage = null

                            coroutineScope.launch {
                                try {
                                    //gestione posizione
                                    if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                        != PackageManager.PERMISSION_GRANTED) {

                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                        isLoading = false
                                        return@launch
                                    }

                                    val location = getCurrentLocation(context)

                                    if(location == null){
                                        errorMessage = "Impossibile ottenere la posizione"
                                        isLoading = false
                                        return@launch
                                    }

                                    val lat = location.latitude.toString().toRequestBody("text/plain".toMediaType())
                                    val lon = location.longitude.toString().toRequestBody("text/plain".toMediaType())


                                    //gestione immagine
                                    val file = createPngFileFromUri(context, imageUri.value!!) ?: throw Exception("Impossibile leggere il file immagine")

                                    //analizza l'immagine con mlkit
                                    val bitmap = BitmapFactory.decodeStream(
                                        context.contentResolver.openInputStream(imageUri.value!!)
                                    )

                                    val analyzedText = imageLabeling(bitmap)
                                    imageDescription.value = analyzedText

                                    val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                                    val imagePart = MultipartBody.Part.createFormData("image", file.name, reqFile)
                                    val desc = imageDescription.value.toRequestBody("text/plain".toMediaType())

                                    //carica immagine
                                    val imageResponse = api.caricaImage("Token ${AuthManager.token}", imagePart, desc, lat, lon)

                                    if(imageResponse.isSuccessful){
                                        val imageId = imageResponse.body()!!.id

                                        //crea post
                                        val postRequest = CreatePostRequest(
                                            title = title.value,
                                            description = postDescription.value,
                                            image = imageId,
                                            group = tripId
                                        )

                                        val postResponse = api.creaPost(
                                            "Token ${AuthManager.token!!}",
                                            postRequest
                                        )

                                        if(postResponse.isSuccessful){
                                            Toast.makeText(context, "Post creato con successo!", Toast.LENGTH_SHORT).show()

                                            //torna alla paginaprecedente
                                            navController.popBackStack()
                                        }
                                        else{
                                            errorMessage = "Errore nella creazione del post"
                                        }
                                    }
                                    else{
                                        errorMessage = "Errore nel caricamento dell'immagine"
                                    }
                                }
                                catch(e: Exception){
                                    errorMessage = "Errore: ${e.localizedMessage}"
                                }
                                finally{
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if(isLoading){
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        else{
                            Text("Pubblica Post")
                        }
                    }
                }
            }

            if(isLoading){
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

//funzione che analizza l'immagine del post con mlkit
suspend fun imageLabeling(bitmap: Bitmap): String {
    val inputImage = InputImage.fromBitmap(bitmap, 0)
    val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    val sb = StringBuilder()

    try {
        val labels = labeler.process(inputImage).await()

        if(labels.isNotEmpty()){
            sb.append(labels.joinToString(" #") { it.text })
        }
    }
    catch(e: Exception){
        sb.append("Errore analisi immagine: ${e.localizedMessage}")
    }

    return sb.toString()
}

//converte un'API asincrona basata su Task<T> in una funzione suspend compatibile con le coroutine
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) {} }
    addOnFailureListener { exception -> cont.resumeWithException(exception) }
}

//funzione per ottenere la posizione
@SuppressLint("MissingPermission")
suspend fun getCurrentLocation(context: Context): Location? {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    return suspendCancellableCoroutine { cont ->
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                cont.resume(location) {}
            }
            .addOnFailureListener {
                cont.resume(null) {}
            }
    }
}


//elemento post in bacheca
@Composable
fun PostItem(
    api: TripTalesApi,
    post: Post,
    navController: NavController
) {
    var image by remember { mutableStateOf<Image?>(null) }
    var author by remember { mutableStateOf<User?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    //carica immagine
    LaunchedEffect(post.image) {
        post.image.let { imageId ->
            try {
                val response = api.getImage("Token ${AuthManager.token}", imageId)

                if(response.isSuccessful){
                    image = response.body()
                }
                else {
                    errorMessage = "Errore nel caricamento dell'immagine (Codice ${response.code()})"
                }
            }
            catch(e: Exception){
                errorMessage = "Errore nel caricamento dell'immagine: ${e.localizedMessage}"
            }
        }
    }

    //carica autore
    LaunchedEffect(post.created_by) {
        try {
            val response = api.getUserById("Token ${AuthManager.token}", post.created_by)

            if(response.isSuccessful){
                author = response.body()
            }
            else{
                errorMessage = "Errore nel recupero dell'autore (Codice ${response.code()})"
            }
        }
        catch(e: Exception){
            errorMessage = "Errore nel recupero dell'autore: ${e.localizedMessage}"
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate("postDetailPage/${post.id}")
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = post.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            image?.let {
                AsyncImage(
                    model = it.image,
                    contentDescription = it.description ?: "Immagine del post",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .padding(bottom = 8.dp),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Lat: " + it.latitude + " Long: " + it.longitude,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                //informazioni autore
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    AsyncImage(
                        model = Constants.BASE_URL + author?.avatar,
                        contentDescription = "Profilo di ${author?.username}",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    author?.username?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                //data post
                Text(
                    text = "Pubblicato il ${formatDate(post.created_at)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            //mostra messaggio di errore se presente
            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

//funzione che modifica il formato della data
fun formatDate(sqlDate: String): String {
    return try{
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")

        val date = parser.parse(sqlDate)
        val formatter = SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault())
        formatter.format(date!!)
    }
    catch(e: Exception){
        sqlDate
    }
}
