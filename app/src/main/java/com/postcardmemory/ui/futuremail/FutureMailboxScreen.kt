package com.postcardmemory.ui.futuremail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.postcardmemory.ui.theme.GalleryPaperWhite
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.ui.theme.PaperTray
import com.postcardmemory.ui.theme.SunsetGold
import com.postcardmemory.ui.theme.SurfaceGray
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 미래 우체통. 갤러리와 나란한 세 번째 "보기 방식"이 아니라, 갤러리 밖의
 * 별도 화면이다 — 배송 중인 엽서의 날짜·건수만 보여주고, 사진·문구·스티커·
 * 도장 등 내용은 절대 노출하지 않는다(그룹 데이터 자체가 postcardId만
 * 들고 있어 이 화면이 실수로라도 내용을 그릴 방법이 없다).
 */
@Composable
fun FutureMailboxScreen(
    onNavigateBack: () -> Unit,
    viewModel: FutureMailboxViewModel = hiltViewModel()
) {
    val groups by viewModel.groups.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.openedMessages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = GalleryPaperWhite,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GalleryPaperWhite)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = InkPrimary
                        )
                    }

                    Text(
                        text = "📮 미래 우체통",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                HorizontalDivider(color = SurfaceGray, thickness = 1.dp)
            }
        }
    ) { paddingValues ->
        if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GalleryPaperWhite)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            color = PaperTray,
                            shape = CircleShape
                        )
                        .padding(horizontal = 38.dp, vertical = 32.dp)
                ) {
                    Text(text = "📮", fontSize = 64.sp)

                    Text(
                        text = "아직 떠난 엽서가 없어요",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = groups,
                    key = { it.deliverAtMillis }
                ) { group ->
                    FutureMailGroupCard(
                        group = group,
                        onOpen = { viewModel.openArrivedGroup(group) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FutureMailGroupCard(
    group: FutureMailGroup,
    onOpen: () -> Unit
) {
    val dateLabel = remember(group.deliverAtMillis) {
        DateTimeFormatter
            .ofPattern("yyyy.MM.dd")
            .format(Instant.ofEpochMilli(group.deliverAtMillis).atZone(ZoneId.systemDefault()))
    }

    val daysLeft = remember(group.deliverAtMillis) {
        daysUntilFutureMail(group.deliverAtMillis, System.currentTimeMillis())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PaperSurface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (group.arrived) "💌 $dateLabel" else "📮 $dateLabel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary
                )

                Text(
                    text = buildString {
                        append("${group.count}건")
                        if (!group.arrived && daysLeft > 0) {
                            append(" · D-$daysLeft")
                        }
                        append(" · ${group.progressPercent}%")
                    },
                    fontSize = 13.sp,
                    color = InkSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (group.arrived) {
                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SunsetGold,
                        contentColor = PaperSurface
                    )
                ) {
                    Text("열어보기")
                }
            } else {
                Text(
                    text = "여행 중",
                    fontSize = 13.sp,
                    color = InkSecondary
                )
            }
        }

        FutureMailNavigationLine(
            progressPercent = group.progressPercent,
            daysLeft = daysLeft,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun FutureMailNavigationLine(
    progressPercent: Int,
    daysLeft: Long,
    modifier: Modifier = Modifier
) {
    val shipIndex = remember(progressPercent) {
        futureMailShipSlotIndex(progressPercent)
    }
    val accessibilityLabel = buildString {
        append("엽서 여행 진행도 ${progressPercent}퍼센트")
        if (daysLeft > 0) {
            append(", 도착까지 ${daysLeft}일")
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = accessibilityLabel
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "\u25B4",
            fontSize = 13.sp,
            color = InkSecondary
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(FUTURE_MAIL_ROUTE_SLOT_COUNT) { index ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (index == shipIndex) "\u26F5\uFE0E" else "\u00B7",
                        fontSize = 13.sp,
                        color = InkSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Text(
            text = "\u25B4",
            fontSize = 13.sp,
            color = InkSecondary
        )
    }
}

private const val FUTURE_MAIL_ROUTE_SLOT_COUNT = 11
