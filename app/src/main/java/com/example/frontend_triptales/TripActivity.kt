package com.example.frontend_triptales

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
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
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
                            Bacheca(trip, api, coroutineScope, user)
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
    coroutineScope: CoroutineScope,
    user: UserViewModel
){
    var showDialog by remember { mutableStateOf(false) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }

    LaunchedEffect(trip?.id) {
        if (trip != null) {
            val response = api.getPosts("Token ${AuthManager.token}", trip.id)

            if(response.isSuccessful){
                posts = response.body() ?: emptyList()
            }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        //qui vanno i post
        items(posts) { post ->
            PostItem(api, post, user)
        }
    }

    if (showDialog && trip != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Chiudi")
                }
            },
            title = {
                Text(text = "Info Trip: ${trip.name}")
            },
            text = {
                Column {
                    Text("Descrizione: ${trip.description}")
                    Spacer(modifier = Modifier.height(5.dp))
                    Text("Codice accesso: ${trip.id}")    //fare il controllo solo per il creatore???
                }
            }
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
                                    val file = createPngFileFromUri(context, imageUri.value!!)
                                        ?: throw Exception("Impossibile leggere il file immagine")

                                    //analizza l'immagine con mlkit
                                    val bitmap = BitmapFactory.decodeStream(
                                        context.contentResolver.openInputStream(imageUri.value!!)
                                    )

                                    val analyzedText = analyzeImageWithMLKit(context, bitmap)
                                    imageDescription.value = analyzedText

                                    val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                                    val imagePart = MultipartBody.Part.createFormData("image", file.name, reqFile)

                                    val lat = "45.0".toRequestBody("text/plain".toMediaType())
                                    val lon = "9.0".toRequestBody("text/plain".toMediaType())
                                    val desc = imageDescription.value.toRequestBody("text/plain".toMediaType())

                                    //carica immagine
                                    val imageResponse = api.caricaImage("Token ${AuthManager.token}",imagePart, desc, lat, lon)

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
                                            Toast.makeText(
                                                context,
                                                "Post creato con successo!",
                                                Toast.LENGTH_SHORT
                                            ).show()

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
suspend fun analyzeImageWithMLKit(context: Context, bitmap: Bitmap): String {
    val inputImage = InputImage.fromBitmap(bitmap, 0)

    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val languageIdentifier = LanguageIdentification.getClient()
    val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    val translatorMap = mutableMapOf<String, Translator>() // per cache

    val sb = StringBuilder()

    try {
        //iconoscimento testo
        val textResult = textRecognizer.process(inputImage).await()
        val recognizedText = textResult.text

        if(recognizedText.isNotBlank()){
            val languageCode = languageIdentifier.identifyLanguage(recognizedText).await()

            if(languageCode != "und" && languageCode != "it"){
                //traduzione
                val translatorOptions = TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.fromLanguageTag(languageCode) ?: TranslateLanguage.ENGLISH)
                    .setTargetLanguage(TranslateLanguage.ITALIAN)
                    .build()

                val translator = Translation.getClient(translatorOptions)
                translatorMap[languageCode] = translator
                translator.downloadModelIfNeeded().await()

                val translatedText = translator.translate(recognizedText).await()
                sb.append("Tradotto dal $languageCode: $translatedText\n")
            }
            else{
                sb.append("Testo rilevato: $recognizedText\n")
            }
        }

        //image labeling
        val labels = labeler.process(inputImage).await()

        if(labels.isNotEmpty()){
            sb.append("Contenuto rilevato: ")
            sb.append(labels.joinToString(", ") { it.text })
        }

    }
    catch(e: Exception){
        sb.append("Errore analisi immagine: ${e.localizedMessage}")
    }
    finally{
        translatorMap.values.forEach { it.close() }
    }

    return sb.toString()
}

//converte un'API asincrona basata su Task<T> in una funzione suspend compatibile con le coroutine
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) {} }
    addOnFailureListener { exception -> cont.resumeWithException(exception) }
}


//elemento post in bacheca
@Composable
fun PostItem(
    api: TripTalesApi,
    post: Post,
    user: UserViewModel
) {
    var image by remember { mutableStateOf<Image?>(null) }
    var author by remember { mutableStateOf<User?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    //carica immagine
    LaunchedEffect(post.image) {
        post.image.let { imageId ->
            try {
                val response = api.getImage("Token ${AuthManager.token}", imageId)

                if (response.isSuccessful) {
                    image = response.body()
                } else {
                    errorMessage = "Errore nel caricamento dell'immagine (Codice ${response.code()})"
                }
            } catch (e: Exception) {
                errorMessage = "Errore nel caricamento dell'immagine: ${e.localizedMessage}"
            }
        }
    }

    //carica autore
    LaunchedEffect(post.created_by) {
        try {
            val response = api.getUserById("Token ${AuthManager.token}", post.created_by)

            if (response.isSuccessful) {
                author = response.body()
            } else {
                errorMessage = "Errore nel recupero dell'autore (Codice ${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Errore nel recupero dell'autore: ${e.localizedMessage}"
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = post.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            post.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

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

                /*
                it.description?.let { desc ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                 */
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = Constants.BASE_URL + author?.avatar,
                    contentDescription = "Profilo di ${author?.username}",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                    contentScale = ContentScale.Crop,
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

            Text(
                text = "Pubblicato il ${post.created_at}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

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
