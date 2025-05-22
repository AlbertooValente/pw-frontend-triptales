package com.example.frontend_triptales

import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.core.graphics.scale
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.android.gms.maps.CameraUpdateFactory
import androidx.core.graphics.createBitmap

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
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = bacheca, 1 = mappa, 2 = classifica
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }

    LaunchedEffect(id) {
        try {
            val response = api.getTripInfo("Token ${user.token}", id)

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

    //carica post
    LaunchedEffect(selectedTab, trip?.id) {
        if(trip != null){
            val response = api.getPosts("Token ${user.token}", trip!!.id)

            if(response.isSuccessful){
                posts = response.body() ?: emptyList()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    trip != null -> {
                        when (selectedTab) {
                            0 -> {
                                Bacheca(trip, posts, api, navController, user, coroutineScope)
                            }
                            1 -> {
                                var postWithImages by remember { mutableStateOf<List<PostWithImage>>(emptyList()) }

                                LaunchedEffect(Unit) {
                                    postWithImages = loadPostWithImages(api, posts, user)
                                }

                                Mappa(postWithImages, navController)
                            }
                            2 -> {
                                Classifiche(trip, api, coroutineScope, user)
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

                //pulsante aggiungi post
                if(selectedTab == 0 && trip != null){
                    FloatingActionButton(
                        onClick = { navController.navigate("create_post/${id}") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aggiungi"
                        )
                    }
                }
            }

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
    }
}

//gestione bacheca
@Composable
fun Bacheca(
    trip: Trip?,
    posts: List<Post>,
    api: TripTalesApi,
    navController: NavController,
    user: UserViewModel,
    coroutineScope: CoroutineScope
) {
    var showDialog by remember { mutableStateOf(false) }

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

                    //opzionale descrizione
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
            PostItem(api, post, navController, user, coroutineScope, trip!!.id)
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
    if(showDialog && trip != null){
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Chiudi")
                }
            },
            title = {
                Text(
                    text = trip.name,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "Descrizione: ${trip.description}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Codice di accesso: ${trip.id}",
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


//gestione mappa
data class PostWithImage(
    val post: Post,
    val image: Image
)

suspend fun loadPostWithImages(api: TripTalesApi, posts: List<Post>, user: UserViewModel): List<PostWithImage> {
    return posts.mapNotNull { post ->
        try{
            val response = api.getImage("Token ${user.token}", post.image)

            if(response.isSuccessful && response.body() != null){
                PostWithImage(post, response.body()!!)
            }
            else null
        }
        catch(e: Exception){
            null
        }
    }
}

@Composable
fun Mappa(
    postWithImages: List<PostWithImage>,
    navController: NavController
) {
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(postWithImages){
        val first = postWithImages.firstOrNull()?.image

        if(first != null){
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    LatLng(first.latitude, first.longitude),
                    12f
                )
            )
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        postWithImages.forEach { item ->
            val lat = item.image.latitude
            val lon = item.image.longitude
            val descriptor by rememberBitmapDescriptorFromUrl(item.image.image)

            if(descriptor != null){
                Marker(
                    state = MarkerState(position = LatLng(lat, lon)),
                    title = item.post.title,
                    snippet = "Tap per dettagli",
                    onClick = {
                        navController.navigate("postDetailPage/${item.post.id}")
                        true
                    },
                    icon = descriptor
                )
            }
        }
    }
}

@Composable
fun rememberBitmapDescriptorFromUrl(fullUrl: String): State<BitmapDescriptor?> {
    val context = LocalContext.current

    return produceState<BitmapDescriptor?>(initialValue = null, key1 = fullUrl) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(fullUrl)
                .allowHardware(false)
                .build()

            val result = (loader.execute(request) as? SuccessResult)?.drawable
            val bitmap = (result as? BitmapDrawable)?.bitmap

            if(bitmap != null){
                val size = 120
                val radius = size / 2f
                val padding = 6f // spazio per il bordo

                //crea una bitmap per l'immagine circolare
                val circularBitmap = createBitmap(size, size)
                val canvas = Canvas(circularBitmap)

                val shader = BitmapShader(
                    bitmap.scale(size, size, false),
                    Shader.TileMode.CLAMP,
                    Shader.TileMode.CLAMP
                )

                val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.shader = shader
                }

                canvas.drawCircle(radius, radius, radius, imagePaint)

                //crea il bitmap finale con bordo + triangolino in basso
                val finalHeight = size + 30
                val finalBitmap = createBitmap(size + padding.toInt() * 2, finalHeight + padding.toInt())
                val finalCanvas = Canvas(finalBitmap)

                //disegna bordo (cerchio leggermente più grande)
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.RED
                }

                finalCanvas.drawCircle(
                    radius + padding,
                    radius + padding,
                    radius + 4f,
                    bgPaint
                )

                //disegna l'immagine circolare sopra il bordo
                finalCanvas.drawBitmap(circularBitmap, padding, padding, null)

                //disegna il triangolino/puntatore
                val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.RED
                }

                val path = Path().apply {
                    moveTo(radius + padding - 20, size + padding)
                    lineTo(radius + padding + 20, size + padding)
                    lineTo(radius + padding, finalHeight + padding)
                    close()
                }

                finalCanvas.drawPath(path, pointerPaint)

                //converte in BitmapDescriptor
                value = BitmapDescriptorFactory.fromBitmap(finalBitmap)
            }
            else{
                value = null
            }
        }
        catch(_: Exception){
            value = null
        }
    }
}


//gestione classifiche
@Composable
fun Classifiche(
    trip: Trip?,
    api: TripTalesApi,
    coroutineScope: CoroutineScope,
    user: UserViewModel
){
    //gestione dropdown menu
    var selectedClassifica by remember { mutableStateOf("Post con più like") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val classificheOptions = listOf(
        "Post con più like",
        "Utenti con più like",
        "Utenti con più post"
    )

    //dati delle varie classifiche
    var topLikePosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var topLikeUsers by remember { mutableStateOf<List<UserLikes>>(emptyList()) }
    var topPosters by remember { mutableStateOf<List<UserPosts>>(emptyList()) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val localContext = LocalContext.current

    //funzione per caricare i dati della classifica selezionata
    fun loadClassificaData() {
        if (trip == null) return

        coroutineScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val token = "Token ${user.token}"

                when (selectedClassifica) {
                    "Post con più like" -> {
                        val response = api.getTopLike(token, trip.id)

                        if(response.isSuccessful){
                            topLikePosts = response.body() ?: emptyList()
                        }
                        else{
                            errorMessage = "Impossibile caricare i post più apprezzati"
                        }
                    }
                    "Utenti con più like" -> {
                        val response = api.getTopLikeUser(token, trip.id)

                        if(response.isSuccessful){
                            topLikeUsers = response.body() ?: emptyList()
                        }
                        else{
                            errorMessage = "Impossibile caricare gli utenti con più like"
                        }
                    }
                    "Utenti con più post" -> {
                        val response = api.getTopPosters(token, trip.id)

                        if(response.isSuccessful){
                            topPosters = response.body() ?: emptyList()
                        }
                        else{
                            errorMessage = "Impossibile caricare gli utenti con più post"
                        }
                    }
                }
            }
            catch(e: Exception){
                errorMessage = "Errore: ${e.message}"
                Toast.makeText(
                    localContext,
                    "Errore nel caricamento: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            finally{
                isLoading = false
            }
        }
    }

    //carica i dati quando cambia la selezione
    LaunchedEffect(selectedClassifica, trip) {
        if (trip != null) {
            loadClassificaData()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Classifiche del viaggio",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        //dropdown per selezione classifica
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isDropdownExpanded = true },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedClassifica,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Icon(
                        imageVector = if (isDropdownExpanded)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = "Espandi menu"
                    )
                }
            }

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                classificheOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option) },
                        onClick = {
                            selectedClassifica = option
                            isDropdownExpanded = false
                        },
                        trailingIcon = {
                            if(selectedClassifica == option){
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selezionato",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            }
        }

        when {
            isLoading -> {
                BoxWithLoader()
            }

            errorMessage != null -> {
                ErrorMessage(errorMessage!!)
            }

            else -> {
                when (selectedClassifica) {
                    "Post con più like" -> RankingList(
                        items = topLikePosts,
                        emptyMessage = "Nessun post disponibile per questa classifica",
                        itemContent = { index, item -> PostRankingItem(item, index + 1) }
                    )

                    "Utenti con più like" -> RankingList(
                        items = topLikeUsers,
                        emptyMessage = "Nessun utente disponibile per questa classifica",
                        itemContent = { index, item ->
                            UserRankingItem(
                                username = item.username,
                                value = item.total_likes,
                                position = index + 1,
                                valueLabel = "like"
                            )
                        }
                    )

                    "Utenti con più post" -> RankingList(
                        items = topPosters,
                        emptyMessage = "Nessun utente disponibile per questa classifica",
                        itemContent = { index, item ->
                            UserRankingItem(
                                username = item.username,
                                value = item.total_posts,
                                position = index + 1,
                                valueLabel = "post"
                            )
                        }
                    )
                }
            }
        }
    }
}
