package com.postcardmemory.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.components.EditorSlider
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.GraphiteAccent
import kotlin.math.roundToInt

/**
 * 사진·배경·텍스트 패널이 공유하는 퍼센트 조절 슬라이더.
 *
 * 라벨과 현재값을 한 줄에 두고 그 아래 얇은 EditorSlider만 둔다. 숫자
 * 직접 입력창과 최소·최대 범위 안내 문구는 없앴다. 드래그 중에는 로컬
 * 값을 보여 주고, 손을 떼면 외부 percent(저장 결과)를 그대로 따른다.
 * onPreviewPercentChanged·onPercentConfirmed 계약은 기존 TextSizeControl과
 * 동일해 미리보기·저장·Undo 스냅샷 로직이 그대로 유지된다.
 */
@Composable
internal fun EditorPercentSlider(
    label: String,
    percent: Int,
    minPercent: Int,
    maxPercent: Int,
    enabled: Boolean,
    onPreviewPercentChanged: (Int) -> Unit,
    onPercentConfirmed: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var draggingPercent by remember { mutableIntStateOf(percent) }
    val shownPercent = if (isDragging) draggingPercent else percent

    Column(
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = BrutalBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "$shownPercent%",
                color = GraphiteAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        EditorSlider(
            value = shownPercent.toFloat(),
            onValueChange = { newValue ->
                val snappedPercent =
                    ((newValue / 5f).roundToInt() * 5)
                        .coerceIn(minPercent, maxPercent)

                isDragging = true
                draggingPercent = snappedPercent
                onPreviewPercentChanged(snappedPercent)
            },
            onValueChangeFinished = {
                isDragging = false
                onPercentConfirmed(draggingPercent)
            },
            valueRange =
                minPercent.toFloat()..maxPercent.toFloat(),
            steps =
                ((maxPercent - minPercent) / 5) - 1,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
