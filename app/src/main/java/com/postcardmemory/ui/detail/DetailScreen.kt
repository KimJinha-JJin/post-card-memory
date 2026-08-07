package com.postcardmemory.ui.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.postcardmemory.ui.components.EditorSlider
import com.postcardmemory.ui.components.PhotoSourceMenu
import com.postcardmemory.ui.components.PostcardBackgroundColorPicker
import com.postcardmemory.ui.components.PostcardBackgroundPattern
import com.postcardmemory.ui.components.PostcardBackgroundPatternPicker
import com.postcardmemory.ui.components.PostcardCustomColorPicker
import com.postcardmemory.ui.components.PostcardDateFormat
import com.postcardmemory.ui.components.PostcardLayoutPicker
import com.postcardmemory.ui.components.PostcardLayoutStyle
import com.postcardmemory.ui.components.PostcardTextFont
import com.postcardmemory.ui.components.SealPreviewContent
import com.postcardmemory.data.FUTURE_MAIL_STATE_SENT
import com.postcardmemory.ui.futuremail.daysUntilFutureMail
import com.postcardmemory.ui.futuremail.isFutureMailArrived
import com.postcardmemory.ui.futuremail.isSelectableFutureMailDate
import com.postcardmemory.ui.futuremail.localStartOfDayToMaterialDatePickerUtcMillis
import com.postcardmemory.ui.futuremail.materialDatePickerUtcMillisToLocalStartOfDay
import com.postcardmemory.ui.futuremail.startOfDayMillis
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.SunsetGold
import com.postcardmemory.ui.theme.NeutralLight
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.GalleryPaperWhite
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperField
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.ui.theme.PaperTray
import com.postcardmemory.ui.theme.ScreenBackgroundGray
import com.postcardmemory.ui.theme.SealInkBlack
import com.postcardmemory.ui.theme.SurfaceGray
import com.postcardmemory.ui.theme.sealInkColors
import com.postcardmemory.utils.DoodlePoint
import com.postcardmemory.utils.DoodleStroke
import com.postcardmemory.utils.DoodleStrokeWidth
import com.postcardmemory.utils.DoodleTool
import com.postcardmemory.utils.PostcardImageExporter
import com.postcardmemory.utils.PostcardRenderSpec
import com.postcardmemory.utils.renderWidth
import com.postcardmemory.utils.sanitizedDoodlePoint

internal enum class StickerEditMode {
    Move,
    Scale,
    Rotate
}

private enum class TextScaleTarget {
    Message,
    Date
}

internal fun clampStickerOffset(
    offset: Offset,
    postcardSize: IntSize,
    stickerSize: IntSize
): Offset {
    val maxX =
        (postcardSize.width - stickerSize.width)
            .coerceAtLeast(0)
            .toFloat()
    val maxY =
        (postcardSize.height - stickerSize.height)
            .coerceAtLeast(0)
            .toFloat()

    return Offset(
        x = offset.x.coerceIn(
            minimumValue = 0f,
            maximumValue = maxX
        ),
        y = offset.y.coerceIn(
            minimumValue = 0f,
            maximumValue = maxY
        )
    )
}

internal fun centeredStickerOffset(
    postcardSize: IntSize,
    stickerSize: IntSize
): Offset =
    Offset(
        x = ((postcardSize.width - stickerSize.width) / 2f)
            .coerceAtLeast(0f),
        y = ((postcardSize.height - stickerSize.height) / 2f)
            .coerceAtLeast(0f)
    )

/**
 * stickerSizes[id]/sealSizes[id] 측정값이 아직 없을 때(예: 방금 추가해 첫
 * 컴포지션이 안 끝난 경우) 쓰는 fallback 크기. 스티커·도장 모두 실제 렌더
 * 크기가 `기준크기(dp) * item.scale`로 정확히 결정되므로(DetailScreen의
 * `.size(STICKER_BASE_SIZE * sticker.scale)`/`.size(SEAL_BASE_SIZE * seal.scale)`
 * 참고) 이 공식이 근사가 아니라 실제 측정값과 사실상 동일하다.
 */
internal fun computeFallbackOverlaySize(
    basePx: Float,
    scale: Float
): IntSize {
    val side =
        (basePx * scale)
            .roundToInt()
            .coerceAtLeast(1)
    return IntSize(side, side)
}

private fun localStickerDeltaToParent(
    localDelta: Offset,
    rotationDegrees: Float,
    flipHorizontal: Boolean,
    flipVertical: Boolean
): Offset {
    val flippedX =
        if (flipHorizontal) -localDelta.x else localDelta.x
    val flippedY =
        if (flipVertical) -localDelta.y else localDelta.y

    if (rotationDegrees == 0f) {
        return Offset(flippedX, flippedY)
    }

    val radians =
        Math.toRadians(rotationDegrees.toDouble())
    val cos = kotlin.math.cos(radians).toFloat()
    val sin = kotlin.math.sin(radians).toFloat()

    return Offset(
        x = flippedX * cos - flippedY * sin,
        y = flippedX * sin + flippedY * cos
    )
}

/** 도장이 화면 밖으로 완전히 사라지지 않도록 남겨두는 가로·세로 최소 가시 길이. */
private val SEAL_MIN_VISIBLE_EDGE = 24.dp
private const val SEAL_MIN_VISIBLE_FRACTION = 0.30f

/**
 * 스티커·도장의 기준(scale=1) 렌더 크기. 실제 렌더 크기는 항상
 * `기준 크기 * item.scale`이라 stickerSizes/sealSizes 측정값이 아직 없을 때도
 * (예: 방금 추가해 첫 컴포지션이 안 끝난 경우) 이 상수와 scale만으로 미리보기와
 * 동일한 크기를 다시 계산할 수 있다 — Export가 이 값으로 fallback한다.
 */
private val STICKER_BASE_SIZE = 120.dp
private val SEAL_BASE_SIZE = 90.dp

/**
 * 회전·확대가 반영된 도장의 최종 시각 경계(AABB)를 기준으로, 도장이 최소 가시 영역
 * 밑으로 사라지는 경우에만 가장 가까운 안전 위치로 offset(회전 전 좌상단 좌표)을
 * 보정한다. 이미 안전하면 원래 offset을 그대로 반환한다.
 *
 * Compose 상태나 UI 객체에 의존하지 않는 순수 함수라 미리보기 드래그 종료
 * 시점과 Export 오버레이 생성(createSealOverlayForExport) 양쪽에서 그대로
 * 재사용한다 — 화면에서 허용된 가장자리 걸침이 Export에서도 유지되게 하는
 * 핵심 함수다. internal은 순수 JUnit 테스트를 위함(같은 패키지에서 호출).
 */
internal fun correctSealOffsetForMinimumVisibility(
    offset: Offset,
    sealSize: IntSize,
    rotationDegrees: Float,
    postcardSize: IntSize,
    minimumVisibleEdgePx: Float
): Offset {
    if (
        sealSize == IntSize.Zero ||
        postcardSize == IntSize.Zero
    ) {
        return offset
    }

    val radians =
        Math.toRadians(rotationDegrees.toDouble())
    val absCos =
        kotlin.math.abs(kotlin.math.cos(radians)).toFloat()
    val absSin =
        kotlin.math.abs(kotlin.math.sin(radians)).toFloat()

    val rotatedWidth =
        sealSize.width * absCos + sealSize.height * absSin
    val rotatedHeight =
        sealSize.width * absSin + sealSize.height * absCos

    val minVisibleWidth =
        minOf(
            rotatedWidth,
            maxOf(rotatedWidth * SEAL_MIN_VISIBLE_FRACTION, minimumVisibleEdgePx)
        )
    val minVisibleHeight =
        minOf(
            rotatedHeight,
            maxOf(rotatedHeight * SEAL_MIN_VISIBLE_FRACTION, minimumVisibleEdgePx)
        )

    val centerX = offset.x + sealSize.width / 2f
    val centerY = offset.y + sealSize.height / 2f

    val aabbLeft = centerX - rotatedWidth / 2f
    val aabbTop = centerY - rotatedHeight / 2f

    val minAabbLeft = minVisibleWidth - rotatedWidth
    val maxAabbLeft = postcardSize.width - minVisibleWidth
    val correctedAabbLeft =
        if (minAabbLeft <= maxAabbLeft) {
            aabbLeft.coerceIn(minAabbLeft, maxAabbLeft)
        } else {
            (postcardSize.width - rotatedWidth) / 2f
        }

    val minAabbTop = minVisibleHeight - rotatedHeight
    val maxAabbTop = postcardSize.height - minVisibleHeight
    val correctedAabbTop =
        if (minAabbTop <= maxAabbTop) {
            aabbTop.coerceIn(minAabbTop, maxAabbTop)
        } else {
            (postcardSize.height - rotatedHeight) / 2f
        }

    val correctedCenterX = correctedAabbLeft + rotatedWidth / 2f
    val correctedCenterY = correctedAabbTop + rotatedHeight / 2f

    return Offset(
        x = correctedCenterX - sealSize.width / 2f,
        y = correctedCenterY - sealSize.height / 2f
    )
}

/** 렌더 크기와 무관하게 도장 선택 판정에 확보하는 최소 터치 영역. 미리보기/저장 이미지 모양에는 영향 없음. */
private val SEAL_MIN_HIT_TARGET_SIZE = 56.dp

/** 선택된 도장에 한해 확보하는 최소 멀티터치(핀치·회전) 영역. 작은 도장도 두 손가락을 올릴 여유를 준다. */
private val SEAL_SELECTED_MIN_GESTURE_SIZE = 120.dp

/** 엽서 한 장에 추가할 수 있는 도장 총 개수(종류 무관 합산). */
const val MAX_SEAL_COUNT = 2

/**
 * 낙서 미리보기 화면 좌표(postcardPreviewSize 기준 px)를 엽서 정규화 좌표로
 * 변환한다. 경계를 살짝 벗어난 좌표는 sanitizedDoodlePoint가 [0,1]로
 * 고정해 그리는 도중 엽서 밖으로 나가도 안전하게 경계선을 따라 이어진다.
 * postcardSize가 아직 측정되지 않았으면(0 이하) null을 반환한다.
 */
internal fun normalizedDoodlePoint(
    offset: Offset,
    postcardSize: IntSize
): DoodlePoint? {
    if (postcardSize.width <= 0 || postcardSize.height <= 0) {
        return null
    }

    return sanitizedDoodlePoint(
        x = offset.x / postcardSize.width.toFloat(),
        y = offset.y / postcardSize.height.toFloat()
    )
}

/** 낙서 직선이 화면상 수평·수직 중 어디에 붙어 있는지, 혹은 자유 각도인지. */
internal enum class DoodleLineSnapDirection {
    NONE,
    HORIZONTAL,
    VERTICAL
}

/** 진입 각도가 이 값(도) 이내면 수평·수직으로 붙는다. */
private const val DOODLE_SNAP_ENTRY_DEGREES = 12f

/** 이미 붙은 상태에서는 이 값(도)을 벗어나야 풀린다 — 경계에서 떨리지 않도록 진입보다 넓게 잡는다. */
private const val DOODLE_SNAP_EXIT_DEGREES = 18f

/**
 * 화면 픽셀 기준 dx/dy(정규화 좌표가 아닌 raw 좌표)로 수평·수직 정렬
 * 상태를 판정한다. 정규화 좌표는 가로/세로를 각각 postcardSize.width,
 * height로 나눠서 만들어지므로, 만약 미리보기가 정사각형이 아니게 되면
 * 그 각도가 화면에 실제로 보이는 각도와 달라진다 — raw 좌표로 판정해
 * 그런 경우에도 시각적으로 정확한 수평·수직을 보장한다. 진입·해제
 * 임계값을 분리해 경계에서 반복적으로 붙었다 풀리지 않게 하고, 현재
 * 상태를 벗어났을 때는 곧바로 반대 방향 진입 여부를 다시 평가해
 * 수평→수직 전환이 자연스럽게 이어지게 한다.
 */
internal fun resolveDoodleLineSnapDirection(
    current: DoodleLineSnapDirection,
    dx: Float,
    dy: Float,
    minDistancePx: Float
): DoodleLineSnapDirection {
    val distanceSq = dx * dx + dy * dy
    if (distanceSq < minDistancePx * minDistancePx) {
        // 아주 짧은 거리에서는 각도가 불안정하므로 판정을 보류하고 직전 상태를 유지한다.
        return current
    }

    val angleDeg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
    val angleMod180 = ((angleDeg % 180f) + 180f) % 180f
    val distanceToHorizontal = min(angleMod180, 180f - angleMod180)
    val distanceToVertical = abs(angleMod180 - 90f)

    val staysHorizontal =
        current == DoodleLineSnapDirection.HORIZONTAL &&
                distanceToHorizontal <= DOODLE_SNAP_EXIT_DEGREES
    val staysVertical =
        current == DoodleLineSnapDirection.VERTICAL &&
                distanceToVertical <= DOODLE_SNAP_EXIT_DEGREES

    return when {
        staysHorizontal -> DoodleLineSnapDirection.HORIZONTAL
        staysVertical -> DoodleLineSnapDirection.VERTICAL
        distanceToHorizontal <= DOODLE_SNAP_ENTRY_DEGREES -> DoodleLineSnapDirection.HORIZONTAL
        distanceToVertical <= DOODLE_SNAP_ENTRY_DEGREES -> DoodleLineSnapDirection.VERTICAL
        else -> DoodleLineSnapDirection.NONE
    }
}

/**
 * [direction]에 따라 끝점을 보정한다. 시작점·현재 손가락 위치와 같은 좌표
 * 공간(화면 raw px)을 그대로 받아 같은 공간의 보정된 끝점을 돌려주므로,
 * 호출부에서 정규화하기 전에 적용해야 시각적으로 정확한 수평·수직이 된다.
 */
internal fun snappedDoodleLineEndpoint(
    direction: DoodleLineSnapDirection,
    start: Offset,
    current: Offset
): Offset = when (direction) {
    DoodleLineSnapDirection.HORIZONTAL -> Offset(current.x, start.y)
    DoodleLineSnapDirection.VERTICAL -> Offset(start.x, current.y)
    DoodleLineSnapDirection.NONE -> current
}

/** 스티커 탭의 페이지 인덱스. 다른 탭에서는 스티커 선택 표시·조작 손잡이·제스처를 시작하지 않는다. */
internal const val STICKER_TAB_PAGE_INDEX = 3

/** 도장 탭의 페이지 인덱스. 다른 탭에서는 도장 선택 표시·제스처를 시작하지 않는다. */
internal const val SEAL_TAB_PAGE_INDEX = 4

/** 낙서 탭의 페이지 인덱스. 다른 탭에서는 낙서 입력·지우개 판정을 시작하지 않는다. */
internal const val DOODLE_TAB_PAGE_INDEX = 5

/** 이 거리(정규화 좌표 기준 화면 px)보다 가까운 점은 새로 추가하지 않아 획 데이터가 과도하게 촘촘해지지 않게 한다. */
private const val MIN_DOODLE_POINT_SPACING_PX = 3f

private fun distanceFromPointToSegment(
    px: Float,
    py: Float,
    ax: Float,
    ay: Float,
    bx: Float,
    by: Float
): Float {
    val abx = bx - ax
    val aby = by - ay
    val lengthSquared = abx * abx + aby * aby

    if (lengthSquared <= 0f) {
        val dx = px - ax
        val dy = py - ay
        return sqrt(dx * dx + dy * dy)
    }

    val t = (((px - ax) * abx) + ((py - ay) * aby)) / lengthSquared
    val clampedT = t.coerceIn(0f, 1f)
    val closestX = ax + clampedT * abx
    val closestY = ay + clampedT * aby
    val dx = px - closestX
    val dy = py - closestY

    return sqrt(dx * dx + dy * dy)
}

/**
 * 지우개가 이 획에 닿았는지 판정한다. 판정 반경은 지우개로 선택한 굵기와
 * 지워질 획 자체의 굵기를 함께 고려한다(굵은 획일수록, 굵은 지우개일수록
 * 쉽게 지워짐). 정규화 좌표(0..1) 공간에서 계산하므로 미리보기·exporter의
 * 화면 크기 차이와 무관하게 항상 같은 획이 지워진다.
 *
 * 획 쪽 반경은 저장된 굵기가 아니라 renderWidth(도구 배율 반영)를 쓴다 —
 * 형광펜처럼 화면에 더 넓게 그려지는 도구도 보이는 만큼 지워져야 한다.
 */
internal fun doodleEraserHitsStroke(
    touchPoint: DoodlePoint,
    stroke: DoodleStroke,
    eraserWidth: DoodleStrokeWidth
): Boolean {
    if (stroke.points.isEmpty()) return false

    val hitRadius =
        (eraserWidth.logicalWidth / 2f + stroke.renderWidth / 2f) /
                PostcardRenderSpec.LOGICAL_SIZE

    if (stroke.points.size == 1) {
        val point = stroke.points[0]
        val dx = touchPoint.x - point.x
        val dy = touchPoint.y - point.y
        return sqrt(dx * dx + dy * dy) <= hitRadius
    }

    for (index in 0 until stroke.points.size - 1) {
        val a = stroke.points[index]
        val b = stroke.points[index + 1]
        val distance =
            distanceFromPointToSegment(
                px = touchPoint.x,
                py = touchPoint.y,
                ax = a.x,
                ay = a.y,
                bx = b.x,
                by = b.y
            )
        if (distance <= hitRadius) return true
    }

    return false
}

