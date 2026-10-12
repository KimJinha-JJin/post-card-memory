package com.postcardmemory.ui.detail

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.R
import com.postcardmemory.ui.gallery.GALLERY_DATE_STRIP_CORAL_FILTER
import com.postcardmemory.ui.theme.InkPrimary
import kotlin.math.roundToInt

// 101일차: 하단 세부 선택줄(사진·배경·스티커·마스킹테이프·낙서 다섯 탭 공통)의 칸마다 갤러리 월 라벨과 같은 종이 띠
// (gallery_date_paper_strip.png)를 깐다. 미선택은 연회색, 선택은 월 라벨과 같은 바랜
// 코랄 띠에 굵은 글자이고, 띠 위에 얼굴 하나가 앞발을 걸치고 빠끔 내다본다. 진한 채움도,
// 라벨 둘레 펜 고리도 쓰지 않는다(실기기 QA에서 펜 고리는 혼란스럽다고 빠졌다). 칸(터치
// 영역)은 기존과 같고, 종이는 칸 안쪽에 그림으로만
// 얹힌다. 값은 실기기 QA로 다듬는 미감 값이다.
private const val PAPER_LABEL_HEIGHT_DP = 25f
private const val PAPER_LABEL_INSET_X_DP = 6f

// 원본(2172×724)은 작은 라벨에 비해 커서 1/4로 줄여 불러온다(약 0.4MB). 종이 영역 좌표는
// 갤러리 월 라벨이 쓰는 원본 기준 값(89,172 / 2011×379)을 같은 비율로 줄인 것이다.
private const val PAPER_LABEL_STRIP_SAMPLE = 4
private val PAPER_LABEL_STRIP_SRC_OFFSET = IntOffset(89 / PAPER_LABEL_STRIP_SAMPLE, 172 / PAPER_LABEL_STRIP_SAMPLE)
private val PAPER_LABEL_STRIP_SRC_SIZE = IntSize(2011 / PAPER_LABEL_STRIP_SAMPLE, 379 / PAPER_LABEL_STRIP_SAMPLE)

/**
 * 미선택 라벨: 민트 테이프 그림의 밝기만 남겨 연한 회색으로 곱한다 — 종이 결·찢긴
 * 가장자리는 살리고 색만 바꾼다. 띠 평균 밝기(≈204)가 연회색 #D9D5CF 근처(화면 배경
 * #F4ECDE보다 한 단계 어두워 묻히지 않는 정도)로 오도록 맞춘 비율이다. 선택된 라벨은
 * 갤러리 월 라벨과 같은 바랜 코랄([GALLERY_DATE_STRIP_CORAL_FILTER])로 칠한다.
 * (첫 실기기 QA: 밝은 종이색 하나로는 배경에 묻혀 가시성이 나빴다.)
 */
private val PAPER_LABEL_STRIP_IDLE_FILTER: ColorFilter = run {
    val toneR = 217f / 204f
    val toneG = 213f / 204f
    val toneB = 207f / 204f
    fun lumaRow(tone: Float) = floatArrayOf(0.299f * tone, 0.587f * tone, 0.114f * tone, 0f, 0f)
    ColorFilter.colorMatrix(
        ColorMatrix(
            lumaRow(toneR) + lumaRow(toneG) + lumaRow(toneB) + floatArrayOf(0f, 0f, 0f, 1f, 0f)
        )
    )
}

// 선택줄이 탭을 오갈 때마다 다시 decode하지 않도록 한 번 줄여 불러온 띠를 붙들어 둔다.
private var paperLabelStripCache: ImageBitmap? = null

private fun loadPaperLabelStrip(resources: Resources): ImageBitmap =
    paperLabelStripCache ?: BitmapFactory.decodeResource(
        resources,
        R.drawable.gallery_date_paper_strip,
        BitmapFactory.Options().apply { inSampleSize = PAPER_LABEL_STRIP_SAMPLE }
    ).asImageBitmap().also { paperLabelStripCache = it }

// 선택된 라벨 위쪽 가장자리에 두 앞발을 걸치고 고개를 빠끔 내민 얼굴(editor_label_peek.png).
// 앞발이 띠 가장자리를 잡고 있어야 해서 띠 앞에 그리고, 아래 평평하게 잘린 앞발 끝을 띠
// 위쪽 가장자리보다 조금 아래에 맞춘다. 띠처럼 뒤집지 않는다 — 얼굴은 늘 똑바로 선다.
// 원본(1254×1254) 중 그림이 있는 영역(약 376,352부터 720×656)만 쓰고, 화면에서는 20dp
// 남짓이라 1/4로 줄여 불러온다(약 0.4MB). 위치·크기는 실기기 QA로 다듬는 미감 값이다.
private const val PAPER_LABEL_PEEK_SAMPLE = 4
private val PAPER_LABEL_PEEK_SRC_OFFSET = IntOffset(376 / PAPER_LABEL_PEEK_SAMPLE, 352 / PAPER_LABEL_PEEK_SAMPLE)
private val PAPER_LABEL_PEEK_SRC_SIZE = IntSize(720 / PAPER_LABEL_PEEK_SAMPLE, 656 / PAPER_LABEL_PEEK_SAMPLE)
private const val PAPER_LABEL_PEEK_HEIGHT_DP = 20f
private const val PAPER_LABEL_PEEK_PAW_OVERLAP_DP = 2.5f
private const val PAPER_LABEL_PEEK_CENTER_X_FRACTION = 0.68f

private var paperLabelPeekCache: ImageBitmap? = null

