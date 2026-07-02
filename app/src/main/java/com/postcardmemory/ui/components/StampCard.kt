package com.postcardmemory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.pastelColors
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

    val cardColor = remember(postcard.id) {
        pastelColors[abs(postcard.id.toInt()) % pastelColors.size]
    }

    val dateFormatter = remember {
        SimpleDateFormat(
            "MM.dd.yyyy",
            Locale.getDefault()
        )
    }

    val borderColor =
        if (isSelected) {
            BrutalCoral
        } else {
            BrutalBlack
        }

    val borderWidth =
        if (isSelected) {
            5.dp
        } else {
            3.dp
        }

    Box(
        modifier = modifier
            .rotate(rotation)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(
                    x = 4.dp,
                    y = 4.dp
                )
                .background(
                    color = BrutalBlack,
                    shape = RoundedCornerShape(8.dp)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = cardColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .pointerInput(
                    onClick,
                    onLongClick
                ) {
                    detectTapGestures(
                        onTap = {
                            onClick()
                        },
                        onLongPress = {
                            onLongClick()
                        }
                    )
                }
                .padding(8.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            StampPhoto(
                imagePath = postcard.imagePath,
                contentDescription = postcard.title,
                modifier = Modifier.fillMaxWidth(),
                outlineColor = Color.White,
                outlineWidth = 3f
            )

            Text(
                text = dateFormatter.format(
                    Date(postcard.capturedAt)
                ),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = BrutalBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