internal fun createStickerOverlayForExport(
    stickerUri: Uri?,
    originalStickerUri: Uri?,
    isBackgroundRemoved: Boolean,
    rotationDegrees: Float,
    flipHorizontal: Boolean,
    flipVertical: Boolean,
    stickerOffset: Offset?,
    postcardSize: IntSize,
    stickerSize: IntSize
): PostcardImageExporter.StickerOverlay? {
    val selectedUri =
        stickerUri
            ?: return null

    if (
        postcardSize.width <= 0 ||
        postcardSize.height <= 0 ||
        stickerSize.width <= 0 ||
        stickerSize.height <= 0
    ) {
        return null
    }

    // 일반 사진 스티커는 도장과 달리 가장자리 걸침을 허용하지 않는다 —
    // 항상 엽서 내부에 완전히 들어오도록 clampStickerOffset을 그대로 쓴다.
    val resolvedOffset =
        clampStickerOffset(
            offset =
                stickerOffset
                    ?: centeredStickerOffset(
                        postcardSize = postcardSize,
                        stickerSize = stickerSize
                    ),
            postcardSize = postcardSize,
            stickerSize = stickerSize
        )
    return PostcardImageExporter.StickerOverlay(
        uri = selectedUri,
        originalUri = originalStickerUri,
        isBackgroundRemoved = isBackgroundRemoved,
        rotationDegrees = rotationDegrees,
        flipHorizontal = flipHorizontal,
        flipVertical = flipVertical,
        normalizedX =
            (resolvedOffset.x /
                    postcardSize.width.toFloat())
                .coerceIn(0f, 1f),
        normalizedY =
            (resolvedOffset.y /
                    postcardSize.height.toFloat())
                .coerceIn(0f, 1f),
        sizeRatio =
            stickerSize.width.toFloat() /
                    postcardSize.width.toFloat()
    )
}

/**
 * stickerSizes[id] 측정값이 아직 없어도(방금 추가한 스티커 등) 조용히 건너뛰지
 * 않고 computeFallbackOverlaySize로 계산한 크기를 사용한다 — 미리보기와 같은
 * 공식(STICKER_BASE_SIZE * sticker.scale)이라 실질적으로 동일한 결과다.
 */
internal fun createStickerOverlaysForExport(
    photoStickers: List<PhotoStickerItem>,
    postcardSize: IntSize,
    stickerSizes: Map<String, IntSize>,
    baseStickerPx: Float
): List<PostcardImageExporter.StickerOverlay> {
    if (
        postcardSize.width <= 0 ||
        postcardSize.height <= 0
    ) {
        return emptyList()
    }

    return photoStickers.mapNotNull { sticker ->
        val stickerSize =
            stickerSizes[sticker.id]
                ?: computeFallbackOverlaySize(
                    basePx = baseStickerPx,
                    scale = sticker.scale
                )

        createStickerOverlayForExport(
            stickerUri = sticker.displayedUri,
            originalStickerUri = sticker.originalUri,
            isBackgroundRemoved = sticker.isBackgroundRemoved,
            rotationDegrees = sticker.rotationDegrees,
            flipHorizontal = sticker.flipHorizontal,
            flipVertical = sticker.flipVertical,
            stickerOffset = sticker.offset,
            postcardSize = postcardSize,
            stickerSize = stickerSize
        )
    }
}

internal fun createSealOverlayForExport(
    type: SealType,
    colorArgb: Long,
    rotationDegrees: Float,
    sealOffset: Offset?,
    postcardSize: IntSize,
    sealSize: IntSize,
    minimumVisibleEdgePx: Float,
    capturedAtMillis: Long?
): PostcardImageExporter.SealOverlay? {
    if (
        postcardSize.width <= 0 ||
        postcardSize.height <= 0 ||
        sealSize.width <= 0 ||
        sealSize.height <= 0
    ) {
        return null
    }

    // 도장은 미리보기 드래그 종료 시점과 같은 최소 가시 영역 정책을 쓴다 —
    // 화면에서 허용된 가장자리 걸침을 Export가 안쪽으로 다시 밀어넣지 않도록,
    // 스티커용 clampStickerOffset이 아니라 도장 전용 함수를 그대로 재사용한다.
    val resolvedOffset =
        correctSealOffsetForMinimumVisibility(
            offset =
                sealOffset
                    ?: centeredStickerOffset(
                        postcardSize = postcardSize,
                        stickerSize = sealSize
                    ),
            sealSize = sealSize,
            rotationDegrees = rotationDegrees,
            postcardSize = postcardSize,
            minimumVisibleEdgePx = minimumVisibleEdgePx
        )

    return PostcardImageExporter.SealOverlay(
        type = type.name,
        // 가장자리 걸침을 표현해야 하므로 [0,1]로 재클램프하지 않는다 —
        // correctSealOffsetForMinimumVisibility가 이미 안전한 범위로 보정했다.
        normalizedX =
            resolvedOffset.x / postcardSize.width.toFloat(),
        normalizedY =
            resolvedOffset.y / postcardSize.height.toFloat(),
        sizeRatio =
            sealSize.width.toFloat() /
                    postcardSize.width.toFloat(),
        rotationDegrees = rotationDegrees,
        colorArgb = colorArgb,
        capturedAtMillis = capturedAtMillis
    )
}

/**
 * sealSizes[id] 측정값이 아직 없어도(방금 추가한 도장 등) 조용히 건너뛰지
 * 않고 computeFallbackOverlaySize로 계산한 크기를 사용한다 — 미리보기와 같은
 * 공식(SEAL_BASE_SIZE * seal.scale)이라 실질적으로 동일한 결과다.
 */
internal fun createSealOverlaysForExport(
    photoSeals: List<PostcardSealItem>,
    postcardSize: IntSize,
    sealSizes: Map<String, IntSize>,
    baseSealPx: Float,
    minimumVisibleEdgePx: Float,
    capturedAtMillis: Long?
): List<PostcardImageExporter.SealOverlay> {
    if (
        postcardSize.width <= 0 ||
        postcardSize.height <= 0
    ) {
        return emptyList()
    }

    return photoSeals.mapNotNull { seal ->
        val sealSize =
            sealSizes[seal.id]
                ?: computeFallbackOverlaySize(
                    basePx = baseSealPx,
                    scale = seal.scale
                )

        createSealOverlayForExport(
            type = seal.type,
            colorArgb = seal.colorArgb,
            minimumVisibleEdgePx = minimumVisibleEdgePx,
            rotationDegrees = seal.rotationDegrees,
            sealOffset = seal.offset,
            postcardSize = postcardSize,
            sealSize = sealSize,
            capturedAtMillis = capturedAtMillis
        )
    }
}

/**
 * 세 패널의 보조 행동(사진 바꾸기·색 가져오기·직접 고르기·문구 편집)에
 * 쓰는 가벼운 외곽선 버튼. 주요 선택지와 달리 검은 풀폭 버튼으로 강조하지
 * 않고, 아이콘+라벨만 얇은 테두리로 보여 준다.
 */
@Composable
private fun EditorSecondaryButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SurfaceGray),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = BrutalBlack
        ),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )

        Spacer(
            modifier = Modifier.size(6.dp)
        )

        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun isPositionInsideTapedFilmPhotoBounds(
    position: Offset,
    previewSize: IntSize,
    stampPhotoScale: Float
): Boolean {
    if (previewSize.width <= 0) {
        return false
    }

    val scale =
        previewSize.width / PostcardRenderSpec.LOGICAL_SIZE
    val photoBounds =
        PostcardRenderSpec.layoutFor(
            layoutStyle = "TAPED_FILM",
            stampPhotoScale = stampPhotoScale
        ).photoBounds

    return position.x >= photoBounds.left * scale &&
            position.x <= photoBounds.right * scale &&
            position.y >= photoBounds.top * scale &&
            position.y <= photoBounds.bottom * scale
}

