package com.postcardmemory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.BrutalWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun StampCard(
    postcard: Postcard,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rotation = remember(postcard.id) {
        val seed = postcard.id.hashCode()
        (abs(seed) % 70 - 35) / 10f
    }

    val dateFormatter = remember {
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        )
    }

    Box(
        modifier = modifier
            .rotate(rotation)
            .padding(4.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        StampPhoto(
            imagePath = postcard.imagePath,
            contentDescription = postcard.title,
            modifier = Modifier.fillMaxWidth(),
            outlineColor = Color.White,
            outlineWidth = 3f
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(PinkingPhotoShape)
                    .background(
                        color = BrutalCoral.copy(alpha = 0.3f)
                    )
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .background(
                    color = BrutalWhite,
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(
                    horizontal = 6.dp,
                    vertical = 2.dp
                )
        ) {
            Text(
                text = dateFormatter.format(
                    Date(postcard.capturedAt)
                ),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = BrutalBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(14.dp)
                    .background(
                        color = BrutalCoral,
                        shape = CircleShape
                    )
            )
        }
    }
}
