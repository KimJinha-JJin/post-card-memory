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

/**
 * 편집 탭 안의 세부 종류(사진: 레이아웃/사진 편집, 배경: 색상/패턴, 스티커:
 * 사진/텍스트/라벨, 마스킹테이프: 기본 디자인/커스텀/사진, 낙서: 도구)를
 * 고르는 선택줄. 53일차 스티커 탭에서 처음 만들어졌고, 같은 역할에는 같은
 * 문법을 적용한다는 원칙에 따라 다섯 탭이 이 컴포저블을 재사용한다.
 *
 * 101일차: 칸마다 종이 띠 라벨을 깔고 선택된 칸만 월 라벨 톤 띠·굵은 글자·
 * 빠끔 얼굴로 표시한다(이전의 갈색 단색 채움과 칸 사이 세로선은 없앴다).
 * 그림과 칸 배치는 [PaperLabelSubcategoryNavRow]가 맡는다.
 *
 * EditorBottomTabBar와 마찬가지로 스크롤 콘텐츠 밖 고정 영역에서만 쓰인다
 * — 호출부가 이 컴포저블을 EditorBottomTabBar와 같은 고정 Box 안에 둔다.
 */
@Composable
internal fun EditorSubcategoryNavBar(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    PaperLabelSubcategoryNavRow(
        options = options,
        selectedIndex = selectedIndex,
        onOptionSelected = onOptionSelected,
        enabled = enabled,
        modifier = modifier
    )
}