@Composable
private fun PostcardPreviewContent(
    imagePath: String,
    message: String,
    backgroundColorArgb: Long,
    backgroundPattern: String,
    messageFont: String,
    layoutStyle: String,
    capturedAt: Long,
    dateFormat: String,
    messageTextScale: Float = 1f,
    dateTextScale: Float = 1f,
    backgroundPatternDensity: Float = 1f,
    stampPhotoScale: Float = 1f,
    polaroidPhotoScale: Float = 1f,
    photoEdgeBlur: Float = 0f,
    stampPhotoOffsetX: Float = 0f,
    stampPhotoOffsetY: Float = 0f,
    polaroidPhotoOffsetX: Float = 0f,
    polaroidPhotoOffsetY: Float = 0f,
    tapedFilmPhotoOffsetX: Float = 0f,
    tapedFilmPhotoOffsetY: Float = 0f,
    stampPhotoZoom: Float = 1f,
    polaroidPhotoZoom: Float = 1f,
    tapedFilmPhotoZoom: Float = 1f
) {
    val sourceBitmap =
        remember(imagePath) {
            runCatching {
                PostcardRenderSpec.decodeSourceBitmap(
                    File(imagePath)
                )
            }.getOrNull()
        }

    DisposableEffect(sourceBitmap) {
        onDispose {
            if (
                sourceBitmap != null &&
                !sourceBitmap.isRecycled
            ) {
                sourceBitmap.recycle()
            }
        }
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val bitmap =
            sourceBitmap ?: return@Canvas

        drawIntoCanvas { composeCanvas ->
            PostcardRenderSpec.drawBaseContent(
                canvas = composeCanvas.nativeCanvas,
                sourceBitmap = bitmap,
                backgroundColorArgb = backgroundColorArgb,
                backgroundPattern = backgroundPattern,
                message = message,
                messageFont = messageFont,
                layoutStyle = layoutStyle,
                capturedAt = capturedAt,
                dateFormat = dateFormat,
                targetSize = size.width,
                messageTextScale = messageTextScale,
                dateTextScale = dateTextScale,
                backgroundPatternDensity = backgroundPatternDensity,
                stampPhotoScale = stampPhotoScale,
                polaroidPhotoScale = polaroidPhotoScale,
                photoEdgeBlur = photoEdgeBlur,
                stampPhotoOffsetX = stampPhotoOffsetX,
                stampPhotoOffsetY = stampPhotoOffsetY,
                polaroidPhotoOffsetX = polaroidPhotoOffsetX,
                polaroidPhotoOffsetY = polaroidPhotoOffsetY,
                tapedFilmPhotoOffsetX = tapedFilmPhotoOffsetX,
                tapedFilmPhotoOffsetY = tapedFilmPhotoOffsetY,
                stampPhotoZoom = stampPhotoZoom,
                polaroidPhotoZoom = polaroidPhotoZoom,
                tapedFilmPhotoZoom = tapedFilmPhotoZoom
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    postcardId: Long,
    onNavigateBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val postcard by viewModel.postcard.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    val futureMailSendState by viewModel.futureMailSendState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val shareState by viewModel.shareState.collectAsState()
    val draftSaveStatus by viewModel.draftSaveStatus.collectAsState()
    val confirmSaveState by viewModel.confirmSaveState.collectAsState()
    val backgroundUpdateState by viewModel.backgroundUpdateState.collectAsState()
    val imageUpdateState by viewModel.imageUpdateState.collectAsState()
    val fontUpdateState by viewModel.fontUpdateState.collectAsState()
    val layoutUpdateState by viewModel.layoutUpdateState.collectAsState()
    val dateFormatUpdateState by viewModel.dateFormatUpdateState.collectAsState()
    val photoColorExtractionState by viewModel.photoColorExtractionState.collectAsState()
    val stickerBackgroundRemovalState by
        viewModel.stickerBackgroundRemovalState.collectAsState()

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var showFutureMailDatePicker by remember {
        mutableStateOf(false)
    }

    var showFutureMailConfirmDialog by remember {
        mutableStateOf(false)
    }

    var pendingFutureMailDeliverAt by remember {
        mutableStateOf<Long?>(null)
    }

    var moreMenuExpanded by remember {
        mutableStateOf(false)
    }

    var isFocusPreviewMode by rememberSaveable {
        mutableStateOf(false)
    }

    var showMessageDialog by remember {
        mutableStateOf(false)
    }

    var messageDraft by remember {
        mutableStateOf("")
    }

    val photoStickers by viewModel.photoStickers.collectAsState()
    val selectedStickerId by viewModel.selectedStickerId.collectAsState()
    val latestPhotoStickers by rememberUpdatedState(photoStickers)
    val canUndoSticker by viewModel.canUndoSticker.collectAsState()
    val canRedoSticker by viewModel.canRedoSticker.collectAsState()

    val photoSeals by viewModel.photoSeals.collectAsState()
    val selectedSealId by viewModel.selectedSealId.collectAsState()
    val latestPhotoSeals by rememberUpdatedState(photoSeals)
    val canUndoSeal by viewModel.canUndoSeal.collectAsState()
    val canRedoSeal by viewModel.canRedoSeal.collectAsState()
    val doodleStrokes by viewModel.doodleStrokes.collectAsState()
    val latestDoodleStrokes by rememberUpdatedState(doodleStrokes)
    val canUndoDoodle by viewModel.canUndoDoodle.collectAsState()
    val canRedoDoodle by viewModel.canRedoDoodle.collectAsState()
    val canUndoPhotoTransform by viewModel.canUndoPhotoTransform.collectAsState()
    val canRedoPhotoTransform by viewModel.canRedoPhotoTransform.collectAsState()
    val canUndoTemplateStyle by viewModel.canUndoTemplateStyle.collectAsState()
    val canRedoTemplateStyle by viewModel.canRedoTemplateStyle.collectAsState()
    val userTemplates by viewModel.userTemplates.collectAsState()
    val templateSaveState by viewModel.templateSaveState.collectAsState()
    val templateManageState by viewModel.templateManageState.collectAsState()
    val lastAppliedTemplateId by viewModel.lastAppliedTemplateId.collectAsState()
    val effectiveSelectedTemplateId =
        resolveEffectiveSelectedTemplateId(
            lastAppliedTemplateId = lastAppliedTemplateId,
            candidateTemplates = BuiltInTemplates.all + userTemplates,
            currentStyle = postcard?.toTemplateStyle()
        )
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var saveTemplateNameInput by remember { mutableStateOf("") }
    var templatesExpanded by remember { mutableStateOf(true) }
    var templatePendingRename by remember { mutableStateOf<PostcardTemplate?>(null) }
    var renameTemplateNameInput by remember { mutableStateOf("") }
    var templatePendingOverwrite by remember { mutableStateOf<PostcardTemplate?>(null) }
    var templatePendingDelete by remember { mutableStateOf<PostcardTemplate?>(null) }
    var stampPhotoScaleDragSnapshotTaken by remember {
        mutableStateOf(false)
    }
    var polaroidPhotoScaleDragSnapshotTaken by remember {
        mutableStateOf(false)
    }
    val latestPostcard by rememberUpdatedState(postcard)

    var stickerEditMode by remember {
        mutableStateOf(StickerEditMode.Move)
    }
    var stickerEditModeOwnerId by remember {
        mutableStateOf<String?>(null)
    }
    val resolvedStickerEditMode =
        if (
            selectedStickerId != null &&
            selectedStickerId == stickerEditModeOwnerId
        ) {
            stickerEditMode
        } else {
            StickerEditMode.Move
        }

    var stickerSizes by remember {
        mutableStateOf(mapOf<String, IntSize>())
    }

    var sealSizes by remember {
        mutableStateOf(mapOf<String, IntSize>())
    }

    var doodleTool by remember {
        mutableStateOf(DoodleTool.PEN)
    }
    val latestDoodleTool by rememberUpdatedState(doodleTool)
    var doodleColorArgb by remember {
        mutableStateOf(SealInkBlack.toArgb().toLong() and 0xFFFFFFFFL)
    }
    val latestDoodleColorArgb by rememberUpdatedState(doodleColorArgb)
    var doodleWidth by remember {
        mutableStateOf(DoodleStrokeWidth.MEDIUM)
    }
    val latestDoodleWidth by rememberUpdatedState(doodleWidth)
    var currentDoodleStrokePoints by remember {
        mutableStateOf(listOf<DoodlePoint>())
    }
    var isDrawingDoodle by remember {
        mutableStateOf(false)
    }

    /** 낙서가 직선 입력으로 전환된 순간을 손끝으로 알리는 데만 쓴다. */
    val hapticFeedback = LocalHapticFeedback.current

    var backgroundRemovalError by remember {
        mutableStateOf<String?>(null)
    }

    var postcardPreviewSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    val baseStickerPx = with(LocalDensity.current) {
        STICKER_BASE_SIZE.toPx()
    }

    val baseSealPx = with(LocalDensity.current) {
        SEAL_BASE_SIZE.toPx()
    }

    val sealMinVisibleEdgePx = with(LocalDensity.current) {
        SEAL_MIN_VISIBLE_EDGE.toPx()
    }
    val stickerScaleHandleTouchSize = 44.dp
    val stickerScaleHandleVisibleSize = 24.dp
    val stickerScaleHandleTouchPx = with(LocalDensity.current) {
        stickerScaleHandleTouchSize.toPx()
    }

    val context = LocalContext.current

    val postcardPhotoPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                viewModel.updatePostcardImage(uri)
            }
        }

    val postcardFilePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                viewModel.updatePostcardImage(uri)
            }
        }

    var showPhotoSourceMenu by rememberSaveable {
        mutableStateOf(false)
    }

    /*
     * TakePicture 결과 콜백은 화면 재구성이나
     * 프로세스 재생성 뒤에도 올 수 있어서,
     * 임시 촬영 파일 경로를 rememberSaveable로 들고 있는다.
     */
    var pendingCameraCapturePath by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    /*
     * 촬영 성공 후 viewModel.updatePostcardImage()가
     * 내부 저장을 끝낼 때까지는 임시 파일을 지우면 안 되므로,
     * imageUpdateState가 Success/Error로 정리된 뒤에만 삭제한다.
     */
    var pendingCameraCaptureCleanupPath by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val postcardCameraCapture =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            val capturePath =
                pendingCameraCapturePath
            pendingCameraCapturePath = null

            if (capturePath == null) {
                return@rememberLauncherForActivityResult
            }

            val captureFile = File(capturePath)

            if (success) {
                val captureUri =
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        captureFile
                    )

                pendingCameraCaptureCleanupPath =
                    capturePath

                viewModel.updatePostcardImage(
                    captureUri
                )
            } else {
                if (captureFile.exists()) {
                    captureFile.delete()
                }
            }
        }

    fun launchPostcardCameraCapture() {
        val captureDir =
            File(
                context.cacheDir,
                "camera_capture"
            )

        if (
            !captureDir.exists() &&
            !captureDir.mkdirs()
        ) {
            Toast.makeText(
                context,
                "임시 촬영 폴더를 만들지 못했어.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val captureFile =
            File(
                captureDir,
                "capture_${UUID.randomUUID()}.jpg"
            )

        pendingCameraCapturePath =
            captureFile.absolutePath

        val captureUri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                captureFile
            )

        runCatching {
            postcardCameraCapture.launch(captureUri)
        }.onFailure {
            pendingCameraCapturePath = null

            Toast.makeText(
                context,
                "카메라 앱을 찾지 못했어.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    var customColorDrawerExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var textScaleTarget by rememberSaveable {
        mutableStateOf(TextScaleTarget.Message)
    }

    val customizationPagerState = rememberPagerState(
        pageCount = { 6 }
    )
    val latestCustomizationPage by rememberUpdatedState(
        customizationPagerState.currentPage
    )
    val customizationPagerScope = rememberCoroutineScope()
    val customizationPageLabels = remember {
        listOf("사진", "배경", "텍스트", "스티커", "도장", "낙서")
    }
    val customizationPageIcons = remember {
        listOf(
            Icons.Default.Image,
            Icons.Default.Wallpaper,
            Icons.Default.TextFields,
            Icons.Default.EmojiEmotions,
            Icons.Default.Verified,
            Icons.Default.Draw
        )
    }
    val selectedLayout =
        remember(postcard?.layoutStyle) {
            PostcardLayoutStyle.entries
                .firstOrNull { layout ->
                    layout.name ==
                            postcard?.layoutStyle
                }
                ?: PostcardLayoutStyle.STAMP
        }

    val selectedFont =
        remember(postcard?.messageFont) {
            PostcardTextFont.entries
                .firstOrNull { font ->
                    font.name ==
                            postcard?.messageFont
                }
                ?: PostcardTextFont.SERIF
        }

    val selectedPattern =
        remember(postcard?.backgroundPattern) {
            PostcardBackgroundPattern.entries
                .firstOrNull { pattern ->
                    pattern.name ==
                            postcard?.backgroundPattern
                }
                ?: PostcardBackgroundPattern.NONE
        }

    val selectedDateFormat =
        remember(postcard?.dateFormat) {
            PostcardDateFormat.entries
                .firstOrNull { dateFormat ->
                    dateFormat.name ==
                            postcard?.dateFormat
                }
                ?: PostcardDateFormat.DOT
        }

    val messageTextScalePercent =
        ((postcard?.messageTextScale ?: 1f) * 100f)
            .roundToInt()

    val dateTextScalePercent =
        ((postcard?.dateTextScale ?: 1f) * 100f)
            .roundToInt()

    val backgroundPatternDensityPercent =
        (
            (postcard?.backgroundPatternDensity ?: 1f) *
                    100f
            ).roundToInt()

    val stampPhotoScalePercent =
        ((postcard?.stampPhotoScale ?: 1f) * 100f)
            .roundToInt()

    val polaroidPhotoScalePercent =
        ((postcard?.polaroidPhotoScale ?: 1f) * 100f)
            .roundToInt()

    val photoEdgeBlurPercent =
        ((postcard?.photoEdgeBlur ?: 0f) * 100f)
            .roundToInt()

    LaunchedEffect(postcardId) {
        viewModel.resetShareState()
        viewModel.loadPostcard(postcardId)
        viewModel.loadStickerSealStateAndAutoRestoreDraft(postcardId)
    }

    val exitScope = rememberCoroutineScope()
    // 아래 시스템 back(BackHandler)은 controlsEnabled(backgroundUpdateState 등
    // Saving 플래그)를 전혀 확인하지 않으므로, 저장이 실제 DAO 쓰기에 닿기 전에도
    // 화면을 나갈 수 있다. 나가기 전 아직 끝나지 않은 저장을 기다려야
    // ViewModelStore가 clear()되기 전에 마지막 값이 Room에 반영된다.
    val navigateBackAfterPendingStyleSaves: () -> Unit = {
        exitScope.launch {
            viewModel.awaitPendingStyleSaves()
            onNavigateBack()
        }
    }

    BackHandler(enabled = isFocusPreviewMode) {
        isFocusPreviewMode = false
    }

    BackHandler(enabled = !isFocusPreviewMode) {
        navigateBackAfterPendingStyleSaves()
    }

    // 미래로 발송된 엽서는 직접 detail/{id} 딥링크로 들어오더라도 편집
    // UI/미리보기를 절대 그리지 않는다 — 아래 이 시점 이후의 코드(편집기,
    // 상단 액션 바, 공유·내보내기 다이얼로그 등)는 SENT 상태에서는 아예
    // 도달하지 않는다. 편집 UI가 없으니 어떤 조작도 트리거될 수 없어
    // controlsEnabled 등 다른 가드와 별개로 완전히 차단된다.
    val sealedPostcard = postcard
    if (sealedPostcard != null && sealedPostcard.futureMailState == FUTURE_MAIL_STATE_SENT) {
        val deliverAtMillis = sealedPostcard.futureMailDeliverAt
        val now = remember { System.currentTimeMillis() }

        val arrived = remember(deliverAtMillis, now) {
            deliverAtMillis != null && isFutureMailArrived(deliverAtMillis, now)
        }

        val daysLeft = remember(deliverAtMillis, now) {
            deliverAtMillis?.let { daysUntilFutureMail(it, now) }
        }

        val formattedDate = remember(deliverAtMillis) {
            deliverAtMillis?.let {
                DateTimeFormatter
                    .ofPattern("yyyy년 M월 d일", Locale.KOREA)
                    .format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
            }
        }

        FutureMailSealedContent(
            arrived = arrived,
            daysLeft = daysLeft,
            formattedDate = formattedDate,
            onNavigateBack = navigateBackAfterPendingStyleSaves
        )
        return
    }

    LaunchedEffect(deleteState) {
        when (val currentDeleteState = deleteState) {
            is PostcardDeleteState.Deleted -> {
                onNavigateBack()
            }

            is PostcardDeleteState.Error -> {
                Toast.makeText(
                    context,
                    currentDeleteState.message,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.acknowledgeDeleteError()
            }

            else -> Unit
        }
    }

    LaunchedEffect(futureMailSendState) {
        when (val currentFutureMailSendState = futureMailSendState) {
            is FutureMailSendState.Sent -> {
                onNavigateBack()
            }

            is FutureMailSendState.Error -> {
                Toast.makeText(
                    context,
                    currentFutureMailSendState.message,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.acknowledgeFutureMailSendError()
            }

            else -> Unit
        }
    }

    LaunchedEffect(backgroundUpdateState) {
        if (backgroundUpdateState is BackgroundUpdateState.Success) {
            viewModel.resetBackgroundUpdateState()
        }
    }

    LaunchedEffect(imageUpdateState) {
        if (imageUpdateState is ImageUpdateState.Success) {
            viewModel.resetImageUpdateState()
        }

        /*
         * updatePostcardImage()가 Success/Error로 끝났다는 것은
         * 임시 촬영 파일의 바이트를 이미 다 읽었다는 뜻이라
         * 이 시점에 지워도 안전하다.
         */
        if (
            imageUpdateState is ImageUpdateState.Success ||
            imageUpdateState is ImageUpdateState.Error
        ) {
            pendingCameraCaptureCleanupPath?.let { cleanupPath ->
                pendingCameraCaptureCleanupPath = null

                File(cleanupPath).let { cleanupFile ->
                    if (cleanupFile.exists()) {
                        cleanupFile.delete()
                    }
                }
            }
        }
    }

    LaunchedEffect(fontUpdateState) {
        if (fontUpdateState is FontUpdateState.Success) {
            viewModel.resetFontUpdateState()
        }
    }

    LaunchedEffect(layoutUpdateState) {
        if (layoutUpdateState is LayoutUpdateState.Success) {
            viewModel.resetLayoutUpdateState()
        }
    }

    LaunchedEffect(dateFormatUpdateState) {
        if (dateFormatUpdateState is DateFormatUpdateState.Success) {
            viewModel.resetDateFormatUpdateState()
        }
    }

    LaunchedEffect(
        photoStickers,
        postcardPreviewSize,
        stickerSizes
    ) {
        if (postcardPreviewSize == IntSize.Zero) {
            return@LaunchedEffect
        }

        val updated = photoStickers.map { sticker ->
            val stickerSize =
                stickerSizes[sticker.id]

            if (
                stickerSize == null ||
                stickerSize == IntSize.Zero
            ) {
                return@map sticker
            }

            val newOffset =
                if (sticker.offset == null) {
                    centeredStickerOffset(
                        postcardSize = postcardPreviewSize,
                        stickerSize = stickerSize
                    )
                } else {
                    clampStickerOffset(
                        offset = sticker.offset,
                        postcardSize = postcardPreviewSize,
                        stickerSize = stickerSize
                    )
                }

            sticker.copy(offset = newOffset)
        }

        if (updated != photoStickers) {
            viewModel.setPhotoStickers(updated)
        }
    }

    LaunchedEffect(stickerBackgroundRemovalState) {
        when (
            val removalState =
                stickerBackgroundRemovalState
        ) {
            is StickerBackgroundRemovalState.Success -> {
                val targetId = removalState.stickerId

                val cacheUrisToDelete = mutableListOf<Uri>()

                val updatedStickers = photoStickers.map { sticker ->
                    if (sticker.id != targetId) {
                        return@map sticker
                    }

                    if (sticker.originalUri != removalState.sourceUri) {
                        cacheUrisToDelete += removalState.resultUri
                        return@map sticker
                    }

                    sticker.removedBgUri?.let { oldUri ->
                        if (oldUri != removalState.resultUri) {
                            cacheUrisToDelete += oldUri
                        }
                    }

                    sticker.copy(
                        removedBgUri = removalState.resultUri,
                        displayedUri = removalState.resultUri,
                        isBackgroundRemoved = true
                    )
                }

                viewModel.setPhotoStickers(updatedStickers)

                cacheUrisToDelete.distinct().forEach { uri ->
                    viewModel.deleteStickerCacheUri(uri)
                }

                backgroundRemovalError = null
                viewModel.resetStickerBackgroundRemovalState()
            }

            is StickerBackgroundRemovalState.Error -> {
                backgroundRemovalError =
                    removalState.message
                viewModel
                    .resetStickerBackgroundRemovalState()
            }

            else -> Unit
        }
    }

    LaunchedEffect(confirmSaveState) {
        when (confirmSaveState) {
            is ConfirmSaveState.Saved -> {
                Toast.makeText(
                    context,
                    "스티커·도장 꾸미기를 저장했어!",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.acknowledgeConfirmSaveResult()
            }

            is ConfirmSaveState.Failed -> {
                Toast.makeText(
                    context,
                    "스티커·도장 꾸미기를 저장하지 못했어. 다시 시도해줘.",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.acknowledgeConfirmSaveResult()
            }

            else -> Unit
        }
    }

    val isRemovingBackground =
        stickerBackgroundRemovalState is StickerBackgroundRemovalState.Removing

    val controlsEnabled =
        exportState !is ExportState.Exporting &&
                shareState !is ShareState.Preparing &&
                backgroundUpdateState !is BackgroundUpdateState.Saving &&
                fontUpdateState !is FontUpdateState.Saving &&
                layoutUpdateState !is LayoutUpdateState.Saving &&
                dateFormatUpdateState !is DateFormatUpdateState.Saving &&
                imageUpdateState !is ImageUpdateState.Saving &&
                confirmSaveState !is ConfirmSaveState.Saving &&
                deleteState !is PostcardDeleteState.Deleting &&
                !isRemovingBackground
    val latestControlsEnabled by rememberUpdatedState(controlsEnabled)

    val detailSnackbarHostState =
        remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.textScaleSaveErrors.collect { message ->
            detailSnackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.draftAutoRestoredEvents.collect {
            val result =
                detailSnackbarHostState.showSnackbar(
                    message = "이전에 꾸미던 상태를 불러왔어요",
                    actionLabel = "원래대로",
                    duration = SnackbarDuration.Short
                )

            if (result == SnackbarResult.ActionPerformed) {
                viewModel.revertToConfirmedState()
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.flushDraftNow()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(shareState) {
        val currentShareState = shareState
        if (currentShareState is ShareState.Error) {
            Toast.makeText(
                context,
                currentShareState.message,
                Toast.LENGTH_SHORT
            ).show()
            viewModel.resetShareState()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackgroundGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 20.dp
                ),
            verticalArrangement =
                if (isFocusPreviewMode) {
                    Arrangement.Center
                } else {
                    Arrangement.Top
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFocusPreviewMode) {
                Spacer(
                    modifier = Modifier.height(72.dp)
                )
            }

            postcard?.let { pc ->
                val postcardPreviewWidthFraction =
                    if (isFocusPreviewMode) 0.96f else 0.8f

                Box(
                    modifier = Modifier.fillMaxWidth(
                        postcardPreviewWidthFraction
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RectangleShape)
                            .background(
                                color = Color(
                                    pc.backgroundColorArgb
                                ),
                                shape = RectangleShape
                            )
                            .onSizeChanged { size ->
                                postcardPreviewSize = size
                            }
                            .pointerInput(
                                selectedLayout,
                                isFocusPreviewMode
                            ) {
                                if (isFocusPreviewMode) {
                                    return@pointerInput
                                }

                                val panSensitivity = 0.45f
                                var tapedFilmDragArmed = true
                                var photoTransformGestureSnapshotPending = true

                                coroutineScope {
                                    launch {
                                        awaitEachGesture {
                                            val down =
                                                awaitFirstDown(
                                                    requireUnconsumed = false
                                                )
                                            tapedFilmDragArmed =
                                                selectedLayout !=
                                                        PostcardLayoutStyle.TAPED_FILM ||
                                                        isPositionInsideTapedFilmPhotoBounds(
                                                            position = down.position,
                                                            previewSize = postcardPreviewSize,
                                                            stampPhotoScale =
                                                                latestPostcard
                                                                    ?.stampPhotoScale
                                                                    ?: 1f
                                                        )
                                            photoTransformGestureSnapshotPending = true
                                        }
                                    }

                                    launch {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        if (
                                            !latestControlsEnabled ||
                                            latestCustomizationPage ==
                                            DOODLE_TAB_PAGE_INDEX
                                        ) {
                                            return@detectTransformGestures
                                        }

                                        if (
                                            selectedLayout ==
                                                    PostcardLayoutStyle.TAPED_FILM &&
                                                    !tapedFilmDragArmed
                                        ) {
                                            return@detectTransformGestures
                                        }

                                        val current =
                                            latestPostcard
                                                ?: return@detectTransformGestures

                                        if (photoTransformGestureSnapshotPending) {
                                            viewModel.recordPhotoTransformSnapshotForUndo()
                                            photoTransformGestureSnapshotPending = false
                                        }

                                        val oldOffsetX =
                                            when (selectedLayout) {
                                                PostcardLayoutStyle.POLAROID ->
                                                    current.polaroidPhotoOffsetX
                                                PostcardLayoutStyle.TAPED_FILM ->
                                                    current.tapedFilmPhotoOffsetX
                                                PostcardLayoutStyle.STAMP,
                                                PostcardLayoutStyle.LETTER ->
                                                    current.stampPhotoOffsetX
                                            }
                                        val oldOffsetY =
                                            when (selectedLayout) {
                                                PostcardLayoutStyle.POLAROID ->
                                                    current.polaroidPhotoOffsetY
                                                PostcardLayoutStyle.TAPED_FILM ->
                                                    current.tapedFilmPhotoOffsetY
                                                PostcardLayoutStyle.STAMP,
                                                PostcardLayoutStyle.LETTER ->
                                                    current.stampPhotoOffsetY
                                            }
                                        val oldZoom =
                                            when (selectedLayout) {
                                                PostcardLayoutStyle.POLAROID ->
                                                    current.polaroidPhotoZoom
                                                PostcardLayoutStyle.TAPED_FILM ->
                                                    current.tapedFilmPhotoZoom
                                                PostcardLayoutStyle.STAMP,
                                                PostcardLayoutStyle.LETTER ->
                                                    current.stampPhotoZoom
                                            }

                                        val newZoom =
                                            (oldZoom * zoom).coerceIn(1f, 3f)

                                        val newOffsetX =
                                            if (postcardPreviewSize.width > 0) {
                                                (
                                                        oldOffsetX -
                                                                (pan.x / postcardPreviewSize.width) *
                                                                panSensitivity * 2f
                                                        ).coerceIn(-1f, 1f)
                                            } else {
                                                oldOffsetX
                                            }
                                        val newOffsetY =
                                            if (postcardPreviewSize.height > 0) {
                                                (
                                                        oldOffsetY -
                                                                (pan.y / postcardPreviewSize.height) *
                                                                panSensitivity * 2f
                                                        ).coerceIn(-1f, 1f)
                                            } else {
                                                oldOffsetY
                                            }

                                        when (selectedLayout) {
                                            PostcardLayoutStyle.POLAROID -> {
                                                viewModel.setPolaroidPhotoOffsetPreview(
                                                    newOffsetX,
                                                    newOffsetY
                                                )
                                                viewModel.savePolaroidPhotoOffset(
                                                    newOffsetX,
                                                    newOffsetY
                                                )
                                                viewModel.setPolaroidPhotoZoomPreview(newZoom)
                                                viewModel.savePolaroidPhotoZoom(newZoom)
                                            }

                                            PostcardLayoutStyle.TAPED_FILM -> {
                                                viewModel.setTapedFilmPhotoOffsetPreview(
                                                    newOffsetX,
                                                    newOffsetY
                                                )
                                                viewModel.saveTapedFilmPhotoOffset(
                                                    newOffsetX,
                                                    newOffsetY
                                                )
                                                viewModel.setTapedFilmPhotoZoomPreview(newZoom)
                                                viewModel.saveTapedFilmPhotoZoom(newZoom)
                                            }

                                            PostcardLayoutStyle.STAMP,
                                            PostcardLayoutStyle.LETTER -> {
                                                viewModel.setStampPhotoOffsetPreview(
                                                    newOffsetX,
                                                    newOffsetY
                                                )
                                                viewModel.saveStampPhotoOffset(
                                                    newOffsetX,
                                                    newOffsetY
                                                )
                                                viewModel.setStampPhotoZoomPreview(newZoom)
                                                viewModel.saveStampPhotoZoom(newZoom)
                                            }
                                        }
                                    }
                                    }
                                }
                            }
                    ) {
                        PostcardPreviewContent(
                            imagePath = pc.imagePath,
                            message = pc.message,
                            backgroundColorArgb =
                                pc.backgroundColorArgb,
                            backgroundPattern =
                                selectedPattern.name,
                            messageFont = selectedFont.name,
                            layoutStyle = selectedLayout.name,
                            capturedAt = pc.capturedAt,
                            dateFormat =
                                selectedDateFormat.name,
                            messageTextScale =
                                pc.messageTextScale,
                            dateTextScale =
                                pc.dateTextScale,
                            backgroundPatternDensity =
                                pc.backgroundPatternDensity,
                            stampPhotoScale =
                                pc.stampPhotoScale,
                            polaroidPhotoScale =
                                pc.polaroidPhotoScale,
                            photoEdgeBlur =
                                pc.photoEdgeBlur,
                            stampPhotoOffsetX =
                                pc.stampPhotoOffsetX,
                            stampPhotoOffsetY =
                                pc.stampPhotoOffsetY,
                            polaroidPhotoOffsetX =
                                pc.polaroidPhotoOffsetX,
                            polaroidPhotoOffsetY =
                                pc.polaroidPhotoOffsetY,
                            tapedFilmPhotoOffsetX =
                                pc.tapedFilmPhotoOffsetX,
                            tapedFilmPhotoOffsetY =
                                pc.tapedFilmPhotoOffsetY,
                            stampPhotoZoom =
                                pc.stampPhotoZoom,
                            polaroidPhotoZoom =
                                pc.polaroidPhotoZoom,
                            tapedFilmPhotoZoom =
                                pc.tapedFilmPhotoZoom
                        )

                        photoStickers.forEach { sticker ->
                            val isSelected =
                                sticker.id == selectedStickerId
                            val isVisuallySelected =
                                isSelected &&
                                        !isFocusPreviewMode &&
                                        latestCustomizationPage ==
                                        STICKER_TAB_PAGE_INDEX
                            val perStickerEditMode =
                                if (isSelected) {
                                    resolvedStickerEditMode
                                } else {
                                    StickerEditMode.Move
                                }
                            val currentOffset =
                                sticker.offset

                            val stickerPositionModifier =
                                if (currentOffset == null) {
                                    Modifier.align(
                                        Alignment.Center
                                    )
                                } else {
                                    Modifier
                                        .align(
                                            Alignment.TopStart
                                        )
                                        .offset {
                                            IntOffset(
                                                x = currentOffset.x
                                                    .roundToInt(),
                                                y = currentOffset.y
                                                    .roundToInt()
                                            )
                                        }
                                }

                            val imageModifier =
                                when {
                                    sticker.isBackgroundRemoved && isVisuallySelected ->
                                        Modifier
                                            .fillMaxSize()
                                            .border(
                                                width = 3.dp,
                                                color = GraphiteAccent,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                    sticker.isBackgroundRemoved ->
                                        Modifier.fillMaxSize()
                                    isVisuallySelected ->
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(
                                                width = 3.dp,
                                                color = GraphiteAccent,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                    else ->
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                }

                            Box(
                                modifier = stickerPositionModifier
                                    .size(STICKER_BASE_SIZE * sticker.scale)
                                    .onSizeChanged { size ->
                                        stickerSizes =
                                            stickerSizes +
                                                    (sticker.id to size)
                                    }
                            ) {
                                AsyncImage(
                                    model = sticker.displayedUri,
                                    contentDescription =
                                        "포스트카드 스티커 사진",
                                    contentScale =
                                        if (sticker.isBackgroundRemoved) {
                                            ContentScale.Fit
                                        } else {
                                            ContentScale.Crop
                                        },
                                    modifier = imageModifier
                                        .graphicsLayer {
                                            rotationZ =
                                                sticker.rotationDegrees
                                            scaleX =
                                                if (sticker.flipHorizontal) -1f else 1f
                                            scaleY =
                                                if (sticker.flipVertical) -1f else 1f
                                        }
                                        .then(
                                            if (
                                                latestCustomizationPage ==
                                                STICKER_TAB_PAGE_INDEX
                                            ) {
                                                Modifier.pointerInput(
                                            sticker.id,
                                            postcardPreviewSize,
                                            perStickerEditMode,
                                            isFocusPreviewMode
                                        ) {
                                            if (isFocusPreviewMode) {
                                                return@pointerInput
                                            }

                                            var stickerGestureSnapshotPending = true

                                            coroutineScope {
                                            launch {
                                                awaitEachGesture {
                                                    awaitFirstDown(
                                                        requireUnconsumed = false
                                                    )
                                                    stickerGestureSnapshotPending = true
                                                }
                                            }
                                            launch {
                                                detectTapGestures(
                                                    onTap = {
                                                        if (selectedStickerId == sticker.id) {
                                                            stickerEditModeOwnerId = null
                                                            viewModel.setSelectedStickerId(null)
                                                        } else {
                                                            viewModel.setSelectedStickerId(
                                                                sticker.id
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                            launch {
                                            when (perStickerEditMode) {
                                                StickerEditMode.Move -> {
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            viewModel.setSelectedStickerId(
                                                                sticker.id
                                                            )
                                                            viewModel.recordStickerSnapshotForUndo()
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()

                                                            if (postcardPreviewSize == IntSize.Zero) {
                                                                return@detectDragGestures
                                                            }

                                                            val currentSticker =
                                                                latestPhotoStickers.find {
                                                                    it.id == sticker.id
                                                                } ?: return@detectDragGestures

                                                            val currentStickerSize =
                                                                stickerSizes[sticker.id]
                                                                    ?: IntSize.Zero

                                                            val oldOffset =
                                                                currentSticker.offset
                                                                    ?: centeredStickerOffset(
                                                                        postcardSize = postcardPreviewSize,
                                                                        stickerSize = currentStickerSize
                                                                    )

                                                            val parentSpaceDrag =
                                                                localStickerDeltaToParent(
                                                                    localDelta = dragAmount,
                                                                    rotationDegrees =
                                                                        currentSticker.rotationDegrees,
                                                                    flipHorizontal =
                                                                        currentSticker.flipHorizontal,
                                                                    flipVertical =
                                                                        currentSticker.flipVertical
                                                                )

                                                            viewModel.setPhotoStickers(
                                                                latestPhotoStickers.map {
                                                                    if (it.id == sticker.id) {
                                                                        it.copy(
                                                                            offset = clampStickerOffset(
                                                                                offset = oldOffset + parentSpaceDrag,
                                                                                postcardSize = postcardPreviewSize,
                                                                                stickerSize = currentStickerSize
                                                                            )
                                                                        )
                                                                    } else {
                                                                        it
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    )
                                                }

                                                StickerEditMode.Scale -> {
                                                    detectTransformGestures {
                                                        centroid, _, zoom, _ ->

                                                        viewModel.setSelectedStickerId(sticker.id)

                                                        val currentSticker =
                                                            latestPhotoStickers.find {
                                                                it.id == sticker.id
                                                            } ?: return@detectTransformGestures

                                                        if (stickerGestureSnapshotPending) {
                                                            viewModel.recordStickerSnapshotForUndo()
                                                            stickerGestureSnapshotPending = false
                                                        }

                                                        val oldScale = currentSticker.scale
                                                        val newScale =
                                                            (oldScale * zoom).coerceIn(0.5f, 2.5f)
                                                        val actualZoom =
                                                            newScale / oldScale

                                                        if (postcardPreviewSize == IntSize.Zero) {
                                                            viewModel.setPhotoStickers(
                                                                latestPhotoStickers.map {
                                                                    if (it.id == sticker.id) {
                                                                        it.copy(scale = newScale)
                                                                    } else {
                                                                        it
                                                                    }
                                                                }
                                                            )
                                                            return@detectTransformGestures
                                                        }

                                                        val currentStickerSize =
                                                            stickerSizes[sticker.id]
                                                                ?: IntSize.Zero

                                                        val oldOffset =
                                                            currentSticker.offset
                                                                ?: centeredStickerOffset(
                                                                    postcardSize = postcardPreviewSize,
                                                                    stickerSize = currentStickerSize
                                                                )

                                                        val correctedOffset =
                                                            oldOffset +
                                                                    localStickerDeltaToParent(
                                                                        localDelta =
                                                                            centroid * (1f - actualZoom),
                                                                        rotationDegrees =
                                                                            currentSticker.rotationDegrees,
                                                                        flipHorizontal =
                                                                            currentSticker.flipHorizontal,
                                                                        flipVertical =
                                                                            currentSticker.flipVertical
                                                                    )

                                                        val newSizePx =
                                                            (baseStickerPx * newScale).roundToInt()
                                                        val newEffectiveSize =
                                                            IntSize(newSizePx, newSizePx)

                                                        viewModel.setPhotoStickers(
                                                            latestPhotoStickers.map {
                                                                if (it.id == sticker.id) {
                                                                    it.copy(
                                                                        scale = newScale,
                                                                        offset = clampStickerOffset(
                                                                            offset = correctedOffset,
                                                                            postcardSize = postcardPreviewSize,
                                                                            stickerSize = newEffectiveSize
                                                                        )
                                                                    )
                                                                } else {
                                                                    it
                                                                }
                                                            }
                                                        )
                                                    }
                                                }

                                                StickerEditMode.Rotate -> {
                                                    detectTransformGestures {
                                                        _, _, _, rotationChange ->

                                                        viewModel.setSelectedStickerId(sticker.id)

                                                        val currentSticker =
                                                            latestPhotoStickers.find {
                                                                it.id == sticker.id
                                                            } ?: return@detectTransformGestures

                                                        if (stickerGestureSnapshotPending) {
                                                            viewModel.recordStickerSnapshotForUndo()
                                                            stickerGestureSnapshotPending = false
                                                        }

                                                        val newRotation =
                                                            normalizeStickerRotation(
                                                                currentSticker.rotationDegrees +
                                                                        rotationChange
                                                            )

                                                        viewModel.setPhotoStickers(
                                                            latestPhotoStickers.map {
                                                                if (it.id == sticker.id) {
                                                                    it.copy(
                                                                        rotationDegrees = newRotation
                                                                    )
                                                                } else {
                                                                    it
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        }  // launch (mode gesture)
                                        }  // coroutineScope
                                            } else {
                                                Modifier
                                            }
                                        )
                                )

                                if (isVisuallySelected && perStickerEditMode == StickerEditMode.Rotate) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .offset(y = (-22).dp)
                                            .size(stickerScaleHandleTouchSize)
                                            .then(
                                                if (
                                                    latestCustomizationPage ==
                                                    STICKER_TAB_PAGE_INDEX
                                                ) {
                                                    Modifier.pointerInput(
                                                sticker.id,
                                                postcardPreviewSize,
                                                stickerScaleHandleTouchPx,
                                                isFocusPreviewMode
                                            ) {
                                                if (isFocusPreviewMode) {
                                                    return@pointerInput
                                                }

                                                var rotationGestureActive = false
                                                var gestureStartRotation = 0f
                                                var gestureStartCenter = Offset.Zero
                                                var gestureStartTouch = Offset.Zero
                                                var gestureStartAngle = 0f
                                                var accumulatedDrag = Offset.Zero

                                                detectDragGestures(
                                                    onDragStart = { startTouch ->
                                                        rotationGestureActive =
                                                            latestControlsEnabled
                                                        val currentSticker =
                                                            if (rotationGestureActive) {
                                                                latestPhotoStickers.find {
                                                                    it.id == sticker.id
                                                                }
                                                            } else {
                                                                null
                                                            }

                                                        if (
                                                            currentSticker == null ||
                                                            postcardPreviewSize == IntSize.Zero
                                                        ) {
                                                            rotationGestureActive = false
                                                        } else {
                                                            viewModel.setSelectedStickerId(
                                                                sticker.id
                                                            )
                                                            viewModel.recordStickerSnapshotForUndo()

                                                            gestureStartRotation =
                                                                currentSticker.rotationDegrees
                                                            val startSidePx =
                                                                baseStickerPx *
                                                                        currentSticker.scale
                                                            val startSize =
                                                                IntSize(
                                                                    startSidePx
                                                                        .roundToInt(),
                                                                    startSidePx
                                                                        .roundToInt()
                                                                )
                                                            val startOffset =
                                                                currentSticker.offset
                                                                    ?: centeredStickerOffset(
                                                                        postcardSize =
                                                                            postcardPreviewSize,
                                                                        stickerSize =
                                                                            startSize
                                                                    )
                                                            gestureStartCenter =
                                                                startOffset +
                                                                        Offset(
                                                                            x = startSidePx / 2f,
                                                                            y = startSidePx / 2f
                                                                        )

                                                            val handleTopLeft =
                                                                Offset(
                                                                    x = (
                                                                        startSidePx -
                                                                                stickerScaleHandleTouchPx
                                                                        ) / 2f,
                                                                    y = -stickerScaleHandleTouchPx / 2f
                                                                )
                                                            gestureStartTouch =
                                                                startOffset +
                                                                        handleTopLeft +
                                                                        startTouch
                                                            gestureStartAngle =
                                                                atan2(
                                                                    gestureStartTouch.y -
                                                                            gestureStartCenter.y,
                                                                    gestureStartTouch.x -
                                                                            gestureStartCenter.x
                                                                )
                                                            accumulatedDrag =
                                                                Offset.Zero
                                                        }
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        if (rotationGestureActive) {
                                                            change.consume()
                                                            accumulatedDrag += dragAmount

                                                            val currentTouch =
                                                                gestureStartTouch +
                                                                        accumulatedDrag
                                                            val currentAngle =
                                                                atan2(
                                                                    currentTouch.y -
                                                                            gestureStartCenter.y,
                                                                    currentTouch.x -
                                                                            gestureStartCenter.x
                                                                )
                                                            val angleDeltaDegrees =
                                                                (
                                                                    currentAngle -
                                                                            gestureStartAngle
                                                                    ) * 180f /
                                                                        kotlin.math.PI.toFloat()
                                                            val newRotation =
                                                                normalizeStickerRotation(
                                                                    gestureStartRotation +
                                                                            angleDeltaDegrees
                                                                )

                                                            viewModel.setPhotoStickers(
                                                                latestPhotoStickers.map {
                                                                    if (it.id == sticker.id) {
                                                                        it.copy(
                                                                            rotationDegrees =
                                                                                newRotation
                                                                        )
                                                                    } else {
                                                                        it
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        rotationGestureActive = false
                                                    },
                                                    onDragCancel = {
                                                        rotationGestureActive = false
                                                    }
                                                )
                                            }
                                                } else {
                                                    Modifier
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(
                                                    stickerScaleHandleVisibleSize
                                                )
                                                .background(
                                                    color = BrutalWhite,
                                                    shape = CircleShape
                                                )
                                                .border(
                                                    width = 2.dp,
                                                    color = BrutalBlack,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }

                                if (isVisuallySelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 8.dp, y = (-8).dp)
                                            .size(26.dp)
                                            .background(
                                                color = GalleryDangerRed,
                                                shape = CircleShape
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                color = BrutalBlack,
                                                shape = CircleShape
                                            )
                                            .clickable(
                                                enabled = controlsEnabled
                                            ) {
                                                val toDelete =
                                                    photoStickers.find {
                                                        it.id == sticker.id
                                                    }
                                                val removedBgUriToDelete =
                                                    toDelete?.removedBgUri

                                                viewModel.recordStickerSnapshotForUndo()

                                                val remaining =
                                                    photoStickers.filter {
                                                        it.id != sticker.id
                                                    }
                                                viewModel.setPhotoStickers(remaining)

                                                removedBgUriToDelete?.let { uri ->
                                                    viewModel
                                                        .deleteStickerCacheUri(uri)
                                                }
                                                toDelete?.originalUri?.let { uri ->
                                                    viewModel
                                                        .deleteStickerOriginalIfUnreferenced(
                                                            uri,
                                                            remaining
                                                        )
                                                }
                                                stickerSizes =
                                                    stickerSizes - sticker.id
                                                if (selectedStickerId == sticker.id) {
                                                    viewModel.setSelectedStickerId(
                                                        remaining.lastOrNull()?.id
                                                    )
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "스티커 삭제",
                                            tint = BrutalWhite,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                if (isVisuallySelected && perStickerEditMode == StickerEditMode.Scale) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(stickerScaleHandleTouchSize)
                                            .then(
                                                if (
                                                    latestCustomizationPage ==
                                                    STICKER_TAB_PAGE_INDEX
                                                ) {
                                                    Modifier.pointerInput(
                                                sticker.id,
                                                postcardPreviewSize,
                                                stickerScaleHandleTouchPx,
                                                isFocusPreviewMode
                                            ) {
                                                if (isFocusPreviewMode) {
                                                    return@pointerInput
                                                }

                                                var scaleGestureActive = false
                                                var gestureStartScale = 1f
                                                var gestureStartCenter = Offset.Zero
                                                var gestureStartTouch = Offset.Zero
                                                var gestureStartDistance = 1f
                                                var accumulatedDrag = Offset.Zero

                                                detectDragGestures(
                                                    onDragStart = { startTouch ->
                                                        scaleGestureActive =
                                                            latestControlsEnabled
                                                        val currentSticker =
                                                            if (scaleGestureActive) {
                                                                latestPhotoStickers.find {
                                                                    it.id == sticker.id
                                                                }
                                                            } else {
                                                                null
                                                            }

                                                        if (currentSticker == null) {
                                                            scaleGestureActive = false
                                                        } else {
                                                            viewModel.setSelectedStickerId(
                                                                sticker.id
                                                            )
                                                            viewModel.recordStickerSnapshotForUndo()

                                                            gestureStartScale =
                                                                currentSticker.scale
                                                            val startSidePx =
                                                                baseStickerPx *
                                                                        gestureStartScale
                                                            val startSize =
                                                                IntSize(
                                                                    startSidePx
                                                                        .roundToInt(),
                                                                    startSidePx
                                                                        .roundToInt()
                                                                )
                                                            val startOffset =
                                                                currentSticker.offset
                                                                    ?: centeredStickerOffset(
                                                                        postcardSize =
                                                                            postcardPreviewSize,
                                                                        stickerSize =
                                                                            startSize
                                                                    )
                                                            gestureStartCenter =
                                                                startOffset +
                                                                        Offset(
                                                                            x = startSidePx / 2f,
                                                                            y = startSidePx / 2f
                                                                        )

                                                            val handleTopLeft =
                                                                Offset(
                                                                    x = (
                                                                        startSidePx -
                                                                                stickerScaleHandleTouchPx
                                                                        ).coerceAtLeast(0f),
                                                                    y = (
                                                                        startSidePx -
                                                                                stickerScaleHandleTouchPx
                                                                        ).coerceAtLeast(0f)
                                                                )
                                                            gestureStartTouch =
                                                                handleTopLeft + startTouch
                                                            gestureStartDistance =
                                                                (
                                                                    gestureStartTouch -
                                                                            gestureStartCenter
                                                                    ).getDistance()
                                                                    .coerceAtLeast(1f)
                                                            accumulatedDrag =
                                                                Offset.Zero
                                                        }
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        if (scaleGestureActive) {
                                                            change.consume()
                                                            accumulatedDrag += dragAmount

                                                            val currentTouch =
                                                                gestureStartTouch +
                                                                        accumulatedDrag
                                                            val currentDistance =
                                                                (
                                                                    currentTouch -
                                                                            gestureStartCenter
                                                                    ).getDistance()
                                                                    .coerceAtLeast(1f)
                                                            val newScale =
                                                                (
                                                                    gestureStartScale *
                                                                            currentDistance /
                                                                            gestureStartDistance
                                                                    ).coerceIn(0.5f, 2.5f)
                                                            val newSidePx =
                                                                baseStickerPx * newScale
                                                            val newSize =
                                                                IntSize(
                                                                    newSidePx
                                                                        .roundToInt(),
                                                                    newSidePx
                                                                        .roundToInt()
                                                                )
                                                            val newOffset =
                                                                gestureStartCenter -
                                                                        Offset(
                                                                            x = newSidePx / 2f,
                                                                            y = newSidePx / 2f
                                                                        )

                                                            viewModel.setPhotoStickers(
                                                                latestPhotoStickers.map {
                                                                    if (it.id == sticker.id) {
                                                                        it.copy(
                                                                            scale = newScale,
                                                                            offset =
                                                                                clampStickerOffset(
                                                                                    offset =
                                                                                        newOffset,
                                                                                    postcardSize =
                                                                                        postcardPreviewSize,
                                                                                    stickerSize =
                                                                                        newSize
                                                                                )
                                                                        )
                                                                    } else {
                                                                        it
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        scaleGestureActive = false
                                                    },
                                                    onDragCancel = {
                                                        scaleGestureActive = false
                                                    }
                                                )
                                            }
                                                } else {
                                                    Modifier
                                                }
                                            ),
                                        contentAlignment = Alignment.BottomEnd
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(
                                                    stickerScaleHandleVisibleSize
                                                )
                                                .background(
                                                    color = BrutalWhite,
                                                    shape = CircleShape
                                                )
                                                .border(
                                                    width = 2.dp,
                                                    color = GraphiteAccent,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        photoSeals.forEach { seal ->
                            val isSealSelected =
                                seal.id == selectedSealId
                            val isSealVisuallySelected =
                                isSealSelected &&
                                        !isFocusPreviewMode &&
                                        latestCustomizationPage ==
                                        SEAL_TAB_PAGE_INDEX
                            val currentSealOffset =
                                seal.offset

                            val sealVisualSize = SEAL_BASE_SIZE * seal.scale
                            val sealHitAreaSize =
                                if (isSealSelected) {
                                    maxOf(sealVisualSize, SEAL_SELECTED_MIN_GESTURE_SIZE)
                                } else {
                                    maxOf(sealVisualSize, SEAL_MIN_HIT_TARGET_SIZE)
                                }
                            val sealHitAreaPadding =
                                (sealHitAreaSize - sealVisualSize) / 2f

                            val sealPositionModifier =
                                if (currentSealOffset == null) {
                                    Modifier.align(
                                        Alignment.Center
                                    )
                                } else {
                                    Modifier
                                        .align(
                                            Alignment.TopStart
                                        )
                                        .offset {
                                            val paddingPx =
                                                sealHitAreaPadding.toPx()
                                            IntOffset(
                                                x = (currentSealOffset.x - paddingPx)
                                                    .roundToInt(),
                                                y = (currentSealOffset.y - paddingPx)
                                                    .roundToInt()
                                            )
                                        }
                                }

                            Box(
                                modifier = sealPositionModifier
                                    .size(sealHitAreaSize)
                                    .graphicsLayer {
                                        rotationZ =
                                            seal.rotationDegrees
                                    }
                                    .then(
                                        if (
                                            latestCustomizationPage ==
                                            SEAL_TAB_PAGE_INDEX
                                        ) {
                                            Modifier.pointerInput(
                                        seal.id,
                                        postcardPreviewSize,
                                        isFocusPreviewMode
                                    ) {
                                        if (isFocusPreviewMode) {
                                            return@pointerInput
                                        }

                                        var sealGestureSnapshotPending = true
                                        var activeSealPointerCount = 0

                                        coroutineScope {
                                            launch {
                                                awaitEachGesture {
                                                    awaitFirstDown(
                                                        requireUnconsumed = false
                                                    )
                                                    sealGestureSnapshotPending = true
                                                }
                                            }
                                            launch {
                                                awaitPointerEventScope {
                                                    while (true) {
                                                        val event =
                                                            awaitPointerEvent(
                                                                PointerEventPass.Initial
                                                            )
                                                        val pressedCount =
                                                            event.changes.count {
                                                                it.pressed
                                                            }

                                                        // 마지막 손가락이 방금 떨어진 순간(제스처 종료) —
                                                        // 최소 가시 영역을 벗어났다면 가장 가까운 안전
                                                        // 위치로만 즉시 보정한다(새 Undo 스냅샷 없음).
                                                        if (
                                                            activeSealPointerCount > 0 &&
                                                            pressedCount == 0
                                                        ) {
                                                            val currentSeal =
                                                                latestPhotoSeals.find {
                                                                    it.id == seal.id
                                                                }
                                                            val currentSealSize =
                                                                sealSizes[seal.id]

                                                            if (
                                                                currentSeal?.offset != null &&
                                                                currentSealSize != null &&
                                                                postcardPreviewSize != IntSize.Zero
                                                            ) {
                                                                val correctedOffset =
                                                                    correctSealOffsetForMinimumVisibility(
                                                                        offset = currentSeal.offset,
                                                                        sealSize = currentSealSize,
                                                                        rotationDegrees = currentSeal.rotationDegrees,
                                                                        postcardSize = postcardPreviewSize,
                                                                        minimumVisibleEdgePx =
                                                                            SEAL_MIN_VISIBLE_EDGE.toPx()
                                                                    )

                                                                if (correctedOffset != currentSeal.offset) {
                                                                    viewModel.setPhotoSeals(
                                                                        latestPhotoSeals.map {
                                                                            if (it.id == seal.id) {
                                                                                it.copy(offset = correctedOffset)
                                                                            } else {
                                                                                it
                                                                            }
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        activeSealPointerCount = pressedCount
                                                    }
                                                }
                                            }
                                            launch {
                                                detectTapGestures(
                                                    onTap = {
                                                        viewModel.setSelectedSealId(
                                                            if (selectedSealId == seal.id) {
                                                                null
                                                            } else {
                                                                seal.id
                                                            }
                                                        )
                                                    }
                                                )
                                            }
                                            launch {
                                                detectTransformGestures { _, pan, zoom, rotationChange ->
                                                    if (postcardPreviewSize == IntSize.Zero) {
                                                        return@detectTransformGestures
                                                    }

                                                    val currentSeal =
                                                        latestPhotoSeals.find {
                                                            it.id == seal.id
                                                        } ?: return@detectTransformGestures

                                                    if (sealGestureSnapshotPending) {
                                                        viewModel.recordSealSnapshotForUndo()
                                                        sealGestureSnapshotPending = false
                                                    }

                                                    // 두 손가락 이상일 때는 중심점 이동(pan)으로
                                                    // 위치를 바꾸지 않는다 — 확대·축소·회전만 적용.
                                                    val isMultiTouch =
                                                        activeSealPointerCount >= 2

                                                    val newOffset =
                                                        if (isMultiTouch) {
                                                            currentSeal.offset
                                                        } else {
                                                            val currentSealSize =
                                                                sealSizes[seal.id]
                                                                    ?: IntSize.Zero

                                                            val oldOffset =
                                                                currentSeal.offset
                                                                    ?: centeredStickerOffset(
                                                                        postcardSize = postcardPreviewSize,
                                                                        stickerSize = currentSealSize
                                                                    )

                                                            val parentDelta =
                                                                localStickerDeltaToParent(
                                                                    localDelta = pan,
                                                                    rotationDegrees = currentSeal.rotationDegrees,
                                                                    flipHorizontal = false,
                                                                    flipVertical = false
                                                                )

                                                            // 제스처 도중에는 벽에 막힌 느낌을 주지 않도록
                                                            // 손가락을 그대로 따라간다 — 경계 보정은 제스처
                                                            // 종료 시점(위 포인터 카운트 감시부)에서 한다.
                                                            oldOffset + parentDelta
                                                        }

                                                    val newScale =
                                                        (currentSeal.scale * zoom)
                                                            .coerceIn(0.5f, 3f)

                                                    val newRotation =
                                                        normalizeStickerRotation(
                                                            currentSeal.rotationDegrees +
                                                                    rotationChange
                                                        )

                                                    viewModel.setSelectedSealId(seal.id)
                                                    viewModel.setPhotoSeals(
                                                        latestPhotoSeals.map {
                                                            if (it.id == seal.id) {
                                                                it.copy(
                                                                    offset = newOffset,
                                                                    scale = newScale,
                                                                    rotationDegrees = newRotation
                                                                )
                                                            } else {
                                                                it
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(sealVisualSize)
                                        .onSizeChanged { size ->
                                            sealSizes =
                                                sealSizes +
                                                        (seal.id to size)
                                        }
                                        .then(
                                            if (isSealVisuallySelected) {
                                                val selectionShape =
                                                    when (seal.type) {
                                                        SealType.CIRCLE_POSTMARK,
                                                        SealType.STAR,
                                                        SealType.DOG_PAW,
                                                        SealType.PIGEON_TRACK,
                                                        SealType.HEART,
                                                        SealType.STAR_STAMP -> CircleShape
                                                        SealType.WAVE_CANCEL,
                                                        SealType.AIR_MAIL ->
                                                            RoundedCornerShape(8.dp)
                                                    }

                                                Modifier.border(
                                                    width = 2.dp,
                                                    color = GraphiteAccent,
                                                    shape = selectionShape
                                                )
                                            } else {
                                                Modifier
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SealPreviewContent(
                                        type = seal.type,
                                        color = Color(seal.colorArgb),
                                        capturedAtMillis = pc.capturedAt,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        // 낙서는 사진·스티커·도장보다 항상 위에 그린다 — 이 Box의
                        // 마지막 자식이라 z-order상 최상단이다. pointerInput 블록
                        // 안에서 return하는 것만으로는 이 Canvas가 다른 탭에서도
                        // 터치를 가로채는 걸 막지 못했다(스티커·도장 드래그·삭제
                        // 버튼이 전부 막히는 회귀가 실기기에서 확인됨) — 그래서
                        // 낙서 탭이 아닐 때는 pointerInput 자체를 아예 붙이지
                        // 않는다(Modifier.then으로 조건부 부착).
                        val isDoodleInputActive =
                            customizationPagerState.currentPage ==
                            DOODLE_TAB_PAGE_INDEX &&
                            !isFocusPreviewMode

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (isDoodleInputActive) {
                                        Modifier.pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down =
                                            awaitFirstDown(
                                                requireUnconsumed = false
                                            )
                                        down.consume()

                                        if (latestDoodleTool == DoodleTool.ERASER) {
                                            var erasedAnyInThisGesture = false

                                            fun eraseAt(position: Offset) {
                                                val touchPoint =
                                                    normalizedDoodlePoint(
                                                        position,
                                                        postcardPreviewSize
                                                    ) ?: return

                                                val remaining =
                                                    latestDoodleStrokes.filterNot { stroke ->
                                                        doodleEraserHitsStroke(
                                                            touchPoint = touchPoint,
                                                            stroke = stroke,
                                                            eraserWidth = latestDoodleWidth
                                                        )
                                                    }

                                                if (remaining.size != latestDoodleStrokes.size) {
                                                    if (!erasedAnyInThisGesture) {
                                                        viewModel.recordDoodleSnapshotForUndo()
                                                        erasedAnyInThisGesture = true
                                                    }
                                                    viewModel.setDoodleStrokes(remaining)
                                                }
                                            }

                                            eraseAt(down.position)

                                            var keepGoing = true
                                            while (keepGoing) {
                                                val event = awaitPointerEvent()
                                                val change =
                                                    event.changes.firstOrNull {
                                                        it.id == down.id
                                                    }
                                                if (change != null && change.pressed) {
                                                    change.consume()
                                                    eraseAt(change.position)
                                                }
                                                keepGoing =
                                                    event.changes.any { it.pressed }
                                            }
                                        } else {
                                            var lastRawPosition = down.position
                                            val firstPoint =
                                                normalizedDoodlePoint(
                                                    down.position,
                                                    postcardPreviewSize
                                                )
                                            currentDoodleStrokePoints =
                                                if (firstPoint != null) {
                                                    listOf(firstPoint)
                                                } else {
                                                    emptyList()
                                                }
                                            isDrawingDoodle = true

                                            // 그리는 도중 탭이 바뀌거나 화면을 벗어나면 이
                                            // pointerInput 코루틴이 취소되는데, finally가 없으면
                                            // isDrawingDoodle/currentDoodleStrokePoints가 남아
                                            // 다음 낙서 탭 재진입 시 미완성 선이 잠깐 보일 수 있다.
                                            try {
                                                // 1단계: 자유곡선인지 직선인지 정한다.
                                                // 손가락을 가만히 두면 포인터 이벤트가 오지 않으므로
                                                // 길게 누르기는 타임아웃으로만 판정할 수 있다. 시간 안에
                                                // touchSlop을 넘게 움직이면 자유곡선, 넘지 않은 채로
                                                // 시간이 지나면 직선이다. 판정 기준은 Android 기본
                                                // 제스처 값을 그대로 쓴다(마법 숫자를 새로 만들지 않는다).
                                                // 한 번 정해진 모드는 손가락을 뗄 때까지 바뀌지 않는다.
                                                val slop = viewConfiguration.touchSlop
                                                var freeDrawResumeAt: Offset? = null
                                                var allPointersUp = false

                                                val decidedBeforeHold =
                                                    withTimeoutOrNull(
                                                        viewConfiguration.longPressTimeoutMillis
                                                    ) {
                                                        while (true) {
                                                            val event = awaitPointerEvent()
                                                            val change =
                                                                event.changes.firstOrNull {
                                                                    it.id == down.id
                                                                }

                                                            if (change != null && change.pressed) {
                                                                change.consume()

                                                                val dx =
                                                                    change.position.x -
                                                                            down.position.x
                                                                val dy =
                                                                    change.position.y -
                                                                            down.position.y

                                                                if (
                                                                    dx * dx + dy * dy >
                                                                    slop * slop
                                                                ) {
                                                                    freeDrawResumeAt =
                                                                        change.position
                                                                    return@withTimeoutOrNull Unit
                                                                }
                                                            }

                                                            if (event.changes.none { it.pressed }) {
                                                                allPointersUp = true
                                                                return@withTimeoutOrNull Unit
                                                            }
                                                        }
                                                    }

                                                val straightLineMode =
                                                    decidedBeforeHold == null
                                                var doodleLineSnapDirection =
                                                    DoodleLineSnapDirection.NONE

                                                if (!allPointersUp) {
                                                    if (straightLineMode) {
                                                        // 직선 상태로 처음 들어간 순간에만 한 번 알린다.
                                                        hapticFeedback.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                    } else {
                                                        // slop을 넘은 그 지점부터 자유곡선을 이어 그린다.
                                                        freeDrawResumeAt?.let { resumePosition ->
                                                            normalizedDoodlePoint(
                                                                resumePosition,
                                                                postcardPreviewSize
                                                            )?.let { resumePoint ->
                                                                currentDoodleStrokePoints =
                                                                    currentDoodleStrokePoints +
                                                                            resumePoint
                                                            }
                                                            lastRawPosition = resumePosition
                                                        }
                                                    }

                                                    var keepGoing = true
                                                    while (keepGoing) {
                                                        val event = awaitPointerEvent()
                                                        val change =
                                                            event.changes.firstOrNull {
                                                                it.id == down.id
                                                            }
                                                        if (change != null && change.pressed) {
                                                            change.consume()

                                                            if (straightLineMode) {
                                                                // 시작점은 고정하고 끝점만 따라간다 —
                                                                // 손가락이 굽은 경로로 지나가도
                                                                // 중간 경로는 남기지 않는다. 수평·수직
                                                                // 근접 각도는 화면 raw 좌표 기준으로
                                                                // 판정한 뒤 정규화 직전에 보정한다.
                                                                val previousSnapDirection =
                                                                    doodleLineSnapDirection
                                                                doodleLineSnapDirection =
                                                                    resolveDoodleLineSnapDirection(
                                                                        current = doodleLineSnapDirection,
                                                                        dx = change.position.x -
                                                                                down.position.x,
                                                                        dy = change.position.y -
                                                                                down.position.y,
                                                                        minDistancePx = slop
                                                                    )

                                                                if (
                                                                    doodleLineSnapDirection !=
                                                                    DoodleLineSnapDirection.NONE &&
                                                                    doodleLineSnapDirection !=
                                                                    previousSnapDirection
                                                                ) {
                                                                    // 수평·수직에 처음 붙는 순간에만 알린다 —
                                                                    // 직선 모드 진입 진동과는 구분된다.
                                                                    hapticFeedback.performHapticFeedback(
                                                                        HapticFeedbackType.TextHandleMove
                                                                    )
                                                                }

                                                                val snappedRawEndpoint =
                                                                    snappedDoodleLineEndpoint(
                                                                        direction = doodleLineSnapDirection,
                                                                        start = down.position,
                                                                        current = change.position
                                                                    )

                                                                val endPoint =
                                                                    normalizedDoodlePoint(
                                                                        snappedRawEndpoint,
                                                                        postcardPreviewSize
                                                                    )
                                                                if (
                                                                    firstPoint != null &&
                                                                    endPoint != null
                                                                ) {
                                                                    currentDoodleStrokePoints =
                                                                        listOf(
                                                                            firstPoint,
                                                                            endPoint
                                                                        )
                                                                }
                                                            } else {
                                                                val dx =
                                                                    change.position.x -
                                                                            lastRawPosition.x
                                                                val dy =
                                                                    change.position.y -
                                                                            lastRawPosition.y

                                                                if (
                                                                    dx * dx + dy * dy >=
                                                                    MIN_DOODLE_POINT_SPACING_PX *
                                                                    MIN_DOODLE_POINT_SPACING_PX
                                                                ) {
                                                                    val point =
                                                                        normalizedDoodlePoint(
                                                                            change.position,
                                                                            postcardPreviewSize
                                                                        )
                                                                    if (point != null) {
                                                                        currentDoodleStrokePoints =
                                                                            currentDoodleStrokePoints + point
                                                                    }
                                                                    lastRawPosition = change.position
                                                                }
                                                            }
                                                        }
                                                        keepGoing =
                                                            event.changes.any { it.pressed }
                                                    }
                                                }

                                                // 직선인데 시작점에서 사실상 움직이지 않았다면 길이 0짜리
                                                // 획 대신 짧은 탭과 같은 점 하나로 남긴다.
                                                val confirmedPoints =
                                                    currentDoodleStrokePoints.let { points ->
                                                        if (
                                                            points.size == 2 &&
                                                            points[0] == points[1]
                                                        ) {
                                                            listOf(points[0])
                                                        } else {
                                                            points
                                                        }
                                                    }

                                                if (confirmedPoints.isNotEmpty()) {
                                                    viewModel.recordDoodleSnapshotForUndo()
                                                    viewModel.setDoodleStrokes(
                                                        latestDoodleStrokes + DoodleStroke(
                                                            points = confirmedPoints,
                                                            colorArgb = latestDoodleColorArgb,
                                                            width = latestDoodleWidth,
                                                            tool = latestDoodleTool
                                                        )
                                                    )
                                                }
                                            } finally {
                                                currentDoodleStrokePoints = emptyList()
                                                isDrawingDoodle = false
                                            }
                                        }
                                    }
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            val visibleStrokes =
                                if (
                                    isDrawingDoodle &&
                                    currentDoodleStrokePoints.isNotEmpty()
                                ) {
                                    doodleStrokes + DoodleStroke(
                                        points = currentDoodleStrokePoints,
                                        colorArgb = doodleColorArgb,
                                        width = doodleWidth,
                                        tool = doodleTool
                                    )
                                } else {
                                    doodleStrokes
                                }

                            drawIntoCanvas { canvas ->
                                PostcardRenderSpec.drawDoodleStrokes(
                                    canvas = canvas.nativeCanvas,
                                    strokes = visibleStrokes,
                                    targetSize = size.width
                                )
                            }
                        }
                    }
                }
            }

            if (!isFocusPreviewMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            postcard?.let { pc ->

                val selectedSticker =
                    photoStickers.find {
                        it.id == selectedStickerId
                    }
                val selectedStickerIndex =
                    photoStickers.indexOfFirst {
                        it.id == selectedStickerId
                    }
                val canMoveSelectedStickerForward =
                    selectedStickerIndex != -1 &&
                            selectedStickerIndex < photoStickers.lastIndex
                val canMoveSelectedStickerBackward =
                    selectedStickerIndex > 0

                if (
                    selectedSticker != null &&
                    customizationPagerState.currentPage ==
                    STICKER_TAB_PAGE_INDEX
                ) {
                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    StickerEditModeToolbar(
                        sticker = selectedSticker,
                        editMode = resolvedStickerEditMode,
                        onModeSelected = { mode ->
                            stickerEditMode = mode
                            stickerEditModeOwnerId = selectedStickerId
                        },
                        isRemovingBackground = isRemovingBackground,
                        onToggleBackgroundRemoval = {
                            if (selectedSticker.isBackgroundRemoved) {
                                viewModel.recordStickerSnapshotForUndo()
                                viewModel.setPhotoStickers(
                                    photoStickers.map {
                                        if (it.id == selectedSticker.id) {
                                            it.copy(
                                                displayedUri = it.originalUri,
                                                isBackgroundRemoved = false
                                            )
                                        } else {
                                            it
                                        }
                                    }
                                )
                                backgroundRemovalError = null
                            } else {
                                backgroundRemovalError = null
                                val removedBgUri =
                                    selectedSticker.removedBgUri
                                if (removedBgUri != null) {
                                    viewModel.recordStickerSnapshotForUndo()
                                    viewModel.setPhotoStickers(
                                        photoStickers.map {
                                            if (it.id == selectedSticker.id) {
                                                it.copy(
                                                    displayedUri = removedBgUri,
                                                    isBackgroundRemoved = true
                                                )
                                            } else {
                                                it
                                            }
                                        }
                                    )
                                } else {
                                    viewModel.removeStickerBackground(
                                        stickerId = selectedSticker.id,
                                        sourceUri = selectedSticker.originalUri
                                    )
                                }
                            }
                        },
                        onToggleFlipHorizontal = {
                            viewModel.recordStickerSnapshotForUndo()
                            viewModel.setPhotoStickers(
                                photoStickers.map {
                                    if (it.id == selectedSticker.id) {
                                        it.copy(
                                            flipHorizontal = !it.flipHorizontal
                                        )
                                    } else {
                                        it
                                    }
                                }
                            )
                        },
                        onToggleFlipVertical = {
                            viewModel.recordStickerSnapshotForUndo()
                            viewModel.setPhotoStickers(
                                photoStickers.map {
                                    if (it.id == selectedSticker.id) {
                                        it.copy(
                                            flipVertical = !it.flipVertical
                                        )
                                    } else {
                                        it
                                    }
                                }
                            )
                        },
                        canMoveForward = canMoveSelectedStickerForward,
                        canMoveBackward = canMoveSelectedStickerBackward,
                        onMoveForward = {
                            viewModel.moveStickerForward(selectedSticker.id)
                        },
                        onMoveBackward = {
                            viewModel.moveStickerBackward(selectedSticker.id)
                        },
                        enabled = controlsEnabled,
                        modifier = Modifier.fillMaxWidth(0.92f)
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                HorizontalPager(
                    state = customizationPagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalAlignment = Alignment.Top
                ) { page ->
                    when (page) {
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth(0.92f)
                            ) {
                    PostcardLayoutPicker(
                        selectedLayout =
                            selectedLayout,
                        onLayoutSelected = { layout ->
                            viewModel.updateLayoutStyle(
                                layout.name
                            )
                        },
                        onUndoPhotoTransform = {
                            viewModel.undoPhotoTransformChange()
                        },
                        onRedoPhotoTransform = {
                            viewModel.redoPhotoTransformChange()
                        },
                        canUndoPhotoTransform = canUndoPhotoTransform,
                        canRedoPhotoTransform = canRedoPhotoTransform,
                        enabled = controlsEnabled,
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClick = {
                                    templatesExpanded = !templatesExpanded
                                }
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "템플릿",
                            color = BrutalBlack,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Icon(
                            imageVector =
                                if (templatesExpanded) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                            contentDescription =
                                if (templatesExpanded) {
                                    "템플릿 영역 접기"
                                } else {
                                    "템플릿 영역 펼치기"
                                },
                            tint = BrutalBlack
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    if (templatesExpanded) {
                    postcard?.let { currentPostcardForTemplates ->
                        val templatePreviewBitmap =
                            rememberTemplatePreviewBitmap(
                                currentPostcardForTemplates.imagePath
                            )

                        PostcardTemplateSection(
                            title = "추천 템플릿",
                            templates = BuiltInTemplates.all,
                            sourceBitmap = templatePreviewBitmap,
                            selectedTemplateId = effectiveSelectedTemplateId,
                            onSelect = { template ->
                                viewModel.applyTemplate(template)
                            },
                            enabled = controlsEnabled,
                            canUndo = canUndoTemplateStyle,
                            canRedo = canRedoTemplateStyle,
                            onUndo = { viewModel.undoTemplateStyleChange() },
                            onRedo = { viewModel.redoTemplateStyleChange() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        PostcardTemplateSection(
                            title = "내 템플릿",
                            templates = userTemplates,
                            sourceBitmap = templatePreviewBitmap,
                            selectedTemplateId = effectiveSelectedTemplateId,
                            onSelect = { template ->
                                viewModel.applyTemplate(template)
                            },
                            enabled = controlsEnabled,
                            modifier = Modifier.fillMaxWidth(),
                            onRequestRename = { template ->
                                templatePendingRename = template
                                renameTemplateNameInput = template.name
                            },
                            onRequestOverwrite = { template ->
                                templatePendingOverwrite = template
                            },
                            onRequestDelete = { template ->
                                templatePendingDelete = template
                            },
                            leadingContent = {
                                Column(
                                    modifier = Modifier
                                        .width(84.dp)
                                        .clickable(
                                            enabled = controlsEnabled,
                                            onClick = {
                                                saveTemplateNameInput =
                                                    viewModel.suggestNewTemplateName()
                                                showSaveTemplateDialog = true
                                            }
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(PaperField)
                                            .border(
                                                width = 1.dp,
                                                color = PaperDivider,
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+",
                                            color = SunsetGold,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "현재 꾸밈 저장",
                                        color = BrutalBlack,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        modifier = Modifier.widthIn(max = 68.dp)
                                    )
                                }
                            }
                        )

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )
                    }
                    }

                    if (
                        selectedLayout ==
                        PostcardLayoutStyle.POLAROID
                    ) {
                        EditorPercentSlider(
                            label = "사진 크기",
                            percent =
                                polaroidPhotoScalePercent,
                            minPercent = 75,
                            maxPercent = 105,
                            enabled = controlsEnabled,
                            onPreviewPercentChanged = { percent ->
                                if (!polaroidPhotoScaleDragSnapshotTaken) {
                                    viewModel.recordPhotoTransformSnapshotForUndo()
                                    polaroidPhotoScaleDragSnapshotTaken = true
                                }
                                viewModel
                                    .setPolaroidPhotoScalePreview(
                                        percent / 100f
                                    )
                            },
                            onPercentConfirmed = { percent ->
                                if (!polaroidPhotoScaleDragSnapshotTaken) {
                                    viewModel.recordPhotoTransformSnapshotForUndo()
                                }
                                polaroidPhotoScaleDragSnapshotTaken = false
                                viewModel
                                    .savePolaroidPhotoScale(
                                        percent / 100f
                                    )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // TAPED_FILM·LETTER는 layoutFor()에서 0.85~1.15로
                        // 클램프하므로 슬라이더 범위도 동일하게 맞춘다.
                        val isNarrowScaleRange =
                            selectedLayout ==
                                    PostcardLayoutStyle.TAPED_FILM ||
                                    selectedLayout ==
                                    PostcardLayoutStyle.LETTER

                        EditorPercentSlider(
                            label = "사진 크기",
                            percent =
                                stampPhotoScalePercent,
                            minPercent = if (isNarrowScaleRange) 85 else 70,
                            maxPercent = if (isNarrowScaleRange) 115 else 130,
                            enabled = controlsEnabled,
                            onPreviewPercentChanged = { percent ->
                                if (!stampPhotoScaleDragSnapshotTaken) {
                                    viewModel.recordPhotoTransformSnapshotForUndo()
                                    stampPhotoScaleDragSnapshotTaken = true
                                }
                                viewModel
                                    .setStampPhotoScalePreview(
                                        percent / 100f
                                    )
                            },
                            onPercentConfirmed = { percent ->
                                if (!stampPhotoScaleDragSnapshotTaken) {
                                    viewModel.recordPhotoTransformSnapshotForUndo()
                                }
                                stampPhotoScaleDragSnapshotTaken = false
                                viewModel
                                    .saveStampPhotoScale(
                                        percent / 100f
                                    )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    EditorPercentSlider(
                        label = "가장자리 흐림",
                        percent =
                            photoEdgeBlurPercent,
                        minPercent = 0,
                        maxPercent = 100,
                        enabled = controlsEnabled,
                        onPreviewPercentChanged = { percent ->
                            viewModel
                                .setPhotoEdgeBlurPreview(
                                    percent / 100f
                                )
                        },
                        onPercentConfirmed = { percent ->
                            viewModel
                                .savePhotoEdgeBlur(
                                    percent / 100f
                                )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    EditorSecondaryButton(
                        text = "사진 바꾸기",
                        icon = Icons.Default.Edit,
                        enabled = controlsEnabled,
                        onClick = {
                            showPhotoSourceMenu = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                            }
                            }
                        }

                        1 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth(0.92f)
                            ) {
                    PostcardBackgroundColorPicker(
                        selectedColorArgb =
                            pc.backgroundColorArgb,
                        enabled = controlsEnabled,
                        onColorSelected = { colorArgb ->
                            viewModel.updateBackgroundColor(
                                colorArgb
                            )
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    TextButton(
                        onClick = {
                            customColorDrawerExpanded =
                                !customColorDrawerExpanded
                        },
                        enabled = controlsEnabled,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = BrutalBlack
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(
                            modifier = Modifier.size(6.dp)
                        )

                        Text(
                            text =
                                if (customColorDrawerExpanded) {
                                    "직접 고르기 닫기"
                                } else {
                                    "직접 고르기"
                                },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    AnimatedVisibility(
                        visible = customColorDrawerExpanded,
                        enter =
                            expandVertically() + fadeIn(),
                        exit =
                            shrinkVertically() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            PostcardCustomColorPicker(
                                selectedColorArgb =
                                    pc.backgroundColorArgb,
                                enabled = controlsEnabled,
                                onColorSelected = { colorArgb ->
                                    viewModel.updateBackgroundColor(
                                        colorArgb
                                    )
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    EditorSecondaryButton(
                        text =
                            if (
                                photoColorExtractionState is
                                        PhotoColorExtractionState.Extracting
                            ) {
                                "색 추출 중..."
                            } else {
                                "사진에서 색 가져오기"
                            },
                        icon = Icons.Default.Palette,
                        enabled =
                            controlsEnabled &&
                                    photoColorExtractionState !is
                                            PhotoColorExtractionState.Extracting,
                        onClick = {
                            viewModel
                                .extractBackgroundColorsFromPhoto()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    (
                            photoColorExtractionState as?
                                    PhotoColorExtractionState.Success
                            )?.let { successState ->
                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            successState.colors.forEach { extractedColor ->
                                val extractedSelected =
                                    pc.backgroundColorArgb ==
                                            extractedColor.colorArgb

                                Column(
                                    horizontalAlignment =
                                        Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable(
                                        enabled = controlsEnabled
                                    ) {
                                        viewModel
                                            .updateBackgroundColor(
                                                extractedColor
                                                    .colorArgb
                                            )
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier.size(44.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    color =
                                                        Color(
                                                            extractedColor
                                                                .colorArgb
                                                        ),
                                                    shape = CircleShape
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = SurfaceGray,
                                                    shape = CircleShape
                                                )
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(
                                                color =
                                                    if (extractedSelected) {
                                                        SunsetGold
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }

                    (
                            photoColorExtractionState as?
                                    PhotoColorExtractionState.Error
                            )?.let { errorState ->
                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Text(
                            text = errorState.message,
                            color = GalleryDangerRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    PostcardBackgroundPatternPicker(
                        selectedColorArgb =
                            pc.backgroundColorArgb,
                        selectedPattern = selectedPattern,
                        enabled = controlsEnabled,
                        onPatternSelected = { pattern ->
                            viewModel
                                .updateBackgroundPattern(
                                    pattern.name
                                )
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    EditorPercentSlider(
                        label = "패턴 세기",
                        percent =
                            backgroundPatternDensityPercent,
                        minPercent = 70,
                        maxPercent = 150,
                        enabled = controlsEnabled,
                        onPreviewPercentChanged = { percent ->
                            viewModel
                                .setBackgroundPatternDensityPreview(
                                    percent / 100f
                                )
                        },
                        onPercentConfirmed = { percent ->
                            viewModel
                                .saveBackgroundPatternDensity(
                                    percent / 100f
                                )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                            }
                            }
                        }

                        2 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth(0.92f)
                            ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text =
                                if (pc.message.isBlank()) {
                                    "글귀가 비어 있어."
                                } else {
                                    pc.message
                                },
                            color =
                                if (pc.message.isBlank()) {
                                    GraphiteAccent
                                } else {
                                    BrutalBlack
                                },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(
                            onClick = {
                                messageDraft = pc.message
                                showMessageDialog = true
                            },
                            enabled = controlsEnabled,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = BrutalBlack
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(
                                modifier = Modifier.size(6.dp)
                            )

                            Text(
                                text = "편집",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Text(
                        text = "조절 대상",
                        color = BrutalBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            TextScaleTarget.Message to "글귀",
                            TextScaleTarget.Date to "날짜"
                        ).forEach { (target, targetLabel) ->
                            val targetSelected =
                                textScaleTarget == target

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (targetSelected) {
                                            SunsetGold.copy(alpha = 0.16f)
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .clickable(
                                        enabled = controlsEnabled
                                    ) {
                                        textScaleTarget = target
                                    }
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 7.dp
                                    )
                            ) {
                                Text(
                                    text = targetLabel,
                                    color =
                                        if (targetSelected) {
                                            SunsetGold
                                        } else {
                                            GraphiteAccent
                                        },
                                    fontSize = 14.sp,
                                    fontWeight =
                                        if (targetSelected) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Medium
                                        }
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    val messageTargetSelected =
                        textScaleTarget == TextScaleTarget.Message

                    EditorPercentSlider(
                        label = "크기",
                        percent =
                            if (messageTargetSelected) {
                                messageTextScalePercent
                            } else {
                                dateTextScalePercent
                            },
                        minPercent = 60,
                        maxPercent =
                            if (messageTargetSelected) 140 else 180,
                        enabled = controlsEnabled,
                        onPreviewPercentChanged = { percent ->
                            if (messageTargetSelected) {
                                viewModel.setMessageTextScalePreview(
                                    percent / 100f
                                )
                            } else {
                                viewModel.setDateTextScalePreview(
                                    percent / 100f
                                )
                            }
                        },
                        onPercentConfirmed = { percent ->
                            if (messageTargetSelected) {
                                viewModel.saveMessageTextScale(
                                    percent / 100f
                                )
                            } else {
                                viewModel.saveDateTextScale(
                                    percent / 100f
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                            }
                            }
                        }

                        3 -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                PhotoStickerPickerPanel(
                                    photoStickers = photoStickers,
                                    selectedStickerId = selectedStickerId,
                                    backgroundRemovalError = backgroundRemovalError,
                                    onSelectSticker = { id ->
                                        viewModel.setSelectedStickerId(id)
                                    },
                                    onAddFromGallery = { uri ->
                                        viewModel.recordStickerSnapshotForUndo()
                                        val newSticker = PhotoStickerItem(
                                            originalUri = uri,
                                            displayedUri = uri
                                        )
                                        viewModel.setPhotoStickers(photoStickers + newSticker)
                                        viewModel.setSelectedStickerId(newSticker.id)
                                        backgroundRemovalError = null
                                        viewModel.resetStickerBackgroundRemovalState()
                                    },
                                    onAddFromFile = { uri ->
                                        viewModel.recordStickerSnapshotForUndo()
                                        val newSticker = PhotoStickerItem(
                                            originalUri = uri,
                                            displayedUri = uri
                                        )
                                        viewModel.setPhotoStickers(photoStickers + newSticker)
                                        viewModel.setSelectedStickerId(newSticker.id)
                                        backgroundRemovalError = null
                                        viewModel.resetStickerBackgroundRemovalState()
                                    },
                                    onAddFromCamera = { captureFile ->
                                        backgroundRemovalError = null
                                        viewModel.resetStickerBackgroundRemovalState()
                                        viewModel.addCameraPhotoSticker(
                                            postcardId,
                                            captureFile
                                        )
                                    },
                                    onDeleteSticker = { id ->
                                        val sticker = photoStickers.find { it.id == id }
                                        val removedBgUriToDelete = sticker?.removedBgUri

                                        viewModel.recordStickerSnapshotForUndo()

                                        val remaining = photoStickers.filter { it.id != id }
                                        viewModel.setPhotoStickers(remaining)

                                        removedBgUriToDelete?.let { uri ->
                                            viewModel.deleteStickerCacheUri(uri)
                                        }
                                        sticker?.originalUri?.let { uri ->
                                            viewModel.deleteStickerOriginalIfUnreferenced(
                                                uri,
                                                remaining
                                            )
                                        }
                                        stickerSizes = stickerSizes - id
                                        if (selectedStickerId == id) {
                                            viewModel.setSelectedStickerId(
                                                remaining.lastOrNull()?.id
                                            )
                                        }
                                    },
                                    onDuplicateSticker = { id ->
                                        viewModel.duplicateSticker(id)
                                    },
                                    onUndoSticker = {
                                        viewModel.undoStickerChange()
                                    },
                                    onRedoSticker = {
                                        viewModel.redoStickerChange()
                                    },
                                    canUndoSticker = canUndoSticker,
                                    canRedoSticker = canRedoSticker,
                                    enabled = controlsEnabled,
                                    modifier = Modifier.fillMaxWidth(0.92f)
                                )
                            }
                        }

                        4 -> {
                            val selectedSealForBoundsCheck =
                                photoSeals.find { it.id == selectedSealId }
                            val selectedSealOutOfBounds =
                                selectedSealForBoundsCheck?.offset != null &&
                                        postcardPreviewSize != IntSize.Zero &&
                                        sealSizes[selectedSealForBoundsCheck.id]?.let { sealSize ->
                                            correctSealOffsetForMinimumVisibility(
                                                offset = selectedSealForBoundsCheck.offset,
                                                sealSize = sealSize,
                                                rotationDegrees = selectedSealForBoundsCheck.rotationDegrees,
                                                postcardSize = postcardPreviewSize,
                                                minimumVisibleEdgePx = sealMinVisibleEdgePx
                                            ) != selectedSealForBoundsCheck.offset
                                        } == true

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                SealPickerPanel(
                                    photoSeals = photoSeals,
                                    selectedSealId = selectedSealId,
                                    isSelectedSealOutOfBounds = selectedSealOutOfBounds,
                                    onRestoreSealPosition = {
                                        val target = selectedSealForBoundsCheck
                                        val targetSize =
                                            target?.let { sealSizes[it.id] }

                                        if (target != null && targetSize != null) {
                                            viewModel.recordSealSnapshotForUndo()
                                            val restoredOffset =
                                                centeredStickerOffset(
                                                    postcardSize = postcardPreviewSize,
                                                    stickerSize = targetSize
                                                )
                                            viewModel.setPhotoSeals(
                                                photoSeals.map {
                                                    if (it.id == target.id) {
                                                        it.copy(offset = restoredOffset)
                                                    } else {
                                                        it
                                                    }
                                                }
                                            )
                                        }
                                    },
                                    onSelectSeal = { id ->
                                        viewModel.setSelectedSealId(
                                            if (selectedSealId == id) {
                                                null
                                            } else {
                                                id
                                            }
                                        )
                                    },
                                    onAddSeal = { type ->
                                        if (photoSeals.size >= MAX_SEAL_COUNT) {
                                            Toast.makeText(
                                                context,
                                                "도장은 엽서 한 장에 최대 ${MAX_SEAL_COUNT}개까지만 붙일 수 있어.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            viewModel.recordSealSnapshotForUndo()
                                            val newSeal =
                                                PostcardSealItem(
                                                    type = type,
                                                    scale = type.defaultScale
                                                )
                                            viewModel.setPhotoSeals(
                                                photoSeals + newSeal
                                            )
                                            viewModel.setSelectedSealId(
                                                newSeal.id
                                            )
                                        }
                                    },
                                    onDeleteSeal = { id ->
                                        viewModel.recordSealSnapshotForUndo()
                                        val remaining =
                                            photoSeals.filter {
                                                it.id != id
                                            }
                                        viewModel.setPhotoSeals(remaining)
                                        sealSizes = sealSizes - id
                                        if (selectedSealId == id) {
                                            viewModel.setSelectedSealId(
                                                remaining.lastOrNull()?.id
                                            )
                                        }
                                    },
                                    onColorSelected = { id, colorArgb ->
                                        viewModel.recordSealSnapshotForUndo()
                                        viewModel.setPhotoSeals(
                                            photoSeals.map {
                                                if (it.id == id) {
                                                    it.copy(colorArgb = colorArgb)
                                                } else {
                                                    it
                                                }
                                            }
                                        )
                                    },
                                    onUndoSeal = {
                                        viewModel.undoSealChange()
                                    },
                                    onRedoSeal = {
                                        viewModel.redoSealChange()
                                    },
                                    canUndoSeal = canUndoSeal,
                                    canRedoSeal = canRedoSeal,
                                    enabled = controlsEnabled,
                                    modifier = Modifier.fillMaxWidth(0.92f)
                                )
                            }
                        }

                        5 -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                DoodlePanel(
                                    doodleTool = doodleTool,
                                    onToolSelected = { doodleTool = it },
                                    doodleColorArgb = doodleColorArgb,
                                    onColorSelected = { doodleColorArgb = it },
                                    doodleWidth = doodleWidth,
                                    onWidthSelected = { doodleWidth = it },
                                    strokeCount = doodleStrokes.size,
                                    onClearAll = {
                                        if (doodleStrokes.isNotEmpty()) {
                                            viewModel.recordDoodleSnapshotForUndo()
                                            viewModel.setDoodleStrokes(emptyList())
                                        }
                                    },
                                    onUndo = {
                                        viewModel.undoDoodleChange()
                                    },
                                    onRedo = {
                                        viewModel.redoDoodleChange()
                                    },
                                    canUndo = canUndoDoodle,
                                    canRedo = canRedoDoodle,
                                    enabled = controlsEnabled,
                                    modifier = Modifier.fillMaxWidth(0.92f)
                                )
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )
                if (
                    dateFormatUpdateState
                            is DateFormatUpdateState.Saving
                ) {
                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = BrutalBlack,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(22.dp)
                        )

                        Text(
                            text = "날짜 형식 저장 중...",
                            color = BrutalBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (
                    layoutUpdateState
                            is LayoutUpdateState.Saving
                ) {
                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = BrutalBlack,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(22.dp)
                        )

                        Text(
                            text = "레이아웃 저장 중...",
                            color = BrutalBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (
                    fontUpdateState
                            is FontUpdateState.Saving
                ) {
                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = BrutalBlack,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(22.dp)
                        )

                        Text(
                            text = "폰트 저장 중...",
                            color = BrutalBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (
                    backgroundUpdateState
                            is BackgroundUpdateState.Saving
                ) {
                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = BrutalBlack,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(22.dp)
                        )

                        Text(
                            text = "배경 저장 중...",
                            color = BrutalBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                run {
                    // "저장됨"은 잠깐 보였다 스스로 사라진다. 나머지 상태는 그대로 유지된다.
                    var showSavedBriefly by remember { mutableStateOf(false) }

                    LaunchedEffect(draftSaveStatus) {
                        if (draftSaveStatus is DraftSaveStatus.Saved) {
                            showSavedBriefly = true
                            delay(1500)
                            showSavedBriefly = false
                        } else {
                            showSavedBriefly = false
                        }
                    }

                    val draftSaveStatusText = when (draftSaveStatus) {
                        DraftSaveStatus.PendingChanges,
                        DraftSaveStatus.Saving -> "꾸미기 저장 중…"
                        DraftSaveStatus.Saved ->
                            "✓ 꾸미기 저장됨".takeIf { showSavedBriefly }
                        DraftSaveStatus.Failed -> "꾸미기를 저장하지 못했어요"
                        DraftSaveStatus.Idle -> null
                    }

                    AnimatedVisibility(
                        visible = draftSaveStatusText != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text = draftSaveStatusText.orEmpty(),
                                color =
                                    if (draftSaveStatus is DraftSaveStatus.Failed) {
                                        GalleryDangerRed
                                    } else {
                                        InkSecondary
                                    },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(96.dp)
                )
            }
            }
            }
        }

        if (!isFocusPreviewMode) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GalleryPaperWhite)
                    .padding(
                        horizontal = 8.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = navigateBackAfterPendingStyleSaves,
                    enabled = controlsEnabled
                ) {
                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로",
                        tint = BrutalBlack
                    )
                }

                Text(
                    text = "엽서 꾸미기",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrutalBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )

                IconButton(
                    onClick = {
                        viewModel.saveEditsAndClearDraft(
                            postcardId
                        )
                    },
                    enabled = controlsEnabled
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "스티커·도장 꾸미기 완료 저장",
                        tint = BrutalBlack
                    )
                }

                Box {
                    IconButton(
                        onClick = {
                            moreMenuExpanded = true
                        },
                        enabled = controlsEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "더보기",
                            tint = BrutalBlack
                        )
                    }

                    DropdownMenu(
                        expanded = moreMenuExpanded,
                        onDismissRequest = {
                            moreMenuExpanded = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(text = "공유")
                            },
                            leadingIcon = {
                                if (shareState is ShareState.Preparing) {
                                    CircularProgressIndicator(
                                        color = BrutalBlack,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = BrutalBlack
                                    )
                                }
                            },
                            enabled = controlsEnabled,
                            onClick = {
                                moreMenuExpanded = false
                                postcard?.let { pc ->
                                    if (postcardPreviewSize == IntSize.Zero) {
                                        Toast.makeText(
                                            context,
                                            "엽서를 준비하는 중이야. 잠시 후 다시 시도해줘.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@let
                                    }

                                    viewModel.sharePostcard(
                                        stickerOverlays =
                                            createStickerOverlaysForExport(
                                                photoStickers =
                                                    photoStickers,
                                                postcardSize =
                                                    postcardPreviewSize,
                                                stickerSizes =
                                                    stickerSizes,
                                                baseStickerPx =
                                                    baseStickerPx
                                            ),
                                        sealOverlays =
                                            createSealOverlaysForExport(
                                                photoSeals =
                                                    photoSeals,
                                                postcardSize =
                                                    postcardPreviewSize,
                                                sealSizes =
                                                    sealSizes,
                                                baseSealPx =
                                                    baseSealPx,
                                                minimumVisibleEdgePx =
                                                    sealMinVisibleEdgePx,
                                                capturedAtMillis =
                                                    pc.capturedAt
                                            )
                                    )
                                }
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(text = "파일 내보내기")
                            },
                            leadingIcon = {
                                if (exportState is ExportState.Exporting) {
                                    CircularProgressIndicator(
                                        color = BrutalCoral,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = BrutalCoral
                                    )
                                }
                            },
                            enabled = controlsEnabled,
                            onClick = {
                                moreMenuExpanded = false
                                postcard?.let { pc ->
                                    if (postcardPreviewSize == IntSize.Zero) {
                                        Toast.makeText(
                                            context,
                                            "엽서를 준비하는 중이야. 잠시 후 다시 시도해줘.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@let
                                    }

                                    viewModel.exportPostcardToGallery(
                                        stickerOverlays =
                                            createStickerOverlaysForExport(
                                                photoStickers =
                                                    photoStickers,
                                                postcardSize =
                                                    postcardPreviewSize,
                                                stickerSizes =
                                                    stickerSizes,
                                                baseStickerPx =
                                                    baseStickerPx
                                            ),
                                        sealOverlays =
                                            createSealOverlaysForExport(
                                                photoSeals =
                                                    photoSeals,
                                                postcardSize =
                                                    postcardPreviewSize,
                                                sealSizes =
                                                    sealSizes,
                                                baseSealPx =
                                                    baseSealPx,
                                                minimumVisibleEdgePx =
                                                    sealMinVisibleEdgePx,
                                                capturedAtMillis =
                                                    pc.capturedAt
                                            )
                                    )
                                }
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(text = "미리보기 크게 보기")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = null,
                                    tint = BrutalBlack
                                )
                            },
                            enabled = controlsEnabled,
                            onClick = {
                                moreMenuExpanded = false
                                showPhotoSourceMenu = false
                                isFocusPreviewMode = true
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(text = "💌 미래의 나에게 보내기")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = InkPrimary
                                )
                            },
                            enabled = controlsEnabled,
                            onClick = {
                                moreMenuExpanded = false
                                showFutureMailDatePicker = true
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "삭제",
                                    color = GalleryDangerRed
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = GalleryDangerRed
                                )
                            },
                            onClick = {
                                moreMenuExpanded = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = SurfaceGray, thickness = 1.dp)
        }
        }
    }

    if (isFocusPreviewMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            IconButton(
                onClick = {
                    isFocusPreviewMode = false
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                    .background(
                        color = PaperSurface.copy(alpha = 0.92f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = "집중 미리보기 종료",
                    tint = InkPrimary
                )
            }
        }
    }

    if (showPhotoSourceMenu) {
        PhotoSourceMenu(
            onDismiss = {
                showPhotoSourceMenu = false
            },
            onCameraSelected = {
                showPhotoSourceMenu = false
                launchPostcardCameraCapture()
            },
            onGallerySelected = {
                showPhotoSourceMenu = false
                postcardPhotoPicker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts
                            .PickVisualMedia
                            .ImageOnly
                    )
                )
            },
            onFileSelected = {
                showPhotoSourceMenu = false
                postcardFilePicker.launch(
                    arrayOf("image/*")
                )
            }
        )
    }

    if (showMessageDialog) {
        AlertDialog(
            onDismissRequest = {
                showMessageDialog = false
            },
            containerColor = PaperSurface,
            titleContentColor = InkPrimary,
            textContentColor = InkPrimary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "글귀 남기기",
                    color = InkPrimary,
                    fontSize = 18.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Text(
                        text =
                            "기억하고 싶은 말을 적어봐.",
                        color = InkSecondary,
                        fontSize = 14.sp,
                        modifier =
                            Modifier.padding(
                                bottom = 12.dp
                            )
                    )

                    OutlinedTextField(
                        value = messageDraft,
                        onValueChange = { newValue ->
                            if (newValue.length <= 120) {
                                messageDraft = newValue
                            }
                        },
                        label = {
                            Text("글귀")
                        },
                        placeholder = {
                            Text(
                                "오늘의 순간을 남겨봐"
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PaperField,
                            unfocusedContainerColor = PaperField,
                            disabledContainerColor =
                                PaperField.copy(alpha = 0.6f),
                            focusedBorderColor = SunsetGold,
                            unfocusedBorderColor = PaperDivider,
                            disabledBorderColor =
                                PaperDivider.copy(alpha = 0.6f),
                            focusedLabelColor = SunsetGold,
                            unfocusedLabelColor = InkSecondary,
                            focusedTextColor = InkPrimary,
                            unfocusedTextColor = InkPrimary,
                            focusedPlaceholderColor = InkSecondary,
                            unfocusedPlaceholderColor = InkSecondary,
                            cursorColor = SunsetGold
                        ),
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Text(
                        text =
                            "${messageDraft.length} / 120",
                        color = InkSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )

                    Text(
                        text =
                            "글귀를 비우고 저장하면 기존 글귀가 삭제돼.",
                        color = InkSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .background(
                                color =
                                    PaperTray.copy(alpha = 0.65f),
                                shape =
                                    RoundedCornerShape(
                                        12.dp
                                    )
                            )
                            .padding(10.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateMessage(
                            messageDraft
                        )

                        showMessageDialog = false
                    }
                ) {
                    Text(
                        text = "저장",
                        color = SunsetGold,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMessageDialog = false
                    }
                ) {
                    Text(
                        text = "취소",
                        color = InkSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    LaunchedEffect(templateSaveState) {
        if (templateSaveState is TemplateSaveState.Saved) {
            showSaveTemplateDialog = false
            viewModel.resetTemplateSaveState()
        }
    }

    if (showSaveTemplateDialog) {
        AlertDialog(
            onDismissRequest = {
                showSaveTemplateDialog = false
                viewModel.resetTemplateSaveState()
            },
            containerColor = PaperSurface,
            titleContentColor = InkPrimary,
            textContentColor = InkPrimary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "현재 꾸밈 저장",
                    color = InkPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Text(
                        text = "지금 이 엽서의 배경·레이아웃·크기 같은 꾸밈 방식만 템플릿으로 저장해. 사진과 글은 저장되지 않아.",
                        color = InkSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = saveTemplateNameInput,
                        onValueChange = { newValue ->
                            if (newValue.length <= 20) {
                                saveTemplateNameInput = newValue
                            }
                        },
                        label = {
                            Text("템플릿 이름")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PaperField,
                            unfocusedContainerColor = PaperField,
                            disabledContainerColor =
                                PaperField.copy(alpha = 0.6f),
                            focusedBorderColor = SunsetGold,
                            unfocusedBorderColor = PaperDivider,
                            disabledBorderColor =
                                PaperDivider.copy(alpha = 0.6f),
                            focusedLabelColor = SunsetGold,
                            unfocusedLabelColor = InkSecondary,
                            focusedTextColor = InkPrimary,
                            unfocusedTextColor = InkPrimary,
                            cursorColor = SunsetGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (
                        saveTemplateNameInput.isNotBlank() &&
                        viewModel.isTemplateNameDuplicate(saveTemplateNameInput)
                    ) {
                        Text(
                            text = "이미 같은 이름의 템플릿이 있어. 그래도 저장하면 따로 구분해서 보관돼.",
                            color = InkSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    val currentTemplateSaveState = templateSaveState
                    if (currentTemplateSaveState is TemplateSaveState.Error) {
                        Text(
                            text = currentTemplateSaveState.message,
                            color = GalleryDangerRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveCurrentStyleAsNewTemplate(
                            saveTemplateNameInput
                        )
                    },
                    enabled =
                        saveTemplateNameInput.trim().isNotBlank() &&
                                templateSaveState !is TemplateSaveState.Saving
                ) {
                    Text(
                        text = "저장",
                        color = SunsetGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveTemplateDialog = false
                        viewModel.resetTemplateSaveState()
                    }
                ) {
                    Text(
                        text = "취소",
                        color = InkSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    LaunchedEffect(templateManageState) {
        if (templateManageState is TemplateManageState.Success) {
            templatePendingRename = null
            templatePendingOverwrite = null
            templatePendingDelete = null
            viewModel.resetTemplateManageState()
        }
    }

    templatePendingRename?.let { templateToRename ->
        AlertDialog(
            onDismissRequest = {
                templatePendingRename = null
                viewModel.resetTemplateManageState()
            },
            containerColor = PaperSurface,
            titleContentColor = InkPrimary,
            textContentColor = InkPrimary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "템플릿 이름 변경",
                    color = InkPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameTemplateNameInput,
                        onValueChange = { newValue ->
                            if (newValue.length <= 20) {
                                renameTemplateNameInput = newValue
                            }
                        },
                        label = {
                            Text("템플릿 이름")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PaperField,
                            unfocusedContainerColor = PaperField,
                            disabledContainerColor =
                                PaperField.copy(alpha = 0.6f),
                            focusedBorderColor = SunsetGold,
                            unfocusedBorderColor = PaperDivider,
                            disabledBorderColor =
                                PaperDivider.copy(alpha = 0.6f),
                            focusedLabelColor = SunsetGold,
                            unfocusedLabelColor = InkSecondary,
                            focusedTextColor = InkPrimary,
                            unfocusedTextColor = InkPrimary,
                            cursorColor = SunsetGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val currentTemplateManageState = templateManageState
                    if (currentTemplateManageState is TemplateManageState.Error) {
                        Text(
                            text = currentTemplateManageState.message,
                            color = GalleryDangerRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameUserTemplate(
                            templateToRename.id,
                            renameTemplateNameInput
                        )
                    },
                    enabled =
                        renameTemplateNameInput.trim().isNotBlank() &&
                                templateManageState !is TemplateManageState.InProgress
                ) {
                    Text(
                        text = "저장",
                        color = SunsetGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        templatePendingRename = null
                        viewModel.resetTemplateManageState()
                    }
                ) {
                    Text(
                        text = "취소",
                        color = InkSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    templatePendingOverwrite?.let { templateToOverwrite ->
        AlertDialog(
            onDismissRequest = {
                templatePendingOverwrite = null
                viewModel.resetTemplateManageState()
            },
            containerColor = PaperSurface,
            titleContentColor = InkPrimary,
            textContentColor = InkPrimary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "현재 꾸밈으로 바꿀까?",
                    color = InkPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Text(
                        text =
                            "'${templateToOverwrite.name}' 템플릿을 지금 이 엽서의 꾸밈으로 덮어써. 이전 스타일은 되돌릴 수 없어.",
                        color = InkSecondary,
                        fontSize = 13.sp
                    )

                    val currentTemplateManageState = templateManageState
                    if (currentTemplateManageState is TemplateManageState.Error) {
                        Text(
                            text = currentTemplateManageState.message,
                            color = GalleryDangerRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.overwriteUserTemplateWithCurrentStyle(
                            templateToOverwrite.id
                        )
                    },
                    enabled = templateManageState !is TemplateManageState.InProgress
                ) {
                    Text(
                        text = "덮어쓰기",
                        color = SunsetGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        templatePendingOverwrite = null
                        viewModel.resetTemplateManageState()
                    }
                ) {
                    Text(
                        text = "취소",
                        color = InkSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    templatePendingDelete?.let { templateToDelete ->
        AlertDialog(
            onDismissRequest = {
                templatePendingDelete = null
                viewModel.resetTemplateManageState()
            },
            containerColor = PaperSurface,
            titleContentColor = InkPrimary,
            textContentColor = InkPrimary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "템플릿을 삭제할까?",
                    color = InkPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text =
                        "'${templateToDelete.name}' 템플릿을 삭제해. 이 템플릿을 과거에 적용했던 엽서들은 영향받지 않아.",
                    color = InkSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteUserTemplate(templateToDelete.id)
                    },
                    enabled = templateManageState !is TemplateManageState.InProgress
                ) {
                    Text(
                        text = "삭제",
                        color = GalleryDangerRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        templatePendingDelete = null
                        viewModel.resetTemplateManageState()
                    }
                ) {
                    Text(
                        text = "취소",
                        color = InkSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text(
                    text = "삭제하시겠어요?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text =
                        "이 우표를 삭제하면 복구할 수 없어요."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePostcard()
                        showDeleteDialog = false
                    }
                ) {
                    Text(
                        text = "삭제",
                        color = GalleryDangerRed,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("취소")
                }
            }
        )
    }

    if (showFutureMailDatePicker) {
        val nowMillis = remember { System.currentTimeMillis() }

        val tomorrowLocalStartOfDay = remember(nowMillis) {
            startOfDayMillis(nowMillis + 24L * 60 * 60 * 1000)
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis =
                localStartOfDayToMaterialDatePickerUtcMillis(tomorrowLocalStartOfDay),
            selectableDates = remember {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                        isSelectableFutureMailDate(
                            selectedDateMillis =
                                materialDatePickerUtcMillisToLocalStartOfDay(utcTimeMillis),
                            nowMillis = nowMillis
                        )
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = {
                showFutureMailDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedUtcMillis = datePickerState.selectedDateMillis
                        if (selectedUtcMillis != null) {
                            pendingFutureMailDeliverAt =
                                materialDatePickerUtcMillisToLocalStartOfDay(selectedUtcMillis)
                            showFutureMailDatePicker = false
                            showFutureMailConfirmDialog = true
                        }
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text("다음")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showFutureMailDatePicker = false
                    }
                ) {
                    Text("취소")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "이 엽서를 언제 다시 만나고 싶나요?",
                        modifier = Modifier.padding(
                            start = 24.dp,
                            end = 12.dp,
                            top = 16.dp
                        )
                    )
                },
                headline = {
                    Text(
                        text = "한 번 보낸 엽서는 도착하는 날까지\n다시 열어볼 수 없어요.",
                        color = InkSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(
                            start = 24.dp,
                            end = 12.dp,
                            bottom = 8.dp
                        )
                    )
                }
            )
        }
    }

    if (showFutureMailConfirmDialog) {
        val deliverAt = pendingFutureMailDeliverAt

        if (deliverAt != null) {
            val formattedDate = remember(deliverAt) {
                DateTimeFormatter
                    .ofPattern("yyyy년 M월 d일", Locale.KOREA)
                    .format(Instant.ofEpochMilli(deliverAt).atZone(ZoneId.systemDefault()))
            }

            AlertDialog(
                onDismissRequest = {
                    showFutureMailConfirmDialog = false
                },
                title = {
                    Text(
                        text = "정말 보낼까요?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "${formattedDate}까지\n이 엽서를 다시 열어볼 수 없어요."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.sendToFuture(deliverAt)
                            showFutureMailConfirmDialog = false
                            pendingFutureMailDeliverAt = null
                        }
                    ) {
                        Text(
                            text = "엽서 보내기",
                            color = SunsetGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showFutureMailConfirmDialog = false
                        }
                    ) {
                        Text("아직 안 보낼래요")
                    }
                }
            )
        }
    }

    if (exportState is ExportState.Success) {
        SaveResultAlertDialog(
            title = "저장 완료!",
            titleColor = BrutalBlack,
            body = "1:1 포스트카드 이미지를 휴대폰 갤러리에 저장했어.\n\nPictures/PostcardMemory 앨범에서 확인할 수 있어.",
            onAcknowledge = { viewModel.resetExportState() }
        )
    }

    (exportState as? ExportState.Error)?.let {
            exportError ->

        SaveResultAlertDialog(
            title = "저장하지 못했어",
            titleColor = BrutalCoral,
            body = exportError.message,
            onAcknowledge = { viewModel.resetExportState() }
        )
    }

    (
            backgroundUpdateState
                    as? BackgroundUpdateState.Error
            )?.let { backgroundError ->

            SaveResultAlertDialog(
                title = "배경을 저장하지 못했어",
                titleColor = BrutalCoral,
                body = backgroundError.message,
                onAcknowledge = { viewModel.resetBackgroundUpdateState() }
            )
        }

    (
            imageUpdateState
                    as? ImageUpdateState.Error
            )?.let { imageError ->

            SaveResultAlertDialog(
                title = "사진을 바꾸지 못했어",
                titleColor = BrutalCoral,
                body = "사진을 바꾸지 못했어. 기존 사진은 그대로 유지했어.\n" +
                        imageError.message,
                onAcknowledge = { viewModel.resetImageUpdateState() }
            )
        }

    (
            fontUpdateState
                    as? FontUpdateState.Error
            )?.let { fontError ->

            SaveResultAlertDialog(
                title = "폰트를 저장하지 못했어",
                titleColor = BrutalCoral,
                body = fontError.message,
                onAcknowledge = { viewModel.resetFontUpdateState() }
            )
        }

    (
            layoutUpdateState
                    as? LayoutUpdateState.Error
            )?.let { layoutError ->

            SaveResultAlertDialog(
                title = "레이아웃을 저장하지 못했어",
                titleColor = BrutalCoral,
                body = layoutError.message,
                onAcknowledge = { viewModel.resetLayoutUpdateState() }
            )
        }

    (
            dateFormatUpdateState
                    as? DateFormatUpdateState.Error
            )?.let { dateFormatError ->

            SaveResultAlertDialog(
                title = "날짜 형식을 저장하지 못했어",
                titleColor = BrutalCoral,
                body = dateFormatError.message,
                onAcknowledge = { viewModel.resetDateFormatUpdateState() }
            )
        }

        if (postcard != null && !isFocusPreviewMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(ScreenBackgroundGray)
                    .navigationBarsPadding()
                    .padding(top = 4.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                EditorBottomTabBar(
                    selectedPage = customizationPagerState.currentPage,
                    labels = customizationPageLabels,
                    icons = customizationPageIcons,
                    enabled = controlsEnabled,
                    onTabSelected = { pageIndex ->
                        customizationPagerScope.launch {
                            customizationPagerState
                                .animateScrollToPage(pageIndex)
                        }
                    }
                )
            }
        }

        SnackbarHost(
            hostState = detailSnackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 64.dp)
        )
    }

    val readyShareState = shareState as? ShareState.Ready
    if (readyShareState != null) {
        SharePreviewBottomSheet(
            file = readyShareState.file,
            onDismissed = { viewModel.resetShareState() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SharePreviewBottomSheet(
    file: File,
    onDismissed: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var isLaunchingShareChooser by remember { mutableStateOf(false) }

    fun dismissSheet() {
        scope.launch {
            sheetState.hide()
            onDismissed()
        }
    }

    fun launchShareChooser() {
        if (isLaunchingShareChooser) return

        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(
                context,
                "공유 이미지를 준비하지 못했어요",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        isLaunchingShareChooser = true

        val shareUri =
            runCatching {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }.getOrNull()

        if (shareUri == null) {
            Toast.makeText(
                context,
                "엽서를 공유할 수 없어요",
                Toast.LENGTH_SHORT
            ).show()
            isLaunchingShareChooser = false
            return
        }

        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        runCatching {
            context.startActivity(
                Intent.createChooser(shareIntent, "엽서 공유하기")
            )
        }.onFailure {
            Toast.makeText(
                context,
                "이 이미지를 받을 수 있는 앱이 없어요",
                Toast.LENGTH_SHORT
            ).show()
        }

        isLaunchingShareChooser = false
        dismissSheet()
    }

    ModalBottomSheet(
        onDismissRequest = onDismissed,
        sheetState = sheetState,
        containerColor = PaperSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "엽서 공유하기",
                color = BrutalBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "완성된 엽서를 이미지로 보낼 수 있어요",
                color = InkSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PaperTray)
                    .border(
                        BorderStroke(1.dp, PaperDivider),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp)
                    .semantics {
                        contentDescription = "공유할 완성 엽서 미리보기"
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = file,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { launchShareChooser() },
                enabled = !isLaunchingShareChooser,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SunsetGold,
                    contentColor = PaperSurface,
                    disabledContainerColor = SunsetGold.copy(alpha = 0.45f),
                    disabledContentColor = PaperSurface.copy(alpha = 0.65f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = PaperSurface
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "공유하기",
                    color = PaperSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
