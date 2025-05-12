package com.example.frontend_triptales

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailPage(
    api: TripTalesApi,
    postId: Int,
    navController: NavController
){
    var post by remember { mutableStateOf<Post?>(null) }
    var image by remember { mutableStateOf<Image?>(null) }
    var author by remember { mutableStateOf<User?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var detectedText by remember { mutableStateOf<String?>(null) }
    var showTranslatedText by remember { mutableStateOf(false) }

    //caricamento post, immagine e autore
    LaunchedEffect(postId) {
        try {
            val postResponse = api.getPost("Token ${AuthManager.token}", postId)

            if(postResponse.isSuccessful){
                post = postResponse.body()

                post?.image?.let { imageId ->
                    val imgResp = api.getImage("Token ${AuthManager.token}", imageId)
                    if (imgResp.isSuccessful) image = imgResp.body()
                }

                post?.created_by?.let { userId ->
                    val userResp = api.getUserById("Token ${AuthManager.token}", userId)
                    if (userResp.isSuccessful) author = userResp.body()
                }
            }
            else{
                errorMessage = "Errore nel recupero del post"
            }
        }
        catch(e: Exception){
            errorMessage = "Errore: ${e.localizedMessage}"
        }
    }

    //effettua OCR e traduzione al caricamento dell'immagine
    LaunchedEffect(image?.image) {
        image?.image?.let { imageUrl ->
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val input = URL(imageUrl).openStream()
                    BitmapFactory.decodeStream(input)
                }

                val inputImage = InputImage.fromBitmap(bitmap, 0)
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
                        detectedText = "Testo rilevato: $recognizedText"
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
        }
    ) { padding ->
        if (post != null) {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            ) {
                //titolo e descrizione post
                item {
                    Text(
                        text = post!!.title,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    post!!.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
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
                                .height(300.dp)
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
                                text = "Lat: ${image!!.latitude} - Long: ${image!!.longitude}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    //sezione text detection
                    item {
                        if (detectedText != null) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    TextButton(
                                        onClick = { showTranslatedText = !showTranslatedText },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
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
                                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                            modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
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

                        //data pubblicazione post
                        Text(
                            text = "Pubblicato il ${formatDate(post!!.created_at)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
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
