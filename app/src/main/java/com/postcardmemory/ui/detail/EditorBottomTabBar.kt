package com.postcardmemory.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.SunsetGold

/**
 * 상세 편집 화면 하단에 고정되는 편집 카테고리 전환용 도크. 스크롤 콘텐츠
 * 밖 루트 Box에 얹혀 편집 내용을 위아래로 움직여도 위치가 바뀌지 않는다.
 * Pager 상태·선택 표현·탭 이동 콜백은 기존 인라인 탭 바와 동일하게 유지한다.
 *
 * 카테고리 목록 자체는 이 컴포저블이 갖지 않고 labels/icons로 받는다 —
 * 실제 목록과 개수는 호출부(DetailScreen의 customizationPageLabels)가
 * 정하므로 여기에 개수를 적어두지 않는다.
 */
@Composable
internal fun EditorBottomTabBar(
    selectedPage: Int,
    labels: List<String>,
    icons: List<ImageVector>,
    enabled: Boolean,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(0.92f)
    ) {
        labels.forEachIndexed { pageIndex, pageLabel ->
            val pageSelected = selectedPage == pageIndex
            val tabColor =
                if (pageSelected) SunsetGold else GraphiteAccent

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = enabled) {
                        onTabSelected(pageIndex)
                    }
                    .semantics {
                        contentDescription = "$pageLabel 편집"
                    }
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icons[pageIndex],
                    contentDescription = null,
                    tint = tabColor,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = pageLabel,
                    color = tabColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(3.dp))

                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .fillMaxWidth(0.5f)
                        .background(
                            color =
                                if (pageSelected) {
                                    SunsetGold
                                } else {
                                    Color.Transparent
                                },
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}