private fun loadPaperLabelPeek(resources: Resources): ImageBitmap =
    paperLabelPeekCache ?: BitmapFactory.decodeResource(
        resources,
        R.drawable.editor_label_peek,
        BitmapFactory.Options().apply { inSampleSize = PAPER_LABEL_PEEK_SAMPLE }
    ).asImageBitmap().also { paperLabelPeekCache = it }

/**
 * 칸([widthDp]×[heightDp]) 기준으로 빠끔 얼굴이 놓이는 영역(dp): 왼쪽, 위, 오른쪽, 아래. 아래
 * 변(앞발 끝)은 띠 위쪽 가장자리보다 조금 아래이고, 위는 칸 위로 조금 넘어갈 수 있다(선택줄
 * 위 여백 안).
 */
internal fun paperLabelPeekBounds(widthDp: Float, heightDp: Float): FloatArray {
    val (labelLeft, labelTop, labelRight, _) = paperLabelBounds(widthDp, heightDp).toList()
    val height = PAPER_LABEL_PEEK_HEIGHT_DP
    val width = height * PAPER_LABEL_PEEK_SRC_SIZE.width / PAPER_LABEL_PEEK_SRC_SIZE.height
    val centerX = labelLeft + (labelRight - labelLeft) * PAPER_LABEL_PEEK_CENTER_X_FRACTION
    val bottom = labelTop + PAPER_LABEL_PEEK_PAW_OVERLAP_DP
    return floatArrayOf(centerX - width / 2f, bottom - height, centerX + width / 2f, bottom)
}

/** 칸([widthDp]×[heightDp]) 안에서 종이 띠가 놓이는 영역(dp): 왼쪽, 위, 오른쪽, 아래. */
internal fun paperLabelBounds(widthDp: Float, heightDp: Float): FloatArray {
    val height = minOf(PAPER_LABEL_HEIGHT_DP, heightDp)
    val top = (heightDp - height) / 2f
    return floatArrayOf(PAPER_LABEL_INSET_X_DP, top, widthDp - PAPER_LABEL_INSET_X_DP, top + height)
}

/**
 * 칸 뒤에 종이 띠를 선택 여부에 맞는 색으로 그린다. 세 칸이 같은 띠를 복사한 것처럼
 * 보이지 않게 [index]에 따라 띠를 그대로·좌우 뒤집기·상하좌우 뒤집기로 번갈아 깐다 —
 * 회전 없이 뒤집기만 써서 수평과 가장자리 선명도는 그대로다. 선택된 칸에는 띠 위쪽
 * 가장자리에 [peek] 얼굴을 걸쳐 그린다.
 */
private fun Modifier.paperLabel(
    strip: ImageBitmap,
    peek: ImageBitmap,
    index: Int,
    selected: Boolean
): Modifier =
    drawWithCache {
        val widthDp = size.width / density
        val heightDp = size.height / density
        val (left, top, right, bottom) = paperLabelBounds(widthDp, heightDp).map { it * density }
        val dstOffset = IntOffset(left.roundToInt(), top.roundToInt())
        val dstSize = IntSize((right - left).roundToInt(), (bottom - top).roundToInt())
        val labelCenter = Offset((left + right) / 2f, (top + bottom) / 2f)
        val flipX = index % 3 != 0
        val flipY = index % 3 == 2
        val (peekLeft, peekTop, peekRight, peekBottom) =
            paperLabelPeekBounds(widthDp, heightDp).map { it * density }
        val peekOffset = IntOffset(peekLeft.roundToInt(), peekTop.roundToInt())
        val peekSize = IntSize((peekRight - peekLeft).roundToInt(), (peekBottom - peekTop).roundToInt())
        onDrawBehind {
            if (dstSize.width > 0 && dstSize.height > 0) {
                scale(
                    scaleX = if (flipX) -1f else 1f,
                    scaleY = if (flipY) -1f else 1f,
                    pivot = labelCenter
                ) {
                    drawImage(
                        image = strip,
                        srcOffset = PAPER_LABEL_STRIP_SRC_OFFSET,
                        srcSize = PAPER_LABEL_STRIP_SRC_SIZE,
                        dstOffset = dstOffset,
                        dstSize = dstSize,
                        colorFilter = if (selected) GALLERY_DATE_STRIP_CORAL_FILTER else PAPER_LABEL_STRIP_IDLE_FILTER,
                        filterQuality = FilterQuality.High
                    )
                }
                if (selected) {
                    drawImage(
                        image = peek,
                        srcOffset = PAPER_LABEL_PEEK_SRC_OFFSET,
                        srcSize = PAPER_LABEL_PEEK_SRC_SIZE,
                        dstOffset = peekOffset,
                        dstSize = peekSize,
                        filterQuality = FilterQuality.High
                    )
                }
            }
        }
    }

/**
 * [EditorSubcategoryNavBar]의 종이 라벨 표현. 칸 배치(전체 92%, 칸마다 같은 폭, 최소 44dp
 * 높이, 위아래 10dp 여백)와 탭 동작은 평평한 표현과 같고 그림만 다르다. 라벨 사이가 칸
 * 안쪽 여백으로 이미 떨어져 있어 세로 구분선은 두지 않는다. 사각형 물결(ripple)은 칸
 * 모양을 드러내 상자처럼 보이게 하므로 끈다 — 탭하면 띠 색과 굵은 글자가 옮겨 가는 것으로 반응한다.
 */
@Composable
internal fun PaperLabelSubcategoryNavRow(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val resources = LocalContext.current.resources
    val strip = remember(resources) { loadPaperLabelStrip(resources) }
    val peek = remember(resources) { loadPaperLabelPeek(resources) }

    Row(
        modifier = modifier.fillMaxWidth(0.92f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex

            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .paperLabel(strip = strip, peek = peek, index = index, selected = selected)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled
                    ) {
                        onOptionSelected(index)
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    color = InkPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
