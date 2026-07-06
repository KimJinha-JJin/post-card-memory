package com.postcardmemory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.NeutralLight
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.BrutalYellow
import com.postcardmemory.ui.theme.SoftGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PostcardDateFormat(
    val label: String,
    val previewText: String,
    private val pattern: String,
    private val locale: Locale,
    private val uppercase: Boolean = false
) {
    DOT(
        label = "점 표기",
        previewText = "2026.07.01",
        pattern = "yyyy.MM.dd",
        locale = Locale.KOREAN
    ),
    KOREAN(
        label = "한글 표기",
        previewText = "2026년 7월 1일",
        pattern = "yyyy년 M월 d일",
        locale = Locale.KOREAN
    ),
    ENGLISH_LONG(
        label = "영문 긴 표기",
        previewText = "JULY 1, 2026",
        pattern = "MMMM d, yyyy",
        locale = Locale.ENGLISH,
        uppercase = true
    ),
    ENGLISH_SHORT(
        label = "영문 짧은 표기",
        previewText = "01 JUL 2026",
        pattern = "dd MMM yyyy",
        locale = Locale.ENGLISH,
        uppercase = true
    );

    fun format(
        capturedAt: Long
    ): String {
        val formatted =
            SimpleDateFormat(
                pattern,
                locale
            ).format(
                Date(capturedAt)
            )

        return if (uppercase) {
            formatted.uppercase(locale)
        } else {
            formatted
        }
    }
}

@Composable
fun PostcardDateFormatPicker(
    selectedFormat: PostcardDateFormat,
    onFormatSelected: (PostcardDateFormat) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .alpha(
                if (enabled) {
                    1f
                } else {
                    0.55f
                }
            )
            .background(
                color = SoftGray,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 2.dp,
                color = BrutalBlack,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(
                horizontal = 16.dp,
                vertical = 16.dp
            )
    ) {
        Text(
            text = "날짜 형식",
            color = BrutalBlack,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = "날짜 모양을 누르면 엽서 미리보기에 바로 반영돼.",
            color = BrutalBlack,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    rememberScrollState()
                )
                .padding(
                    end = 4.dp,
                    bottom = 4.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            PostcardDateFormat.entries.forEach { format ->
                DateFormatOption(
                    format = format,
                    selected =
                        format == selectedFormat,
                    enabled = enabled,
                    onClick = {
                        onFormatSelected(format)
                    }
                )
            }
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = "선택한 날짜 형식은 자동으로 저장돼.",
            color = BrutalBlack,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = NeutralLight,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                )
        )
    }
}

@Composable
private fun DateFormatOption(
    format: PostcardDateFormat,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val cardShape =
        RoundedCornerShape(14.dp)

    Box {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(
                    x = 4.dp,
                    y = 4.dp
                )
                .background(
                    color = BrutalBlack,
                    shape = cardShape
                )
        )

        Column(
            modifier = Modifier
                .width(150.dp)
                .background(
                    color =
                        if (selected) {
                            BrutalYellow
                        } else {
                            BrutalWhite
                        },
                    shape = cardShape
                )
                .border(
                    width =
                        if (selected) {
                            3.dp
                        } else {
                            2.dp
                        },
                    color = BrutalBlack,
                    shape = cardShape
                )
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 12.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text = format.previewText,
                color = BrutalBlack,
                fontFamily = FontFamily.Serif,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = format.label,
                color = BrutalBlack,
                fontSize = 12.sp,
                fontWeight =
                    if (selected) {
                        FontWeight.ExtraBold
                    } else {
                        FontWeight.Bold
                    },
                textAlign = TextAlign.Center
            )
        }
    }
}
