package com.example.frontend_triptales

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Translate
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
    LaunchedEffect(postId){
        try{
            val postResponse = api.getPost("Token ${AuthManager.token}", postId)

            if(postResponse.isSuccessful){
                post = postResponse.body()

                //carica immagine
                val imgResp = api.getImage("Token ${AuthManager.token}", post!!.image)
                if (imgResp.isSuccessful) image = imgResp.body()

                //carica autore
                val userResp = api.getUser("Token ${AuthManager.token}")
                if (userResp.isSuccessful) author = userResp.body()
            }
            else{
                errorMessage = "Errore nel recupero del post"
            }
        }
        catch(e: Exception){
            errorMessage = "Errore: ${e.localizedMessage}"
        }
    }

    // Effettua OCR e traduzione al caricamento dell'immagine
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
                            .setSourceLanguage(TranslateLanguage.fromLanguageTag(languageCode)
                                ?: TranslateLanguage.ENGLISH)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        post?.let { p ->
            LazyColumn(modifier = Modifier
                .padding(padding)
                .padding(16.dp)) {
                item {
                    Text(p.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    p.description?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if(image != null){
                    item {
                        AsyncImage(
                            model = image?.image,
                            contentDescription = image?.description ?: "Immagine del post",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.5f)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )

                        image?.description?.let { desc ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                desc,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Lat: ${image?.latitude} - Long: ${image?.longitude}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    //se è stato rilevato del testo mostra il pulsante per vederlo
                    item {
                        if(detectedText != null){
                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(onClick = { showTranslatedText = !showTranslatedText }) {
                                Icon(
                                    imageVector = Icons.Outlined.Translate,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showTranslatedText) "Nascondi testo rilevato" else "Mostra testo rilevato")
                            }

                            if(showTranslatedText){
                                Text(
                                    text = detectedText ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
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

                        //data post
                        Text(
                            text = "Pubblicato il ${formatDate(p.created_at)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
