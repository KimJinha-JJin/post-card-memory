package com.postcardmemory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.pastelColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun StampCard(
    postcard: Postcard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation = remember(postcard.id) {
        val seed = postcard.id.hashCode()
        (abs(seed) % 70 - 35) / 10f  // -3.5 to +3.5 degrees
    }
    val cardColor = remember(postcard.id) {
        pastelColors[abs(postcard.id.toInt()) % pastelColors.size]
    }
    val dateFormatter = remember { SimpleDateFormat("MM.dd.yyyy", Locale.getDefault()) }

    Box(
        modifier = modifier
            .rotate(rotation)
            .padding(4.dp)
    ) {
        // Offset shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = 4.dp, y = 4.dp)
                .background(
                    color = BrutalBlack,
                    shape = RoundedCornerShape(8.dp)
                )
                .aspectRatio(3f / 4.5f)
        )

        // Main card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = cardColor, shape = RoundedCornerShape(8.dp))
                .border(width = 3.dp, color = BrutalBlack, shape = RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stamp photo area with perforated border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
            ) {
                // Photo
                AsyncImage(
                    model = File(postcard.imagePath),
                    contentDescription = postcard.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                // Perforated border overlay
                StampBorderCanvas(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = Color.Transparent,
                    perfSize = 7.dp,
                    strokeWidth = 2.5.dp
                )
            }

            // Date label
            Text(
                text = dateFormatter.format(Date(postcard.capturedAt)),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = BrutalBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
