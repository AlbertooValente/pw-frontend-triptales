@file:Suppress("DEPRECATION")

package com.example.frontend_triptales

import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.font.FontStyle
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailPage(
    api: TripTalesApi,
    tripId: Int,
    postId: Int,
    navController: NavController,
    coroutineScope: CoroutineScope,
    user: UserViewModel
){
    var post by remember { mutableStateOf<Post?>(null) }
    var image by remember { mutableStateOf<Image?>(null) }
    var author by remember { mutableStateOf<User?>(null) }
    var authorWithBadge by remember { mutableStateOf<UserWithBadge?>(null) }
    var numLike by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var detectedText by remember { mutableStateOf<String?>(null) }
    var showTranslatedText by remember { mutableStateOf(false) }
    val localContext = LocalContext.current

    //caricamento post, immagine e autore
    LaunchedEffect(postId) {
        try {
            val postResponse = api.getPost("Token ${user.token}", postId)

            if(postResponse.isSuccessful){
                post = postResponse.body()

                //aggiornoil numero di like
                post?.likes_count?.let { likes ->
                    numLike = likes
                }

                post?.image?.let { imageId ->
                    val imgResp = api.getImage("Token ${user.token}", imageId)
                    if (imgResp.isSuccessful) image = imgResp.body()
                }

                post?.created_by?.let { userId ->
                    val userResp = api.getUserById("Token ${user.token}", userId)
                    if (userResp.isSuccessful) author = userResp.body()
                }
            }
            else{
                errorMessage = "Errore nel recupero del post"
            }
        }
        catch(e: Exception){
            errorMessage = "Errore di rete"
        }
    }

    //carica autore
    LaunchedEffect(post?.created_by) {
        try {
            val response = api.getUserById("Token ${user.token}", post!!.created_by)

            if(response.isSuccessful){
                author = response.body()
            }
            else{
                errorMessage = "Errore nel recupero dell'autore"
            }
        }
        catch(e: Exception){
            errorMessage = "Errore di rete"
        }
    }

    val currentUserId = user.user?.id
    val isAuthor = user.user?.id == post?.created_by
    var isLiked by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId, post?.liked_user_ids){
        if(currentUserId != null){
            isLiked = post?.liked_user_ids?.contains(currentUserId) == true
        }
    }

    //carica badge
    LaunchedEffect(author){
        if(author != null){
            try {
                val response = api.getBadge("Token ${user.token}", tripId, author!!.id)

                if(response.isSuccessful){
                    authorWithBadge = response.body()
                }
                else{
                    errorMessage = "Errore nel recupero del badge"
                }
            }
            catch(e: Exception){
                errorMessage = "Errore di rete"
            }
        }
    }

    //effettua OCR e traduzione al caricamento dell'immagine
    LaunchedEffect(image?.image) {
        image?.image?.let { imageUrl ->
            try {
                val loader = ImageLoader(localContext)

                val request = ImageRequest.Builder(localContext)
                    .data(imageUrl)
                    .allowHardware(false) //necessario per ottenere un Bitmap
                    .build()

                val result = withContext(Dispatchers.IO) {
                    (loader.execute(request).drawable as? BitmapDrawable)?.bitmap
                }

                val inputImage = InputImage.fromBitmap(result!!, 0)
                val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val languageIdentifier = LanguageIdentification.getClient()

                val textResult = textRecognizer.process(inputImage).await()
                val recognizedText = textResult.text

                if(recognizedText.isNotBlank()){
                    val languageCode = languageIdentifier.identifyLanguage(recognizedText).await()

                    if(languageCode != "und" && languageCode != "it"){
                        val translatorOptions = TranslatorOptions.Builder()
                            .setSourceLanguage(
                                TranslateLanguage.fromLanguageTag(languageCode)
                                    ?: TranslateLanguage.ENGLISH
                            )
                            .setTargetLanguage(TranslateLanguage.ITALIAN)
                            .build()

                        val translator = Translation.getClient(translatorOptions)
                        translator.downloadModelIfNeeded().await()

                        val translatedText = translator.translate(recognizedText).await()
                        detectedText = "Tradotto dal ${languageCode.uppercase()}: $translatedText"
                    }
                    else{
                        detectedText = "Testo rilevato (${languageCode}): $recognizedText"
                    }
                }
            }
            catch(e: Exception){
                Log.e("ImageTextDetection", "Errore riconoscimento/traduzione", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dettaglio Post") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }
                }
            )
        },
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if(!isAuthor){
                            coroutineScope.launch {
                                handleToggleLike(
                                    api = api,
                                    postId = postId,
                                    isLiked = isLiked,
                                    user = user,
                                    onSuccess = {
                                        isLiked = it
                                        numLike += if (it) 1 else -1
                                    },
                                    onError = {
                                        errorMessage = it
                                    }
                                )
                            }
                        }
                    }
                )
            }
    ) { padding ->
        if (post != null) {
            //per la gestione dei commenti
            var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
            var commentAuthorMap by remember { mutableStateOf<Map<Int, User>>(emptyMap()) }
            var loadingComments by remember { mutableStateOf(true) }
            var commentError by remember { mutableStateOf<String?>(null) }
            var commentText by remember { mutableStateOf("") }
            var submittingComment by remember { mutableStateOf(false) }
            var commentToDelete by remember { mutableStateOf<Comment?>(null) }
            var showDeleteDialog by remember { mutableStateOf(false) }
            var showDeletePostDialog by remember { mutableStateOf(false) }
            var isDeleting by remember { mutableStateOf(false) }

            //gestione dropdown modifica/elimina post
            var expanded by remember { mutableStateOf(false) }

            //caricamento posizione dell'immagine
            var address by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(image?.latitude, image?.longitude) {
                withContext(Dispatchers.IO) {
                    try{
                        val geocoder = Geocoder(localContext, Locale.getDefault())
                        val result = geocoder.getFromLocation(image!!.latitude, image!!.longitude, 1)
                        val addr = result?.firstOrNull()?.getAddressLine(0)
                        withContext(Dispatchers.Main) {
                            address = addr ?: "Indirizzo non trovato"
                        }
                    }
                    catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            address = "Errore geocoding"
                        }
                    }
                }
            }

            //caricamento dei commenti all'avvio
            LaunchedEffect(postId) {
                try {
                    val response = api.getComments("Token ${user.token}", postId)

                    if(response.isSuccessful){
                        comments = response.body() ?: emptyList()

                        //caricamento degli autori dei commenti
                        val authorIds = comments.map { it.author }.distinct()
                        val authors = mutableMapOf<Int, User>()

                        authorIds.forEach { authorId ->
                            try {
                                val userResponse = api.getUserById("Token ${user.token}", authorId)

                                if(userResponse.isSuccessful){
                                    userResponse.body()?.let { user ->
                                        authors[authorId] = user
                                    }
                                }
                            }
                            catch(_: Exception){ }
                        }

                        commentAuthorMap = authors
                    }
                    else{
                        commentError = "Impossibile caricare i commenti"
                    }
                }
                catch(e: Exception){
                    commentError = "Errore: ${e.message}"
                }
                finally{
                    loadingComments = false
                }
            }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .fillMaxSize()
                        .padding(bottom = 70.dp)
                ) {
                    //titolo e descrizione post
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = post!!.title,
                                style = MaterialTheme.typography.headlineMedium
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            handleToggleLike(
                                                api = api,
                                                postId = postId,
                                                isLiked = isLiked,
                                                user = user,
                                                onSuccess = {
                                                    isLiked = it
                                                    numLike += if (it) 1 else -1
                                                },
                                                onError = {
                                                    errorMessage = it
                                                }
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isLiked || isAuthor) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = if (isLiked) "Togli like" else "Metti like",
                                        tint = if (isLiked) Color.Red
                                        else if (isAuthor)
                                            Color.Gray
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "$numLike",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                //icona cestino solo per autore
                                if (isAuthor){
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                                    }

                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Modifica") },
                                            onClick = {
                                                expanded = false
                                                navController.navigate("edit_post/${postId}")
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Elimina") },
                                            onClick = {
                                                expanded = false
                                                showDeletePostDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        post!!.description?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        //dialog elimina post
                        if(showDeletePostDialog){
                            AlertDialog(
                                onDismissRequest = { showDeletePostDialog = false },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showDeletePostDialog = false
                                            isDeleting = true
                                            coroutineScope.launch {
                                                try {
                                                    val response = api.deletePost("Token ${user.token}", postId)

                                                    if(response.isSuccessful){
                                                        navController.popBackStack()
                                                    }
                                                    else{
                                                        errorMessage = "Errore nella cancellazione del post "
                                                    }
                                                }
                                                catch(e: Exception){
                                                    errorMessage = "Errore di rete"
                                                }
                                                finally{
                                                    isDeleting = false
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Conferma")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeletePostDialog = false }) {
                                        Text("Annulla")
                                    }
                                },
                                title = { Text("Eliminare questo post?") },
                                text = { Text("Questa azione non può essere annullata.") }
                            )
                        }
                    }

                    //sezione immagine
                    if(image != null){
                        item {
                            //immagine
                            AsyncImage(
                                model = image!!.image,
                                contentDescription = image!!.description ?: "Immagine del post",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.FillWidth
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            //descrizione immagine
                            image!!.description?.let { desc ->
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            //dettagli posizione
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    text = address ?: "Lat: ${image?.latitude} - Long: ${image?.longitude}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }

                        //sezione text detection
                        item {
                            if(detectedText != null){
                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                                            alpha = 0.5f
                                        )
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        TextButton(
                                            onClick = { showTranslatedText = !showTranslatedText },
                                            contentPadding = PaddingValues(
                                                horizontal = 8.dp,
                                                vertical = 4.dp
                                            ),
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Translate,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(
                                                text = if (showTranslatedText) "Nascondi testo rilevato" else "Mostra testo rilevato",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }

                                        AnimatedVisibility(
                                            visible = showTranslatedText,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            Text(
                                                text = detectedText ?: "",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontStyle = FontStyle.Italic
                                                ),
                                                modifier = Modifier.padding(
                                                    top = 8.dp,
                                                    start = 8.dp,
                                                    end = 8.dp
                                                ),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            //informazioni autore
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                BadgeComponent(authorWithBadge, Modifier)

                                AsyncImage(
                                    model = Constants.BASE_URL + author?.avatar,
                                    contentDescription = "Profilo di ${author?.username}",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.onPrimary,
                                            CircleShape
                                        ),
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

                            //data pubblicazione post
                            Text(
                                text = "Pubblicato il ${formatDate(post!!.created_at)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    //separatore per la sezione commenti
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Commenti",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    //stato di caricamento commenti
                    if (loadingComments) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    else if (commentError != null) {    //messaggi di errore
                        item {
                            Text(
                                text = commentError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }
                    else if (comments.isEmpty()) {  //nessun commento
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Non ci sono ancora commenti. Sii il primo a commentare!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    //lista commenti
                    items(comments) { comment ->
                        val commentAuthor = commentAuthorMap[comment.author]
                        var cAuthorWithBadge by remember { mutableStateOf<UserWithBadge?>(null) }

                        //carica badge
                        LaunchedEffect(author) {
                            if (author != null) {
                                try {
                                    val response = api.getBadge(
                                        "Token ${user.token}",
                                        tripId,
                                        commentAuthor!!.id
                                    )

                                    if (response.isSuccessful) {
                                        cAuthorWithBadge = response.body()
                                    } else {
                                        errorMessage = "Errore nel recupero del badge"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Errore di rete"
                                }
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.7f
                                )
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                //intestazione commento con avatar, nome utente e cestino
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = Constants.BASE_URL + commentAuthor?.avatar,
                                        contentDescription = "Avatar di ${commentAuthor?.username}",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.onPrimary,
                                                CircleShape
                                            ),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = commentAuthor?.username ?: "Utente sconosciuto",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))

                                    BadgeComponent(cAuthorWithBadge, Modifier)
                                    Spacer(modifier = Modifier.weight(1f))

                                    if (comment.author == user.user!!.id) {
                                        Box(
                                            modifier = Modifier
                                                .padding(5.dp)
                                                .size(24.dp)
                                                .clickable {
                                                    commentToDelete = comment
                                                    showDeleteDialog = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Elimina commento",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                //testo del commento
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                //data del commento
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = formatDate(comment.created_at),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    //spazio extra in fondo per evitare che l'ultimo commento sia nascosto dall'input
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                //dialog per l'eliminazione del commento
                if(showDeleteDialog && commentToDelete != null){
                    AlertDialog(
                        onDismissRequest = {
                            showDeleteDialog = false
                            commentToDelete = null
                        },
                        title = {
                            Text("Conferma eliminazione")
                        },
                        text = {
                            Text("Sei sicuro di voler eliminare questo commento?")
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showDeleteDialog = false
                                commentToDelete?.let { comment ->
                                    coroutineScope.launch {
                                        try {
                                            val response = api.deleteComment("Token ${user.token}", postId, comment.id)

                                            if(response.isSuccessful){
                                                comments = comments.filterNot { it.id == comment.id }
                                            }
                                            else{
                                                commentError = "Impossibile eliminare il commento"
                                            }
                                        }
                                        catch(e: Exception){
                                            commentError = "Errore durante l'eliminazione: ${e.localizedMessage}"
                                        }
                                    }
                                }
                                commentToDelete = null
                            }) {
                                Text("Elimina", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showDeleteDialog = false
                                commentToDelete = null
                            }) {
                                Text("Annulla")
                            }
                        }
                    )
                }

                //input per inserire nuovi commenti
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Scrivi un commento...") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        IconButton(
                            onClick = {
                                if(commentText.isNotBlank() && !submittingComment){
                                    submittingComment = true
                                    coroutineScope.launch {
                                        try {
                                            val request = CreateCommentRequest(commentText.trim())
                                            val response = api.createComment("Token ${user.token}", postId, request)

                                            if(response.isSuccessful){
                                                val newComment = response.body()

                                                newComment?.let {
                                                    //aggiunge il nuovo commento all'elenco
                                                    comments = listOf(it) + comments

                                                    //aggiunge l'autore alla mappa se non presente
                                                    if (!commentAuthorMap.containsKey(it.author)) {
                                                        val currentUser = user.user

                                                        currentUser?.let { user ->
                                                            commentAuthorMap = commentAuthorMap + (it.author to user)
                                                        }
                                                    }

                                                    commentText = ""
                                                }
                                            }
                                            else{
                                                throw Exception("Errore durante l'invio del commento")
                                            }
                                        }
                                        catch(e: Exception){
                                            Toast.makeText(
                                                localContext,
                                                "Impossibile inviare il commento: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        finally{
                                            submittingComment = false
                                        }
                                    }
                                }
                            },
                            enabled = commentText.isNotBlank() && !submittingComment,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = if (commentText.isNotBlank() && !submittingComment)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    shape = CircleShape
                                )
                        ) {
                            if(submittingComment){
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            }
                            else{
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Invia commento",
                                    tint = if (commentText.isNotBlank())
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        }
                    }
                }
            }
        }
        else if(errorMessage != null){
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
        else{   //loading
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
