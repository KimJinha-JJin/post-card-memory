package com.postcardmemory.ui.detail

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.SunsetGold

// 102일차: 상위 기능 버튼의 그림은 잡지에서 오려낸 인쇄 삽화(PNG)다. 원본(1254×1254)은
// 화면에서 20dp 남짓이라 1/4로 줄여 불러오고(장당 약 0.4MB), 투명 여백을 걷어낸 그림 영역만
// 같은 상자([EDITOR_CUTOUT_BOX_WIDTH_DP]×[EDITOR_CUTOUT_BOX_HEIGHT_DP]) 안에 비율대로 맞춘다 —
// 캔버스가 같아도 그림마다 여백이 달라 그대로 두면 크기가 들쭉날쭉해 보이기 때문이다.
// 상자 크기는 실기기 QA로 다듬는 미감 값이다.
private const val EDITOR_CUTOUT_SAMPLE = 4
private const val EDITOR_CUTOUT_ALPHA_THRESHOLD = 16
private const val EDITOR_CUTOUT_BOX_WIDTH_DP = 32f
private const val EDITOR_CUTOUT_BOX_HEIGHT_DP = 24f

private class EditorCutout(
    val image: ImageBitmap,
    val srcOffset: IntOffset,
    val srcSize: IntSize
)

// 편집 화면을 오갈 때마다 다시 decode하지 않도록 줄여 불러온 그림과 그림 영역을 붙들어 둔다.
private val editorCutoutCache = HashMap<Int, EditorCutout>()

private fun loadEditorCutout(resources: Resources, @DrawableRes resId: Int): EditorCutout =
    editorCutoutCache.getOrPut(resId) {
        val bitmap = BitmapFactory.decodeResource(
            resources,
            resId,
            BitmapFactory.Options().apply { inSampleSize = EDITOR_CUTOUT_SAMPLE }
        )
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1
        for (y in 0 until height) {
            for (x in 0 until width) {
                if ((pixels[y * width + x] ushr 24) > EDITOR_CUTOUT_ALPHA_THRESHOLD) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < 0) {
            EditorCutout(bitmap.asImageBitmap(), IntOffset.Zero, IntSize(width, height))
        } else {
            EditorCutout(
                bitmap.asImageBitmap(),
                IntOffset(minX, minY),
                IntSize(maxX - minX + 1, maxY - minY + 1)
            )
        }
    }

/** 그림 영역([srcWidth]×[srcHeight])을 공통 상자 안에 비율대로 맞춘 표시 크기(dp): 너비, 높이. */
internal fun editorCutoutDisplaySizeDp(srcWidth: Int, srcHeight: Int): FloatArray {
    if (srcWidth <= 0 || srcHeight <= 0) {
        return floatArrayOf(EDITOR_CUTOUT_BOX_WIDTH_DP, EDITOR_CUTOUT_BOX_HEIGHT_DP)
    }
    val scale = minOf(
        EDITOR_CUTOUT_BOX_WIDTH_DP / srcWidth,
        EDITOR_CUTOUT_BOX_HEIGHT_DP / srcHeight
    )
    return floatArrayOf(srcWidth * scale, srcHeight * scale)
}

/**
 * 상세 편집 화면 하단에 고정되는 편집 카테고리 전환용 도크. 스크롤 콘텐츠
 * 밖 루트 Box에 얹혀 편집 내용을 위아래로 움직여도 위치가 바뀌지 않는다.
 * Pager 상태·선택 표현·탭 이동 콜백은 기존 인라인 탭 바와 동일하게 유지한다.
 *
 * 카테고리 목록 자체는 이 컴포저블이 갖지 않고 labels/icons로 받는다 —
 * 실제 목록과 개수는 호출부(DetailScreen의 customizationPageLabels)가
 * 정하므로 여기에 개수를 적어두지 않는다.
 *
 * 102일차: icons는 Material 아이콘 대신 잡지 오림 삽화의 drawable id다. 그림은
 * 장식이라 설명을 비우고, 칸 전체가 "<기능명> 편집"을 알린다. 그림 크기와 칸(터치
 * 영역)은 서로 묶이지 않는다 — 칸은 기존처럼 줄 너비를 똑같이 나눈 전체다.
 */
@Composable
internal fun EditorBottomTabBar(
    selectedPage: Int,
    labels: List<String>,
    icons: List<Int>,
    enabled: Boolean,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val resources = LocalContext.current.resources
    val cutoutPainters = remember(resources, icons) {
        icons.map { resId ->
            val cutout = loadEditorCutout(resources, resId)
            BitmapPainter(
                image = cutout.image,
                srcOffset = cutout.srcOffset,
                srcSize = cutout.srcSize,
                filterQuality = FilterQuality.High
            ) to editorCutoutDisplaySizeDp(cutout.srcSize.width, cutout.srcSize.height)
        }
    }

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
                val (cutoutPainter, cutoutSizeDp) = cutoutPainters[pageIndex]
                Box(
                    modifier = Modifier.size(
                        EDITOR_CUTOUT_BOX_WIDTH_DP.dp,
                        EDITOR_CUTOUT_BOX_HEIGHT_DP.dp
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = cutoutPainter,
                        contentDescription = null,
                        modifier = Modifier.size(cutoutSizeDp[0].dp, cutoutSizeDp[1].dp)
                    )
                }

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
