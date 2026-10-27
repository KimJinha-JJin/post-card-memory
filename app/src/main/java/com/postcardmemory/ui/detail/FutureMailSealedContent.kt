package com.postcardmemory.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperTray
import com.postcardmemory.ui.theme.ScreenBackgroundGray
import com.postcardmemory.ui.theme.SunsetGold

/**
 * 미래로 발송된(futureMailState=SENT) 엽서를 detail/{id} 딥링크로 직접
 * 열었을 때 보여주는 봉인 안내 화면. 사진/문구/스티커/도장 등 실제 내용은
 * 절대 노출하지 않는다 — [DetailScreen]에서 postcard가 SENT면 이 화면만
 * 그리고 나머지 편집 UI 트리는 아예 컴포지션되지 않는다.
 *
 * 도착 여부·남은 일수·도착일 문자열은 모두 호출부에서 미리 계산해 전달한다.
 * 이 Composable은 값을 그릴 뿐 날짜를 직접 파싱·포맷하지 않는다.
 */
@Composable
internal fun FutureMailSealedContent(
    arrived: Boolean,
    daysLeft: Long?,
    formattedDate: String?,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackgroundGray)
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = BrutalBlack
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = PaperTray,
                        shape = CircleShape
                    )
                    .padding(
                        horizontal = 38.dp,
                        vertical = 32.dp
                    )
            ) {
                Text(
                    text = "💌",
                    fontSize = 64.sp
                )
            }

            Text(
                text = if (arrived) "엽서가 도착했어요" else "아직 여행 중이에요",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = InkPrimary,
                modifier = Modifier.padding(top = 24.dp)
            )

            Text(
                text = if (arrived) {
                    "미래 우체통에서 열어보기를 눌러야\n다시 볼 수 있어요."
                } else {
                    "한 번 보낸 엽서는\n다시 열어볼 수 없어요."
                },
                fontSize = 15.sp,
                color = InkSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )

            if (!arrived && formattedDate != null) {
                Text(
                    text = "${formattedDate}에 다시 만나요.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (!arrived && daysLeft != null && daysLeft > 0) {
                Text(
                    text = "D-$daysLeft",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SunsetGold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
