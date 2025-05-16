package com.example.frontend_triptales

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
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
                            Bacheca(trip, api, navController, user, coroutineScope)
                        }
                        1 -> {
                            Text("Mappa (...)", modifier = Modifier.padding(24.dp))
                        }
                        2 -> {
                            Classifiche(trip, api, coroutineScope)
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
    navController: NavController,
    user: UserViewModel,
    coroutineScope: CoroutineScope
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
            PostItem(api, post, navController, user, coroutineScope)
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

@Composable
fun Mappa(){}

@Composable
fun Classifiche(
    trip: Trip?,
    api: TripTalesApi,
    coroutineScope: CoroutineScope
){
    //gestione dropdown menu
    var selectedClassifica by remember { mutableStateOf("Post più apprezzati") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val classificheOptions = listOf(
        "Post più apprezzati",
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
                val token = "Token ${AuthManager.token}"

                when (selectedClassifica) {
                    "Post più apprezzati" -> {
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
                    "Post più apprezzati" -> RankingList(
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
