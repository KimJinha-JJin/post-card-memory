package com.postcardmemory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.NeutralLight
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperField
import com.postcardmemory.ui.theme.SunsetGold
import com.postcardmemory.ui.theme.SurfaceGray

@Composable
fun EditorUndoRedoButtons(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    enabled: Boolean,
    undoContentDescription: String,
    redoContentDescription: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-4).dp)
    ) {
        IconButton(
            onClick = onUndo,
            enabled = enabled && canUndo
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = undoContentDescription,
                tint = if (canUndo) BrutalBlack else GraphiteAccent,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = onRedo,
            enabled = enabled && canRedo
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Redo,
                contentDescription = redoContentDescription,
                tint = if (canRedo) BrutalBlack else GraphiteAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun EditorEmptyHint(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = BrutalBlack,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = BrutalWhite,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

/**
 * 편집 패널 공통 슬라이더.
 *
 * 얇은 4dp 트랙과 작은 16dp 원형 손잡이로 통일한다. 색상은 개편 전
 * 기존 값을 유지한다(활성·손잡이 = BrutalBlack, 비활성 = 호출부가 넘기는
 * inactiveTrackColor, 기본 NeutralLight). value·valueRange·steps·콜백은
 * 호출부에서 그대로 전달받아 기능·저장 동작은 바뀌지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    steps: Int = 0,
    enabled: Boolean = true,
    inactiveTrackColor: Color = NeutralLight
) {
    val span = valueRange.endInclusive - valueRange.start
    val fraction =
        if (span > 0f) {
            ((value - valueRange.start) / span).coerceIn(0f, 1f)
        } else {
            0f
        }
    val accentColor = if (enabled) BrutalBlack else inactiveTrackColor

    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        modifier = modifier,
        thumb = {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = accentColor,
                        shape = CircleShape
                    )
            )
        },
        track = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(inactiveTrackColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )
            }
        }
    )
}

/**
 * 스티커·도장 패널의 보조 행동(복제·삭제·회전 등)에 쓰는 가벼운 외곽선 버튼.
 *
 * 사진·배경·텍스트 패널의 보조 버튼과 같은 인상(둥근 12dp, 얇은 테두리,
 * 아이콘+라벨)을 컬렉션 패널에서도 공유한다. 일반 행동은 기본색을,
 * 위험 행동(삭제)은 borderColor·contentColor에 DangerRed 계열을 넘겨 쓴다.
 */
@Composable
fun EditorOutlineButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentColor: Color = BrutalBlack,
    borderColor: Color = SurfaceGray
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 10.dp
        ),
        modifier = modifier
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.size(6.dp))
        }

        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * 꾸미기 프리셋(스티커·도장·마스킹테이프·배경 패턴 등) 미리보기 타일의 공통 틀.
 *
 * 미리보기 박스(기본 모서리 10dp, 기본 배경 BrutalWhite) 아래에 필요하면 이름
 * 라벨(10sp SemiBold)을, 그 아래 필요하면 선택 상태 밑줄(2dp 높이 24dp 너비,
 * 선택 시 SunsetGold)을 붙인다. 미리보기 자체의 크기·모양·내용은 호출부가
 * `previewModifier`와 `preview`로 그대로 결정하므로, 스티커의 정사각형·
 * 마스킹테이프의 가로형 같은 콘텐츠 고유 비율은 그대로 유지된다. 이 컴포저블이
 * 통일하는 것은 이름·선택 표시의 위치·크기·색뿐이다.
 */
@Composable
fun DecorationPresetTile(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    previewModifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    backgroundColor: Color = BrutalWhite,
    contentPadding: PaddingValues = PaddingValues(6.dp),
    label: String? = null,
    selected: Boolean = false,
    showSelectionIndicator: Boolean = true,
    preview: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = previewModifier
                .background(color = backgroundColor, shape = shape)
                .clip(shape)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
            content = preview
        )

        if (label != null) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                color = BrutalBlack,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (showSelectionIndicator) {
            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .height(2.dp)
                    .width(24.dp)
                    .background(
                        color = if (selected) SunsetGold else Color.Transparent,
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

/**
 * 한 편집 영역 안에서 서로 다른 하위 패널 중 하나만 보여줄 때 쓰는 텍스트
 * 칩 선택줄(예: 스티커 탭의 사진/텍스트/라벨). 엽서 레이아웃 선택
 * (`PostcardLayoutPicker`)과 같은 시각 언어 — 균등폭 칩, 선택 시 SunsetGold
 * 옅은 채움 + 굵은 글씨, 그 외엔 PaperField 배경 + PaperDivider 테두리 —
 * 를 재사용한다. 어떤 패널을 보여줄지만 나타내는 화면 로컬 선택 상태이므로
 * 저장값이나 Room과는 무관하다.
 */
@Composable
fun EditorSegmentedTabRow(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val shape = RoundedCornerShape(12.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .background(
                        color = if (selected) SunsetGold.copy(alpha = 0.16f) else PaperField,
                        shape = shape
                    )
                    .border(
                        width = 1.dp,
                        color = PaperDivider,
                        shape = shape
                    )
                    .clickable(enabled = enabled) {
                        onOptionSelected(index)
                    }
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    color = BrutalBlack,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
