package com.postcardmemory.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.BrutalWhite
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val detailDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

@Composable
fun PostcardDetailRow(
    postcard: Postcard,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateText = remember(postcard.capturedAt) {
        Instant.ofEpochMilli(postcard.capturedAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(detailDateFormatter)
    }

    val contentText =
        if (postcard.message.isBlank()) {
            "내용 없음"
        } else {
            postcard.message
        }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) {
                    BrutalCoral.copy(alpha = 0.22f)
                } else {
                    BrutalWhite
                }
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(
                horizontal = 16.dp,
                vertical = 14.dp
            )
    ) {
        Text(
            text = dateText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = BrutalBlack,
            modifier = Modifier.width(96.dp)
        )

        Text(
            text = contentText,
            fontSize = 14.sp,
            color = BrutalBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
