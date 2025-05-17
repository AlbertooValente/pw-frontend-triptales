package com.example.frontend_triptales

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

//mostra indicatore di caricamento
@Composable
fun BoxWithLoader(){
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

//mostra errore
@Composable
fun ErrorMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

//mostra una lista di qualsiasi tipo T, se è vuota mostra EmptyListMessage
@Composable
fun <T> RankingList(
    items: List<T>,
    emptyMessage: String,
    itemContent: @Composable (Int, T) -> Unit
) {
    if(items.isEmpty()){
        EmptyListMessage(emptyMessage)
        return
    }

    LazyColumn {
        itemsIndexed(items) { index, item ->
            itemContent(index, item)

            /*
            if(index < items.size - 1){
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            }
             */
        }
    }
}

//elemento per rappresentare un item di classifica
@Composable
fun RankingItemCard(
    position: Int,
    content: @Composable () -> Unit,
    value: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //posizione in classifica
            PositionBadge(position)
            Spacer(modifier = Modifier.width(16.dp))

            //contenuto personalizzato
            content()

            //indicatore di valore personalizzato
            value()
        }
    }
}

//mostra la posizione in classifica
@Composable
fun PositionBadge(position: Int) {
    val backgroundColor = when (position) {
        1 -> Color(0xFFFFD700)  //oro
        2 -> Color(0xFFC0C0C0)  //argento
        3 -> Color(0xFFCD7F32)  //bronzo
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (position <= 3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                color = backgroundColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = position.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

//crea un item per la classifica dei post
@Composable
fun PostRankingItem(post: Post, position: Int) {
    RankingItemCard(
        position = position,
        content = {
            //contenuto del post
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                post.description?.let {
                    if(it.isNotEmpty()){
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        value = {
            //numero di like
            ValueIndicator(
                icon = Icons.Filled.Favorite,
                value = post.likes_count,
                tint = Color.Red
            )
        }
    )
}

//crea un item per la classifica degli user
@Composable
fun UserRankingItem(
    username: String,
    value: Int,
    position: Int,
    valueLabel: String
) {
    val icon = if (valueLabel == "like") Icons.Filled.Favorite else Icons.Default.PostAdd
    val tint = if (valueLabel == "like") Color.Red else MaterialTheme.colorScheme.primary

    RankingItemCard(
        position = position,
        content = {
            //nome utente
            Text(
                text = username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        value = {
            //valore (numero di like o post)
            ValueIndicator(
                icon = icon,
                value = value,
                tint = tint
            )
        }
    )
}

//mostra un icona con un valore numerico associato al post o user
@Composable
fun ValueIndicator(
    icon: ImageVector,
    value: Int,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "$value",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

//mostra un messaggio quando la classifica è vuota
@Composable
fun EmptyListMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}