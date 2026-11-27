package com.postcardmemory.ui.gallery

import androidx.compose.ui.draw.clip
import java.io.File
import com.postcardmemory.ui.components.PinkingPhotoShape
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.foundation.BorderStroke
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.imageResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.postcardmemory.ui.futuremail.FutureMailGroup
import com.postcardmemory.data.BookmarkDesign
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.components.StampCard
import com.postcardmemory.ui.components.StampCardContent
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.GalleryPaperWhite
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperField
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.ui.theme.PaperTray
import com.postcardmemory.ui.theme.SunsetGold
import com.postcardmemory.ui.theme.SurfaceGray
import com.postcardmemory.R
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

private val PlayModeSaver = Saver<GalleryPlayMode, String>(
    save = { it.name },
    restore = { GalleryPlayMode.valueOf(it) }
)

private val SortOrderSaver = Saver<GallerySortOrder, String>(
    save = { it.name },
    restore = { GallerySortOrder.valueOf(it) }
)

/**
 * 저장된 이름이 3단/캘린더/우표/타임라인처럼 76일차에 삭제된 보기이거나
 * 알 수 없는 값이면 안전 보기인 [GalleryPageFormat.MONTHLY]로 되돌린다.
 */
private val PageFormatSaver = Saver<GalleryPageFormat, String>(
    save = { it.name },
    restore = { saved ->
        runCatching { GalleryPageFormat.valueOf(saved) }
            .getOrDefault(GalleryPageFormat.MONTHLY)
    }
)

private const val SHAKE_THRESHOLD_G = 2.7f
private const val SHAKE_DEBOUNCE_MS = 1000L

// 빈 화면 탭·드래그로 만드는 파문 튜닝값 — 카드 발사와는 무관, 그냥 배경 반응.
private val EMPTY_TAP_MAX_MOVE = 18.dp
private const val EMPTY_TAP_MAX_HOLD_MS = 300L
private val EMPTY_TRAIL_MIN_DISTANCE = 26.dp
private val EMPTY_TRAIL_RIPPLE_RADIUS = 22.dp

// 놀이를 켜고 끌 때 상단 시계·양손 장면이 물러나고 돌아오는 시간(108일차).
private const val GALLERY_TOP_SCENE_ANIM_MS = 450

private fun <T> galleryTopSceneTween() =
    tween<T>(durationMillis = GALLERY_TOP_SCENE_ANIM_MS, easing = FastOutSlowInEasing)
private val EMPTY_TAP_RIPPLE_RADIUS = 34.dp

/**
 * 가속도계로 흔들기를 감지해, 흔들릴 때마다 값이 1씩 증가하는 트리거를 반환한다.
 * 초기값 0은 "아직 흔들리지 않음"을 의미한다.
 *
 * enabled가 false인 동안은 센서를 등록하지 않는다 — 〈엽서의 연못〉 모드가 꺼져
 * 있을 때는 흔들기에 전혀 반응하지 않아야 하고, 배터리도 아낀다.
 */
@Composable
private fun rememberShakeTrigger(enabled: Boolean): Int {
    val context = LocalContext.current
    var triggerCount by remember { mutableStateOf(0) }

    DisposableEffect(enabled) {
        if (!enabled) {
            return@DisposableEffect onDispose { }
        }

        val sensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer =
            sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var lastShakeAtMillis = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val gX = event.values[0] / SensorManager.GRAVITY_EARTH
                val gY = event.values[1] / SensorManager.GRAVITY_EARTH
                val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
                val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

                if (gForce > SHAKE_THRESHOLD_G) {
                    val now = System.currentTimeMillis()
                    if (now - lastShakeAtMillis > SHAKE_DEBOUNCE_MS) {
                        lastShakeAtMillis = now
                        triggerCount++
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(
                listener,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    return triggerCount
}

/**
 * 84일차: 메인 갤러리 "흔들어서 한 장"의 센서 연결. 판정은
 * [GalleryShakeDetector]가 하고, 여기서는 등록/해제만 맡는다.
 *
 * 연못 모드의 [rememberShakeTrigger]와 달리 화면 lifecycle을 따른다 —
 * 갤러리 back stack entry가 RESUMED일 때만 등록하고 ON_PAUSE에서 바로
 * 해제한다. 상세 화면으로 넘어가거나(entry가 RESUMED를 벗어남) 앱이
 * 백그라운드로 가면 감지가 멈추고, composable이 사라지면 onDispose에서
 * 해제된다. 다시 RESUMED가 될 때마다 detector를 새로 만들어 이전 sample이
 * 남지 않게 한다.
 */
@Composable
private fun GalleryShakeToOpenEffect(
    enabled: Boolean,
    onShake: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnShake by rememberUpdatedState(onShake)

    DisposableEffect(enabled, lifecycleOwner) {
        if (!enabled) {
            return@DisposableEffect onDispose { }
        }

        val sensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer =
            sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorManager == null || accelerometer == null) {
            return@DisposableEffect onDispose { }
        }

        var detector = GalleryShakeDetector()
        var registered = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val accepted = detector.onSample(
                    timestampMillis = event.timestamp / 1_000_000L,
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2]
                )

                if (accepted) {
                    currentOnShake()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        fun register() {
            if (registered) return
            detector = GalleryShakeDetector()
            registered = sensorManager.registerListener(
                listener,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        fun unregister() {
            if (!registered) return
            sensorManager.unregisterListener(listener)
            registered = false
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> register()
                Lifecycle.Event.ON_PAUSE -> unregister()
                else -> Unit
            }
        }

        // 이미 RESUMED라면 addObserver가 ON_RESUME까지 따라잡아 바로 등록된다.
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unregister()
        }
    }
}

@Composable
fun GalleryScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToFutureMailbox: () -> Unit,
    visitedEpochDays: Set<Long> = emptySet(),
    totalVisitDays: Int? = null,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val postcards by viewModel.postcards.collectAsState()
    val futureMailGroups by viewModel.futureMailGroups.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.deletionMessages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    var playMode by rememberSaveable(stateSaver = PlayModeSaver) {
        mutableStateOf(GalleryPlayMode.NONE)
    }
    val isPondModeOn = playMode == GalleryPlayMode.POND
    val isShelterModeOn = playMode == GalleryPlayMode.SHEEP_RANCH
    val isRaceModeOn = playMode == GalleryPlayMode.RACE

    val shakeTrigger = rememberShakeTrigger(enabled = isPondModeOn)

    val pondController = remember { PondController() }

    DisposableEffect(Unit) {
        onDispose {
            pondController.reset()
        }
    }

    LaunchedEffect(playMode) {
        if (!isPondModeOn) {
            pondController.reset()
        }
    }

    var selectedIds by remember {
        mutableStateOf<Set<Long>>(emptySet())
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var sortOrder by rememberSaveable(stateSaver = SortOrderSaver) {
        mutableStateOf(GallerySortOrder.NEWEST)
    }

    // 76일차: 월별 보기·기억 저울 보기 2개만 남아 사용자가 껐다 켤 UI가
    // 없으므로 더 이상 토글 가능한 상태가 아니다 — 항상 이 순서(월별 먼저)로
    // 고정된 값이다.
    val activePageFormats = remember {
        setOf(GalleryPageFormat.MONTHLY, GalleryPageFormat.DENSITY)
    }

    var currentPageFormat by rememberSaveable(stateSaver = PageFormatSaver) {
        mutableStateOf(GalleryPageFormat.MONTHLY)
    }

    // 월별 보기의 스크롤 위치를 여기서 hoist해 페이지 스와이프로 잠시 화면
    // 밖에 나갔다 돌아와도 위치가 초기화되지 않게 한다.
    val monthlyGridState = rememberLazyGridState()

    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    var isSearchActive by rememberSaveable {
        mutableStateOf(false)
    }

    var sortMenuExpanded by remember {
        mutableStateOf(false)
    }

    // 143일차: 책갈피 있는 엽서만 보기. 월별 보기에만 적용하고 기억 저울·놀이에는 영향이 없다.
    var bookmarkOnly by rememberSaveable {
        mutableStateOf(false)
    }

    // 검색·정렬은 월별 보기 top bar에만 노출된다(위 topBar 참고). 기억
    // 저울 보기로 넘어간 사이에도 이 상태들이 true로 남아있으면 월별 보기로
    // 돌아왔을 때 검색창이나 드롭다운이 탭 없이 저절로 열려 보인다 — 월별
    // 보기를 벗어나는 순간 정리한다.
    LaunchedEffect(currentPageFormat) {
        if (currentPageFormat != GalleryPageFormat.MONTHLY) {
            isSearchActive = false
            searchQuery = ""
            sortMenuExpanded = false
        }
    }

    var fabMenuExpanded by remember {
        mutableStateOf(false)
    }
    val visitDrawerState = rememberDrawerState(DrawerValue.Closed)
    val visitDrawerScope = rememberCoroutineScope()

    val selectionMode = selectedIds.isNotEmpty()

    // 84일차 후속: 흔들어서 뽑은 엽서. null이 아니면 갤러리 위에
    // [GalleryRandomPostcardOverlay]가 떠 있고, 그동안 흔들기는 꺼진다.
    val randomReveal = remember { GalleryRandomRevealState() }
    val randomCards = remember { GalleryRandomCardRegistry() }
    val randomReducedMotion = rememberRandomRevealReducedMotion()
    val shakePickedPostcardId = randomReveal.session?.postcardId
    val galleryLifecycle = LocalLifecycleOwner.current
    DisposableEffect(galleryLifecycle, randomReveal) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) randomReveal.clear()
        }
        galleryLifecycle.lifecycle.addObserver(observer)
        onDispose {
            galleryLifecycle.lifecycle.removeObserver(observer)
            randomReveal.clear()
        }
    }
    val shakePickedPostcard = shakePickedPostcardId?.let { id ->
        postcards.firstOrNull { it.id == id }
    }

    // overlay가 떠 있는 사이 그 엽서가 목록에서 사라지면(삭제 등) 닫는다 —
    // 그대로 두면 아무것도 안 보이는데 흔들기만 꺼진 상태가 된다.
    LaunchedEffect(shakePickedPostcardId, shakePickedPostcard == null) {
        if (shakePickedPostcardId != null && shakePickedPostcard == null) {
            randomReveal.clear()
        }
    }

    // 84일차: 흔들어서 한 장. 연못 모드는 흔들기를 파문에 쓰고 동글이 쉼터·쫑쫑컵은
    // 전체 화면 놀이라, 일반 갤러리(playMode NONE)에서만 켠다. 선택·삭제 확인·
    // 방문 달력이 열려 있거나 이미 뽑은 엽서 overlay가 떠 있을 때는 끈다.
    // 후보는 지금 화면에 보이는 목록(검색어 적용)과 같다 — 검색이 비어 있으면
    // 갤러리의 일반 엽서 전체이고, 발송된 미래편지는 DAO 단계에서 이미 빠진다.
    GalleryShakeToOpenEffect(
        enabled = playMode == GalleryPlayMode.NONE &&
            !selectionMode &&
            !showDeleteDialog &&
            !visitDrawerState.isOpen &&
            shakePickedPostcardId == null
    ) {
        if (shakePickedPostcardId != null) return@GalleryShakeToOpenEffect

        val candidateIds =
            filterPostcardsForBookmark(
                filterPostcardsForSearch(postcards, searchQuery),
                bookmarks,
                bookmarkOnly && currentPageFormat == GalleryPageFormat.MONTHLY
            ).map { it.id }
        val id = nextShakeOverlayPostcardId(shakePickedPostcardId, candidateIds)
            ?: return@GalleryShakeToOpenEffect

        vibrateGalleryFab(
            context,
            GalleryShakeHapticDurationMs,
            GalleryShakeHapticAmplitude
        )
        fabMenuExpanded = false
        randomReveal.start(
            id,
            if (!randomReducedMotion && currentPageFormat == GalleryPageFormat.MONTHLY) {
                randomCards.source(id, monthlyGridState.layoutInfo.visibleItemsInfo.any { it.key == id })
            } else null
        )
    }

    fun toggleSelection(id: Long) {
        selectedIds =
            if (id in selectedIds) {
                selectedIds - id
            } else {
                selectedIds + id
            }
    }

    fun handleItemClick(id: Long) {
        if (randomReveal.session != null) return
        if (selectedIds.isNotEmpty()) {
            toggleSelection(id)
        } else {
            playMode = GalleryPlayMode.NONE
            onNavigateToDetail(id)
        }
    }

    fun handleItemLongClick(id: Long) {
        if (randomReveal.session != null) return
        toggleSelection(id)
    }

    // 뒤로가기는 검색과 같이 책갈피만 보기부터 풀어 준다. 선택·검색·메뉴가 먼저다(나중에 등록된 쪽 우선).
    BackHandler(
        enabled = bookmarkOnly && playMode == GalleryPlayMode.NONE &&
            currentPageFormat == GalleryPageFormat.MONTHLY
    ) {
        bookmarkOnly = false
    }

    BackHandler(enabled = selectionMode) {
        selectedIds = emptySet()
    }

    BackHandler(enabled = isSearchActive && !selectionMode) {
        isSearchActive = false
        searchQuery = ""
    }

    BackHandler(enabled = fabMenuExpanded) {
        fabMenuExpanded = false
    }

    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }

    VisitCalendarDrawer(visitDrawerState, visitedEpochDays, totalVisitDays) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        modifier = if (shakePickedPostcardId != null) Modifier.clearAndSetSemantics { } else Modifier,
        containerColor = GalleryPaperWhite,

        topBar = {
            if (selectionMode) {
                Column {
                    // 125일차: 세부 편집 상단바와 같은 56dp(버튼 48dp + 위아래 4dp)로 맞춘다.
                    // 기본 갤러리 상단바·검색 바는 별도 Row라 영향이 없다.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GalleryPaperWhite)
                            .padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                selectedIds = emptySet()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "선택 취소",
                                tint = BrutalBlack
                            )
                        }

                        Text(
                            text = "${selectedIds.size}장 선택",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )

                        // 141일차 B안: 아이콘 + "삭제" 글자의 테두리 버튼(종이색 바탕·2dp 모서리).
                        // 보이는 상자는 36dp, 누를 수 있는 영역은 바 높이 48dp 전체다.
                        GallerySelectionDeleteButton(
                            onClick = {
                                showDeleteDialog = true
                            }
                        )
                    }

                    HorizontalDivider(color = SurfaceGray, thickness = 1.dp)
                }
            } else if (isSearchActive) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GalleryPaperWhite)
                            .padding(
                                horizontal = 8.dp,
                                vertical = 8.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = InkSecondary,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(20.dp)
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { newValue ->
                                searchQuery = newValue
                            },
                            placeholder = {
                                Text(
                                    text = "글귀, 장소, 날짜로 검색",
                                    color = InkSecondary
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PaperField,
                                unfocusedContainerColor = PaperField,
                                focusedBorderColor = SunsetGold,
                                unfocusedBorderColor = PaperDivider,
                                focusedTextColor = InkPrimary,
                                unfocusedTextColor = InkPrimary,
                                focusedPlaceholderColor = InkSecondary,
                                unfocusedPlaceholderColor = InkSecondary,
                                cursorColor = SunsetGold
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                }
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            searchQuery = ""
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "검색어 지우기",
                                            tint = InkSecondary
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(searchFocusRequester)
                        )

                        IconButton(
                            onClick = {
                                isSearchActive = false
                                searchQuery = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "검색 종료",
                                tint = BrutalBlack
                            )
                        }
                    }

                    if (bookmarkOnly) {
                        GalleryBookmarkFilterBar(onClear = { bookmarkOnly = false })
                    }
                    HorizontalDivider(color = SurfaceGray, thickness = 1.dp)
                }
            } else {
                // 89일차: 시계·커피 줄까지 불투명하게 칠해, 위로 스크롤한 엽서와
                // 아래 종이 질감이 header 뒤로 비치지 않게 한다.
                Column(modifier = Modifier.background(GalleryPaperWhite)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GalleryPaperWhite)
                            .padding(
                                horizontal = 16.dp,
                                vertical = 10.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                fabMenuExpanded = false
                                sortMenuExpanded = false
                                visitDrawerScope.launch { visitDrawerState.open() }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "방문 달력 열기",
                                tint = InkSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "포스트카드 메모리",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // 검색·정렬은 월별 보기 안에서만 의미가 있다. 기억 저울
                        // 보기는 좌우 스와이프(GalleryPageIndicator 점)로
                        // 들어가므로 별도 진입 아이콘이 필요 없다(76일차).
                        if (currentPageFormat == GalleryPageFormat.MONTHLY) {
                            Box {
                                IconButton(
                                    onClick = {
                                        playMode = GalleryPlayMode.NONE
                                        isSearchActive = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "엽서 검색",
                                        tint = InkSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (currentPageFormat.sortAffectsOrder) {
                            Box {
                                IconButton(
                                    onClick = {
                                        sortMenuExpanded = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SwapVert,
                                        contentDescription = "정렬 방식 변경",
                                        tint = InkSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = {
                                        sortMenuExpanded = false
                                    },
                                    shape = RectangleShape,
                                    containerColor = PaperSurface
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "날짜 최신순",
                                                color = InkPrimary,
                                                fontWeight = if (sortOrder == GallerySortOrder.NEWEST) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Normal
                                                }
                                            )
                                        },
                                        modifier = if (sortOrder == GallerySortOrder.NEWEST) {
                                            Modifier.background(SunsetGold.copy(alpha = 0.16f))
                                        } else {
                                            Modifier
                                        },
                                        onClick = {
                                            sortOrder = GallerySortOrder.NEWEST
                                            sortMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "날짜 오래된 순",
                                                color = InkPrimary,
                                                fontWeight = if (sortOrder == GallerySortOrder.OLDEST) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Normal
                                                }
                                            )
                                        },
                                        modifier = if (sortOrder == GallerySortOrder.OLDEST) {
                                            Modifier.background(SunsetGold.copy(alpha = 0.16f))
                                        } else {
                                            Modifier
                                        },
                                        onClick = {
                                            sortOrder = GallerySortOrder.OLDEST
                                            sortMenuExpanded = false
                                        }
                                    )
                                    HorizontalDivider(color = PaperDivider, thickness = 1.dp)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "책갈피 있는 엽서만",
                                                color = InkPrimary,
                                                fontWeight = if (bookmarkOnly) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Normal
                                                }
                                            )
                                        },
                                        modifier = (if (bookmarkOnly) {
                                            Modifier.background(SunsetGold.copy(alpha = 0.16f))
                                        } else {
                                            Modifier
                                        }).semantics { selected = bookmarkOnly },
                                        onClick = {
                                            bookmarkOnly = !bookmarkOnly
                                            // 놀이 화면은 필터와 무관한 엽서를 띄우므로 켜는 순간 월별 보기로 돌아온다.
                                            if (bookmarkOnly) playMode = GalleryPlayMode.NONE
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 작은 레트로 탁상시계 + 커피잔. 검색·정렬 아이콘이 있는 타이틀 Row와
                    // 겹치지 않게 그 아래 별도 줄에 둔다. 아래 fillMaxWidth는 시계가 놓이는
                    // 자리의 폭이다. 양손은 화면 끝에서 잘리고, 시계의 왼쪽 여백은 내부에서 유지한다.
                    //
                    // 108일차: 세 놀이(연못·쉼터·쫑쫑컵) 중 하나라도 켜지면 시계·양손 장면이
                    // 한 덩어리로 위로 물러나며 높이까지 접혀, 놀이 화면이 그만큼 넓어진다.
                    // 조건이 playMode 하나라 놀이끼리 바로 바꿀 때는 다시 내려오지 않는다.
                    AnimatedVisibility(
                        visible = playMode == GalleryPlayMode.NONE,
                        enter = slideInVertically(
                            animationSpec = galleryTopSceneTween(),
                            initialOffsetY = { fullHeight -> -fullHeight }
                        ) + expandVertically(
                            animationSpec = galleryTopSceneTween(),
                            expandFrom = Alignment.Top
                        ),
                        exit = slideOutVertically(
                            animationSpec = galleryTopSceneTween(),
                            targetOffsetY = { fullHeight -> -fullHeight }
                        ) + shrinkVertically(
                            animationSpec = galleryTopSceneTween(),
                            shrinkTowards = Alignment.Top
                        )
                    ) {
                        GalleryRetroClock(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 6.dp)
                        )
                    }

                    if (
                        bookmarkOnly && playMode == GalleryPlayMode.NONE &&
                        currentPageFormat == GalleryPageFormat.MONTHLY
                    ) {
                        GalleryBookmarkFilterBar(onClear = { bookmarkOnly = false })
                    }
                    HorizontalDivider(color = SurfaceGray, thickness = 1.dp)
                }
            }
        }
    ) { paddingValues ->

        if (isShelterModeOn || isRaceModeOn) {
            val stagePostcards = remember(postcards) {
                postcards
                    .sortedWith(
                        compareByDescending<Postcard> {
                            it.capturedAt
                        }.thenByDescending {
                            it.id
                        }
                    )
                    .take(10)
            }

            GalleryPlayStage(
                postcards = stagePostcards,
                paddingValues = paddingValues,
                onPostcardClick = ::handleItemClick,
                raceEnabled = isRaceModeOn
            )
        } else if (postcards.isEmpty()) {
            // 엽서가 하나도 없을 때의 빈 상태는 어떤 보기 형식을 보고
            // 있든 공통이어야 한다(보기마다 서로 다른 빈 상태를 만들지
            // 않는다). pager 진입 전에 걸러 모든 보기가 자동으로 같은 빈
            // 상태를 쓰게 한다.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                GalleryPaperBackground(modifier = Modifier.matchParentSize())

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
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
                        text = "📮",
                        fontSize = 64.sp
                    )

                    Text(
                        text = "아직 추억이 없어\n첫 번째 사진을 찍어봐!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrutalBlack,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        } else {
            // 배경과 기억 저울이 같은 종이 디코딩 결과를 공유한다.
            val paperTile = rememberGalleryPaperTile()
            // 갤러리는 HorizontalPager 페이지로 묶여, 월별 보기(MONTHLY)와
            // 기억 저울 보기(DENSITY) 두 페이지가 고정 순서로 있다(위
            // activePageFormats). 연못 모드는 별도 화면이 아니라 월별 grid
            // 안에서 그려진다(isPondModeOn 전달).
            //
            // 검색·정렬 적용 결과(displayedPostcards)는 여기 pager 레벨에서
            // 한 번만 계산해 두 페이지에 동일하게 내려보낸다 — 월별 보기를
            // 벗어나면 검색은 항상 비워지므로(위 LaunchedEffect) 기억 저울
            // 보기에서는 사실상 sortOrder만 반영된다.
            val displayedPostcards = remember(postcards, sortOrder, searchQuery) {
                val filtered = filterPostcardsForSearch(postcards, searchQuery)

                when (sortOrder) {
                    GallerySortOrder.NEWEST ->
                        filtered.sortedByDescending { it.capturedAt }

                    GallerySortOrder.OLDEST ->
                        filtered.sortedBy { it.capturedAt }
                }
            }

            // 책갈피 필터는 월별 보기에만 얹는다. 기억 저울은 위 displayedPostcards 그대로다.
            val monthlyPostcards = remember(displayedPostcards, bookmarks, bookmarkOnly) {
                filterPostcardsForBookmark(displayedPostcards, bookmarks, bookmarkOnly)
            }

            // 연못은 월별 묶음 대신 전체 엽서 중 최신 10장만 띄운다(검색·정렬 무관).
            val pondPostcards = remember(postcards) {
                pondPostcardsFor(postcards)
            }

            val orderedActiveFormats = remember(activePageFormats) {
                orderedGalleryPageFormats(activePageFormats)
            }

            val pagerState = rememberPagerState(
                initialPage = orderedActiveFormats.indexOf(currentPageFormat).coerceAtLeast(0),
                pageCount = { orderedActiveFormats.size }
            )

            // orderedActiveFormats를 key에 함께 넣으면, 목록이 바뀐 바로 그
            // 순간(아직 pagerState.currentPage가 이동하기 전) 옛 index를
            // 새 목록에 대입해 currentPageFormat을 엉뚱한 값으로 덮어써
            // 버린다 — 바로 아래 보정 effect가 참조하는 lastKnownFormat
            // 자체가 오염된다. pagerState.currentPage 변화에만 반응해야
            // "실제로 페이지가 이동한 뒤"에만 currentPageFormat을 갱신한다.
            LaunchedEffect(pagerState.currentPage) {
                orderedActiveFormats.getOrNull(pagerState.currentPage)?.let { format ->
                    currentPageFormat = format
                }
            }

            // 활성 보기 목록이 바뀌었을 때(설정 토글이 있던 시절의 보정이다 —
            // 76일차부터 목록이 고정값이라 지금은 사실상 처음 한 번만 돈다) 지금
            // 보고 있던 보기가 여전히 활성 상태면 그 보기로, 방금 꺼진
            // 보기를 보고 있었다면 가장 가까운 유효한 페이지로 이동한다.
            // 인덱스만 clamp하면 중간 페이지가 꺼졌을 때 엉뚱한 보기로
            // 이동할 수 있어, 보기 자체를 기준으로 찾는다.
            // 이 effect가 scrollToPage로 pagerState.currentPage를 바꾸면
            // 위 effect가 그 결과로 다시 실행되어 currentPageFormat을
            // 최종적으로 맞는 값으로 동기화한다.
            LaunchedEffect(orderedActiveFormats) {
                val targetIndex = resolveGalleryPagerTargetIndex(
                    activeFormats = orderedActiveFormats,
                    lastKnownFormat = currentPageFormat,
                    currentIndex = pagerState.currentPage
                )

                if (targetIndex != pagerState.currentPage) {
                    pagerState.scrollToPage(targetIndex)
                }
            }

            // 108일차: 기억 저울 페이지를 보던 중 퀵셀렉트로 연못을 켜면 연못이 보이지 않는
            // 페이지에 머물게 되므로, 연못이 켜지는 순간 월별 페이지로 옮긴다. 이동한 뒤
            // currentPageFormat은 위 currentPage effect가 MONTHLY로 맞춘다.
            LaunchedEffect(isPondModeOn) {
                if (!isPondModeOn) return@LaunchedEffect
                val monthlyIndex = orderedActiveFormats.indexOf(GalleryPageFormat.MONTHLY)
                if (monthlyIndex >= 0 && pagerState.currentPage != monthlyIndex) {
                    pagerState.animateScrollToPage(monthlyIndex)
                }
            }

            // 선택한 엽서가 보이는 월별 페이지에서만 선택 관리를 한다. 진행 중인
            // 스와이프나 프로그램 이동으로 다른 페이지가 잡혀도 선택 ID는 유지한다.
            LaunchedEffect(selectionMode, pagerState.currentPage) {
                if (!selectionMode) return@LaunchedEffect
                val monthlyIndex = orderedActiveFormats.indexOf(GalleryPageFormat.MONTHLY)
                if (monthlyIndex >= 0 && pagerState.currentPage != monthlyIndex) {
                    pagerState.scrollToPage(monthlyIndex)
                }
            }

            val showPageIndicator = orderedActiveFormats.size > 1

            val pageContentPaddingValues = if (showPageIndicator) {
                PaddingValues(
                    top = paddingValues.calculateTopPadding() +
                        GALLERY_PAGE_INDICATOR_RESERVED_HEIGHT,
                    bottom = paddingValues.calculateBottomPadding()
                )
            } else {
                paddingValues
            }

            Box(modifier = Modifier.fillMaxSize()) {
                // 89일차: 종이는 pager 밖 한 레이어라 스크롤·보기 전환에 따라
                // 움직이지 않는다. 페이지들은 배경을 칠하지 않는다.
                GalleryPaperBackground(
                    tile = paperTile,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding())
                )

                // 연못과 선택 관리는 월별 페이지에서만 한다. 둘 다 종료하면 스와이프를 복원한다.
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isPondModeOn && !selectionMode,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (orderedActiveFormats.getOrNull(page)) {
                        GalleryPageFormat.DENSITY ->
                            GalleryMemoryScalePage(
                                postcards = displayedPostcards,
                                paddingValues = paddingValues,
                                paperTile = paperTile,
                                futureMailGroups = futureMailGroups,
                                onOpenFutureMailbox = onNavigateToFutureMailbox,
                                isPageVisible = pagerState.currentPage == page
                            )

                        GalleryPageFormat.MONTHLY, null ->
                            GalleryMonthlyGridPage(
                                postcards = if (isPondModeOn) pondPostcards else monthlyPostcards,
                                bookmarks = bookmarks,
                                bookmarkOnly = bookmarkOnly && !isPondModeOn,
                                selectedIds = selectedIds,
                                shakeTrigger = shakeTrigger,
                                isPondModeOn = isPondModeOn,
                                pondController = pondController,
                                paddingValues = pageContentPaddingValues,
                                searchQuery = searchQuery,
                                gridState = monthlyGridState,
                                randomReveal = randomReveal,
                                randomCards = randomCards,
                                randomReducedMotion = randomReducedMotion,
                                onItemClick = ::handleItemClick,
                                onItemLongClick = ::handleItemLongClick
                            )
                    }
                }

                // 페이지가 잠긴 동안 점만 감춘다. 예약 여백은 그대로 유지한다.
                if (showPageIndicator && !isPondModeOn && !selectionMode) {
                    GalleryPageIndicator(
                        pageCount = orderedActiveFormats.size,
                        currentPage = pagerState.currentPage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingValues.calculateTopPadding())
                            .height(GALLERY_PAGE_INDICATOR_RESERVED_HEIGHT)
                    )
                }
            }
        }
    }

        AnimatedVisibility(
            visible = fabMenuExpanded,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(110, delayMillis = 70)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.20f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        fabMenuExpanded = false
                    }
            )
        }

        GalleryQuickSelectHands(
            expanded = fabMenuExpanded,
            visible = !selectionMode,
            playMode = playMode,
            onToggle = {
                fabMenuExpanded = !fabMenuExpanded
            },
            onNavigateToCamera = {
                fabMenuExpanded = false
                onNavigateToCamera()
            },
            onNavigateToFutureMailbox = {
                fabMenuExpanded = false
                onNavigateToFutureMailbox()
            },
            onPlayModeSelected = { selectedMode ->
                fabMenuExpanded = false
                selectedIds = emptySet()
                playMode = if (playMode == selectedMode) {
                    GalleryPlayMode.NONE
                } else {
                    selectedMode
                }
                // 연못은 검색과 무관한 최신 엽서를 띄우므로, 남아 있던 검색어가
                // 연못 화면과 어긋나 보이지 않게 검색을 닫는다.
                if (playMode == GalleryPlayMode.POND) {
                    isSearchActive = false
                    searchQuery = ""
                }
            },
            // 손은 화면 오른쪽 끝 밖에서 들어오므로 가장자리 여백을 두지 않는다.
            modifier = Modifier.navigationBarsPadding().then(
                if (shakePickedPostcardId != null) Modifier.clearAndSetSemantics { } else Modifier
            )
        )

        shakePickedPostcard?.let { picked ->
            val generation = randomReveal.session?.generation
            // key: 다른 엽서가 뽑히면 등장 연출을 처음부터 다시 한다.
            key(generation) {
                GalleryRandomPostcardOverlay(
                    postcard = picked,
                    state = randomReveal,
                    reducedMotion = randomReducedMotion,
                    prepareSource = {
                        if (monthlyGridState.isScrollInProgress) {
                            monthlyGridState.stopScroll()
                            // Let the cancelled fling's last layout settle before taking its window coordinates.
                            repeat(2) { withFrameNanos { } }
                        }
                        if (!randomReducedMotion && currentPageFormat == GalleryPageFormat.MONTHLY) {
                            randomCards.source(picked.id, monthlyGridState.layoutInfo.visibleItemsInfo.any { it.key == picked.id })
                        } else null
                    },
                    onOpen = {
                        if (randomReveal.session?.generation == generation) {
                            randomReveal.clear(generation)
                            onNavigateToDetail(picked.id)
                        }
                    },
                    onDismissed = {
                        randomReveal.clear(generation)
                    }
                )
            }
        }
    }

    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            // 141일차 B안: 종이 한 장 같은 4dp 모서리 창, 지울 엽서 썸네일, 2dp 모서리 버튼.
            shape = RoundedCornerShape(4.dp),
            containerColor = PaperSurface,
            titleContentColor = BrutalBlack,
            textContentColor = BrutalBlack,
            title = {
                Text(
                    text = "엽서 ${selectedIds.size}장을 삭제할까?",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                val thumbnails = remember(postcards, selectedIds) {
                    galleryDeleteDialogThumbnails(postcards, selectedIds)
                }
                Column {
                    if (thumbnails.shown.isNotEmpty()) {
                        GalleryDeleteThumbnailRow(thumbnails)
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    // 실제 삭제 범위: 앱 안의 엽서 사진·꾸미기 상태·초안 파일과 DB 기록(되돌리기 없음).
                    Text(
                        text = "삭제하면 되돌릴 수 없어. 엽서에 담긴 사진과 꾸미기도 함께 지워져."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idsToDelete = selectedIds

                        showDeleteDialog = false
                        selectedIds = emptySet()

                        viewModel.deletePostcards(idsToDelete)
                    },
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GalleryDangerRed,
                        contentColor = PaperSurface
                    )
                ) {
                    Text(
                        text = "${selectedIds.size}장 삭제",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteDialog = false
                    },
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.5.dp, PaperDivider),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = InkPrimary
                    )
                ) {
                    Text(
                        text = "취소",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}

/**
 * 141일차 B안: 선택 모드 상단 삭제 버튼. 휴지통 아이콘과 "삭제" 글자를 적갈색 테두리·종이색
 * 바탕·2dp 모서리 상자에 함께 둔다(알약형 아님). 보이는 상자는 36dp, 터치 영역은 48dp.
 */
@Composable
private fun GallerySelectionDeleteButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(end = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .background(PaperSurface, RoundedCornerShape(2.dp))
                .border(1.5.dp, GalleryDangerRed, RoundedCornerShape(2.dp))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = GalleryDangerRed,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "삭제",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = GalleryDangerRed
            )
        }
    }
}

/** 삭제 확인창에 보여줄 썸네일: 갤러리 순서대로 앞의 [limit]장과 나머지 장 수. */
internal data class GalleryDeleteThumbnails(
    val shown: List<Postcard>,
    val remaining: Int
)

internal const val GALLERY_DELETE_THUMBNAIL_LIMIT = 4

internal fun galleryDeleteDialogThumbnails(
    postcards: List<Postcard>,
    selectedIds: Set<Long>,
    limit: Int = GALLERY_DELETE_THUMBNAIL_LIMIT
): GalleryDeleteThumbnails {
    val selected = postcards.filter { it.id in selectedIds }
    return GalleryDeleteThumbnails(
        shown = selected.take(limit),
        remaining = (selected.size - limit).coerceAtLeast(0)
    )
}

/**
 * 지울 엽서 썸네일 줄. 44dp 크기로만 불러와(Coil이 표시 크기에 맞춰 decode) 원본을 크게 읽지
 * 않고, 4장을 넘으면 "+N"만 적는다. 사진을 못 읽으면 종이색 빈 자리로 남는다.
 */
@Composable
private fun GalleryDeleteThumbnailRow(thumbnails: GalleryDeleteThumbnails) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        thumbnails.shown.forEach { postcard ->
            AsyncImage(
                model = File(postcard.imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(PinkingPhotoShape)
                    .background(PaperTray)
            )
        }
        if (thumbnails.remaining > 0) {
            Text(
                text = "+${thumbnails.remaining}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkSecondary,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

// 63일차 추가 구현: 기존 카메라 FAB 단일 진입점을 우측 하단 확장 클러스터로
// 개편해 엽서 생성·미래 우체통·특별한 갤러리(연못/양떼목장/쫑쫑컵) 3종을
// 한 곳에서 바로 펼쳐 접근하게 했다. 좌측 패널(구 [GalleryFeatureDrawer])이
// 담당하던 두 진입 기능은 전부 이 자리로 흡수되어 좌측 패널 자체는 제거됐다.
//
// 88일차: 같은 다섯 기능을 신문 오림 손 다섯 장이 직접 꺼내 보여주는 부채로
// 바꿨다. 기능 연결(부모 callback·navigation·노출 조건·뒤로가기·바깥 탭)은
// 그대로이고 표현만 바뀐다. "특별한 갤러리" 묶음 단계와 68일차 롱프레스 드래그
// 바로 실행은 이때 빠졌다(사용자 승인). 부채 계산은 GalleryQuickSelectFan.kt.

// 68일차 1차 후속: 실기기 QA 결과 LocalHapticFeedback.performHapticFeedback()이
// 손끝에 전혀 느껴지지 않는다고 확인됨 — 기기의 "터치 피드백" 시스템 설정이나
// SegmentTick/Confirm 같은 최신 상수의 API 레벨 지원 여부에 따라 조용히
// 무반응일 수 있는 것으로 판단, 원인을 앱 코드 안에서 통제 가능한 가장 작은
// 표준 수단인 Vibrator.vibrate(VibrationEffect)로 직접 교체한다(minSdk 26부터
// createOneShot 사용 가능, 새 프레임워크 도입 없음).
// 기능 확정의 "톡".
private const val GalleryFabHapticConfirmDurationMs = 22L
private const val GalleryFabHapticConfirmAmplitude = 160
// 68일차 2차 후속: 손잡이(구 + 버튼) 짧은 탭 전용 — 기능 확정의 "톡"보다도
// 더 짧고 가벼운, 메뉴를 열기 전 손끝에 주는 최소한의 답.
private const val GalleryFabHapticAnchorTapDurationMs = 10L
private const val GalleryFabHapticAnchorTapAmplitude = 90
// 84일차: 흔들어서 한 장 — 인식됐다는 것만 알리는 짧은 "톡". 도장 "콩!"보다 약하다.
private const val GalleryShakeHapticDurationMs = 18L
private const val GalleryShakeHapticAmplitude = 120

private fun vibrateGalleryFab(context: Context, durationMillis: Long, amplitude: Int) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    if (!vibrator.hasVibrator()) return
    vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, amplitude))
}

// 켜져 있는 놀이 모드의 손은 블록 테두리만 옅은 금색 잉크로 짚어준다(구 선택 배경과 같은 계열).
private val GalleryQuickSelectActiveColor = SunsetGold
// 닫힌 묶음이 오른쪽 끝에 삐져나온 자리의 손잡이 터치 영역.
private val GalleryQuickSelectHandleWidth = 60.dp
// 127일차: 기억 저울 아래 미래 우체통 종이가 닫힌 손잡이를 피하는 계산에도 쓴다.
internal val GalleryQuickSelectHandleHeight = 64.dp

private fun GalleryQuickSelectItem.handResId(): Int =
    when (this) {
        GalleryQuickSelectItem.CAMERA -> R.drawable.quick_select_camera_hand
        GalleryQuickSelectItem.FUTURE_MAILBOX -> R.drawable.quick_select_letter_hand
        GalleryQuickSelectItem.POND -> R.drawable.quick_select_river_hand
        GalleryQuickSelectItem.DONGGEULI_SHELTER -> R.drawable.quick_select_shelter_hand
        GalleryQuickSelectItem.RACE -> R.drawable.quick_select_checker_hand
    }

// 접근성 이름은 기존 버튼의 contentDescription을 그대로 쓴다.
private fun GalleryQuickSelectItem.label(): String =
    when (this) {
        GalleryQuickSelectItem.CAMERA -> "카메라"
        GalleryQuickSelectItem.FUTURE_MAILBOX -> "미래 우체통"
        GalleryQuickSelectItem.POND -> "엽서의 연못"
        GalleryQuickSelectItem.DONGGEULI_SHELTER -> "동글이 쉼터"
        GalleryQuickSelectItem.RACE -> "엽서 쫑쫑컵"
    }

private fun GalleryQuickSelectItem.playMode(): GalleryPlayMode? =
    when (this) {
        GalleryQuickSelectItem.POND -> GalleryPlayMode.POND
        GalleryQuickSelectItem.DONGGEULI_SHELTER -> GalleryPlayMode.SHEEP_RANCH
        GalleryQuickSelectItem.RACE -> GalleryPlayMode.RACE
        GalleryQuickSelectItem.CAMERA, GalleryQuickSelectItem.FUTURE_MAILBOX -> null
    }

@Composable
private fun GalleryQuickSelectHands(
    expanded: Boolean,
    visible: Boolean,
    playMode: GalleryPlayMode,
    onToggle: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToFutureMailbox: () -> Unit,
    onPlayModeSelected: (GalleryPlayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) {
        return
    }

    val context = LocalContext.current
    val currentOnNavigateToCamera by rememberUpdatedState(onNavigateToCamera)
    val currentOnNavigateToFutureMailbox by rememberUpdatedState(onNavigateToFutureMailbox)
    val currentOnPlayModeSelected by rememberUpdatedState(onPlayModeSelected)

    // 손은 어떤 기능을 부를지만 고르고, 실행은 기존 부모 callback 그대로다.
    fun dispatch(item: GalleryQuickSelectItem) {
        when (item) {
            GalleryQuickSelectItem.CAMERA -> currentOnNavigateToCamera()
            GalleryQuickSelectItem.FUTURE_MAILBOX -> currentOnNavigateToFutureMailbox()
            GalleryQuickSelectItem.POND -> currentOnPlayModeSelected(GalleryPlayMode.POND)
            GalleryQuickSelectItem.DONGGEULI_SHELTER -> currentOnPlayModeSelected(GalleryPlayMode.SHEEP_RANCH)
            GalleryQuickSelectItem.RACE -> currentOnPlayModeSelected(GalleryPlayMode.RACE)
        }
    }

    var phase by remember { mutableStateOf(GalleryQuickSelectPhase.CLOSED) }
    // 0 = 닫힌 손잡이, 1 = 쑤욱 들어옴.
    val slide = remember { Animatable(0f) }
    // 펼치기 직전 묶음이 아래로 살짝 젖혀지는 정도.
    val tilt = remember { Animatable(0f) }
    // 0 = 한 묶음, 1 = 부채로 다 펼쳐짐.
    val spread = remember { Animatable(0f) }

    // 쑤욱 → 젖힘 → 촤라락. 닫기는 단순 역방향. 반복해서 쓰는 메뉴라 전체를
    // 0.4초 안쪽으로 두고, 방향이 바뀌면 지금 자리에서 바로 되돌아간다.
    LaunchedEffect(expanded) {
        phase = phase.onRequest(expanded)
        when (phase) {
            GalleryQuickSelectPhase.OPENING -> {
                coroutineScope {
                    if (slide.value < 1f) {
                        slide.animateTo(1f, tween(130, easing = FastOutSlowInEasing))
                    }
                    if (spread.value < 1f) {
                        tilt.animateTo(1f, tween(60, easing = LinearOutSlowInEasing))
                        launch { tilt.animateTo(0f, tween(170, easing = FastOutSlowInEasing)) }
                        spread.animateTo(1f, tween(170, easing = FastOutSlowInEasing))
                    }
                }
                phase = phase.onAnimationFinished()
            }

            GalleryQuickSelectPhase.CLOSING -> {
                coroutineScope {
                    launch { tilt.animateTo(0f, tween(90)) }
                    spread.animateTo(0f, tween(120, easing = FastOutLinearInEasing))
                    slide.animateTo(0f, tween(110, easing = FastOutLinearInEasing))
                }
                phase = phase.onAnimationFinished()
            }

            else -> Unit
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthDp = maxWidth.value
        val heightDp = maxHeight.value
        val spacingDeg = quickSelectSpacingDegrees(heightDp)
        val pivot = quickSelectPivot(widthDp, heightDp, spacingDeg)
        val touchHalfHeightDp = quickSelectTouchHalfHeightDp(spacingDeg)

        // 손잡이: 닫힌 묶음이 삐져나온 자리. 손들보다 먼저 두어 펼쳐진 뒤
        // 겹치는 곳은 손의 터치 영역이 입력을 받는다. 열린 동안 누르면 닫힌다.
        val closedCenter = quickSelectAnimatedBlockCenter(
            pivot,
            slide = 0f,
            angleDeg = quickSelectHandAngle(0, spacingDeg, spread = 0f, tilt = 0f)
        )
        Box(
            modifier = Modifier
                .offset(
                    x = maxWidth - GalleryQuickSelectHandleWidth,
                    y = closedCenter.y.dp - GalleryQuickSelectHandleHeight / 2
                )
                .size(GalleryQuickSelectHandleWidth, GalleryQuickSelectHandleHeight)
                .semantics {
                    contentDescription = if (expanded) "바로가기 닫기" else "바로가기 열기"
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button
                ) {
                    vibrateGalleryFab(
                        context,
                        GalleryFabHapticAnchorTapDurationMs,
                        GalleryFabHapticAnchorTapAmplitude
                    )
                    onToggle()
                }
        )

        // 위 손부터 그려 아래 손이 위 손의 손바닥을 덮는다 — 블록은 가려지지 않는다.
        GalleryQuickSelectItem.entries
            .sortedByDescending { it.slotFromBottom }
            .forEach { item ->
                key(item) {
                    GalleryQuickSelectHand(
                        item = item,
                        phase = phase,
                        isActive = item.playMode() == playMode,
                        pivot = pivot,
                        spacingDeg = spacingDeg,
                        touchHalfHeightDp = touchHalfHeightDp,
                        slide = slide,
                        tilt = tilt,
                        spread = spread,
                        onSelect = {
                            val (next, accepted) = phase.onSelect()
                            phase = next
                            if (accepted) {
                                vibrateGalleryFab(
                                    context,
                                    GalleryFabHapticConfirmDurationMs,
                                    GalleryFabHapticConfirmAmplitude
                                )
                                dispatch(item)
                            }
                        }
                    )
                }
            }
    }
}

@Composable
private fun GalleryQuickSelectHand(
    item: GalleryQuickSelectItem,
    phase: GalleryQuickSelectPhase,
    isActive: Boolean,
    pivot: QuickSelectPoint,
    spacingDeg: Float,
    touchHalfHeightDp: Float,
    slide: Animatable<Float, AnimationVector1D>,
    tilt: Animatable<Float, AnimationVector1D>,
    spread: Animatable<Float, AnimationVector1D>,
    onSelect: () -> Unit
) {
    val hand: ImageBitmap = ImageBitmap.imageResource(item.handResId())
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // 선택 반응은 아주 짧은 눌림 하나 — bounce·glow 없음.
    val pressScale = animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(70),
        label = "galleryQuickSelectHandPress"
    )

    val dpPerPx = QUICK_SELECT_DP_PER_SOURCE_PX
    val blockCenterY = (item.blockTopPx + item.blockBottomPx) / 2f * dpPerPx

    Box(
        modifier = Modifier
            .size(QUICK_SELECT_HAND_SIZE_DP.dp)
            // 연출 값은 draw 단계에서만 읽어 프레임마다 recomposition하지 않는다.
            // 블록 중심을 부채 반지름 위에 놓고 그 점을 축으로 돌린다.
            .graphicsLayer {
                val angle = quickSelectHandAngle(item.slotFromBottom, spacingDeg, spread.value, tilt.value)
                val center = quickSelectAnimatedBlockCenter(pivot, slide.value, angle)
                translationX = center.x.dp.toPx() - item.blockCenterXFraction * size.width
                translationY = center.y.dp.toPx() - item.blockCenterYFraction * size.height
                transformOrigin = TransformOrigin(item.blockCenterXFraction, item.blockCenterYFraction)
                rotationZ = angle
                val scale = quickSelectHandScale(slide.value) * pressScale.value
                scaleX = scale
                scaleY = scale
            }
    ) {
        Image(
            bitmap = hand,
            contentDescription = null,
            filterQuality = FilterQuality.Medium,
            modifier = Modifier.fillMaxSize()
        )

        if (isActive) {
            Box(
                modifier = Modifier
                    .offset(x = (item.blockLeftPx * dpPerPx).dp, y = (item.blockTopPx * dpPerPx).dp)
                    .size(
                        width = ((item.blockRightPx - item.blockLeftPx) * dpPerPx).dp,
                        height = ((item.blockBottomPx - item.blockTopPx) * dpPerPx).dp
                    )
                    .border(1.5.dp, GalleryQuickSelectActiveColor.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
            )
        }

        // 터치 영역은 블록보다 조금 넉넉하게, 이웃 손과 겹치지 않는 높이까지만.
        // 닫힌 묶음에서는 두지 않는다(손잡이가 받는다).
        if (phase != GalleryQuickSelectPhase.CLOSED) {
            Box(
                modifier = Modifier
                    .offset(
                        x = (item.blockLeftPx * dpPerPx - QUICK_SELECT_TOUCH_PAD_X_DP).dp,
                        y = (blockCenterY - touchHalfHeightDp).dp
                    )
                    .size(
                        width = (item.blockHalfWidthDp * 2f + QUICK_SELECT_TOUCH_PAD_X_DP * 2f).dp,
                        height = (touchHalfHeightDp * 2f).dp
                    )
                    .semantics {
                        contentDescription = item.label()
                        if (isActive) {
                            selected = true
                        }
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = phase.acceptsSelection,
                        role = Role.Button,
                        onClick = onSelect
                    )
            )
        }
    }
}

/** 보기 형식 dot indicator 한 줄이 차지하는 고정 높이. 페이지 콘텐츠는 이만큼을 위쪽 padding으로 더 확보해 겹치지 않는다. */
private val GALLERY_PAGE_INDICATOR_RESERVED_HEIGHT = 28.dp

/** 종이 타일 한 장이 화면에 보이는 크기. 기기 밀도와 상관없이 섬유 크기를 일정하게 둔다. */
internal val GALLERY_PAPER_TILE_SIZE = 360.dp

/** 헤더와 같은 단색 위에 겹치는 종이 타일의 농도. 실기기 QA로 맞추는 값이다. */
private const val GALLERY_PAPER_TILE_ALPHA = 1f

/** 배경과 기억 저울에 전달할 종이 이미지를 같은 composition에서 한 번 읽는다. */
@Composable
private fun rememberGalleryPaperTile(): ImageBitmap =
    ImageBitmap.imageResource(R.drawable.gallery_paper_tile)

/**
 * 89일차: 구분선 아래 갤러리 본문에 까는 카드보드 종이. 사용자 제공 seamless
 * 타일(drawable-nodpi/gallery_paper_tile.png)을 가로·세로 같은 배율로만 줄여
 * 반복한다 — 늘리거나 자르지 않아 화면 비율이 달라도 섬유 모양이 그대로다.
 * 헤더 단색을 먼저 칠하고 그 위에 [GALLERY_PAPER_TILE_ALPHA]로 겹친다.
 */
@Composable
private fun GalleryPaperBackground(
    modifier: Modifier = Modifier,
    tile: ImageBitmap = rememberGalleryPaperTile()
) {

    Spacer(
        modifier = modifier.drawWithCache {
            val shader = ImageShader(tile, TileMode.Repeated, TileMode.Repeated)
            val scale = GALLERY_PAPER_TILE_SIZE.toPx() / tile.width
            shader.setLocalMatrix(android.graphics.Matrix().apply { setScale(scale, scale) })
            val brush = ShaderBrush(shader)

            onDrawBehind {
                drawRect(GalleryPaperWhite)
                drawRect(brush, alpha = GALLERY_PAPER_TILE_ALPHA)
            }
        }
    )
}

/**
 * 활성 보기 목록이 바뀐 뒤 pager가 위치해야 할 새 페이지 index를 계산한다
 * (보기 비활성화 edge case). 순수 함수라 Compose 없이도
 * 검증 가능하다.
 *
 * - [lastKnownFormat]이 여전히 [activeFormats]에 있으면 그 새 위치로 이동한다
 *   (중간 보기가 꺼져 인덱스가 당겨져도 같은 보기를 계속 보게 된다).
 * - 없으면(지금 보던 보기 자체가 꺼짐) [currentIndex]를 유효 범위로 clamp한다.
 * - [activeFormats]가 비어 있으면 0을 돌려준다(활성 보기는 항상 고정 2개라
 *   실제로는 발생하지 않아야 하는 방어적 경로).
 */
internal fun resolveGalleryPagerTargetIndex(
    activeFormats: List<GalleryPageFormat>,
    lastKnownFormat: GalleryPageFormat,
    currentIndex: Int
): Int {
    val preservedIndex = activeFormats.indexOf(lastKnownFormat)

    if (preservedIndex >= 0) {
        return preservedIndex
    }

    if (activeFormats.isEmpty()) {
        return 0
    }

    return currentIndex.coerceIn(0, activeFormats.lastIndex)
}

/** 활성 집합의 순서와 무관하게 제품의 고정 보기 순서로 pager 목록을 만든다. */
internal fun orderedGalleryPageFormats(
    activeFormats: Set<GalleryPageFormat>
): List<GalleryPageFormat> =
    GalleryPageFormat.entries.filter { it in activeFormats }

/**
 * 활성 보기가 둘 이상일 때만 그리는 점 indicator — 점이면 점답게,
 * pill이나 floating box로 감싸지 않는다.
 */
@Composable
private fun GalleryPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isCurrent = index == currentPage

            Box(
                modifier = Modifier
                    .size(if (isCurrent) 7.dp else 6.dp)
                    .background(
                        color = if (isCurrent) SunsetGold else PaperDivider,
                        shape = CircleShape
                    )
            )
        }
    }
}

/** 월별 보기 grid 셀에 쓰는 "일(day)만" 표기 — 월 헤더가 이미 연/월을 보여준다. */
private val monthlyGridDayLabelFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd")

/**
 * 62일차 2차: 월별로 묶어 보여주는 grid 보기(76일차부터 기본 갤러리).
 * 월마다 둥근 카드로 감싸지 않고, 기존 [GalleryMonthHeader]를 그대로
 * 재사용해 하나의 [LazyVerticalGrid] 안에서 전체 폭 헤더와 3열 썸네일을
 * 번갈아 그린다 — `LazyColumn`을 중첩하지 않고
 * `item(span = { GridItemSpan(maxLineSpan) })`으로 헤더에만 전체 폭을
 * 줘서 스크롤 컨테이너를 하나로 유지한다.
 *
 * 연못 수면([PondWaterSurface] — 탭/드래그·발사·충돌 물살)도 이 grid 뒤에
 * 그린다. 108일차: 연못 모드에서는 호출부가 [pondPostcardsFor]로 고른 최신
 * 10장을 넘기고, 이 페이지는 월 헤더(월·장수) 없이 그 엽서만 3열로 띄운다.
 * 스크롤 위치는 연못 전용 state에 따로 둬, 연못을 꺼도 월별 보기의 위치가
 * 그대로 돌아온다. 검색 결과가 없을 때의 안내([SearchEmptyState])도 이 페이지가
 * 맡는다("엽서가 하나도 없음" 판정은 호출부인 pager 레벨이 먼저 처리한다).
 */
@Composable
private fun GalleryMonthlyGridPage(
    postcards: List<Postcard>,
    bookmarks: Map<Long, BookmarkDesign?>?,
    bookmarkOnly: Boolean,
    selectedIds: Set<Long>,
    shakeTrigger: Int,
    isPondModeOn: Boolean,
    pondController: PondController,
    paddingValues: PaddingValues,
    searchQuery: String,
    gridState: LazyGridState,
    randomReveal: GalleryRandomRevealState,
    randomCards: GalleryRandomCardRegistry,
    randomReducedMotion: Boolean,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit
) {
    if (postcards.isEmpty()) {
        // 책갈피를 아직 불러오는 중이면 "없음" 안내를 띄우지 않는다.
        if (bookmarkOnly && bookmarks == null) return
        SearchEmptyState(
            query = searchQuery.trim(),
            paddingValues = paddingValues,
            bookmarkOnly = bookmarkOnly
        )
        return
    }

    val monthSections = remember(postcards, isPondModeOn) {
        if (isPondModeOn) emptyList() else monthSectionsFor(postcards)
    }

    val pondGridState = rememberLazyGridState()
    val randomDensity = LocalDensity.current

    var originInWindow by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                if (isPondModeOn) {
                    originInWindow = coordinates.positionInWindow()
                    pondController.gridBoundsInWindow = coordinates.boundsInWindow()
                }
            }
            .then(
                if (isPondModeOn) {
                    Modifier.pointerInput(pondController) {
                        val tapMaxMovePx = EMPTY_TAP_MAX_MOVE.toPx()
                        val trailMinDistancePx = EMPTY_TRAIL_MIN_DISTANCE.toPx()
                        val trailRipplePx = EMPTY_TRAIL_RIPPLE_RADIUS.toPx()
                        val tapRipplePx = EMPTY_TAP_RIPPLE_RADIUS.toPx()

                        // 카드나 스크롤 제스처를 가로채지 않도록 Initial 패스로 살짝
                        // 엿보기만 하고 절대 consume하지 않는다 — 순수 배경 장식용.
                        awaitPointerEventScope {
                            var downPosition: Offset? = null
                            var downTimeMillis = 0L
                            var lastTrailPosition: Offset? = null

                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull() ?: continue

                                when {
                                    change.changedToDownIgnoreConsumed() -> {
                                        downPosition = change.position
                                        downTimeMillis = System.currentTimeMillis()
                                        lastTrailPosition = change.position
                                    }

                                    change.changedToUpIgnoreConsumed() -> {
                                        val start = downPosition
                                        if (start != null) {
                                            val totalMove =
                                                (change.position - start).getDistance()
                                            val heldMs =
                                                System.currentTimeMillis() - downTimeMillis
                                            if (
                                                totalMove < tapMaxMovePx &&
                                                heldMs < EMPTY_TAP_MAX_HOLD_MS
                                            ) {
                                                pondController.addRipple(
                                                    change.position + originInWindow,
                                                    tapRipplePx,
                                                    700L
                                                )
                                            }
                                        }
                                        downPosition = null
                                        lastTrailPosition = null
                                    }

                                    change.pressed -> {
                                        val current = change.position
                                        val last = lastTrailPosition
                                        if (
                                            last != null &&
                                            (current - last).getDistance() > trailMinDistancePx
                                        ) {
                                            pondController.addRipple(
                                                current + originInWindow,
                                                trailRipplePx,
                                                450L
                                            )
                                            lastTrailPosition = current
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        if (isPondModeOn) {
            // 108일차: 하늘색 하프톤 수면. 상단 여백보다 36dp 아래까지 물이 차오른다 — 기울였을 때
            // 높아지는 쪽 물이 상단 바 뒤로 숨지 않게 위쪽 여유를 둔다. 파문 이벤트(탭·자취·
            // 발사·비행·벽 충돌) 자리마다 도트 면이 출렁인다.
            PondWaterSurface(
                controller = pondController,
                originInWindow = originInWindow,
                surfaceTop = paddingValues.calculateTopPadding() + 36.dp,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 월 라벨 종이 띠는 목록 item마다 decode하지 않게 페이지에서 한 번만 불러온다.
        val dateStrip = ImageBitmap.imageResource(R.drawable.gallery_date_paper_strip)

        // 108일차: 연잎도 카드마다 decode하지 않게 페이지에서 한 번만, 연못이 켜진 동안만 불러온다.
        val pondLeaf = if (isPondModeOn) ImageBitmap.imageResource(R.drawable.pond_lily_pad) else null

        LazyVerticalGrid(
            state = if (isPondModeOn) pondGridState else gridState,
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = paddingValues.calculateTopPadding() + 14.dp,
                bottom = paddingValues.calculateBottomPadding() + 88.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = randomReveal.session == null,
            modifier = Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                randomCards.viewport = androidx.compose.ui.geometry.Rect(
                    position.x,
                    position.y + with(randomDensity) { paddingValues.calculateTopPadding().toPx() },
                    position.x + coordinates.size.width,
                    position.y + coordinates.size.height
                )
            }
        ) {
            if (isPondModeOn) {
                lazyGridItems(
                    items = postcards,
                    key = { postcard -> postcard.id }
                ) { postcard ->
                    GalleryMonthlyGridItem(
                        postcard = postcard,
                        isSelected = postcard.id in selectedIds,
                        shakeTrigger = shakeTrigger,
                        isPondModeOn = true,
                        pondController = pondController,
                        pondLeaf = pondLeaf,
                        onClick = {
                            onItemClick(postcard.id)
                        },
                        onLongClick = {
                            onItemLongClick(postcard.id)
                        }
                    )
                }
            }

            monthSections.forEach { section ->
                item(
                    key = "month_${section.yearMonth}",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    GalleryMonthHeader(
                        yearMonth = section.yearMonth,
                        postcardCount = section.postcards.size,
                        dateStrip = dateStrip
                    )
                }

                lazyGridItems(
                    items = section.postcards,
                    key = { postcard -> postcard.id }
                ) { postcard ->
                    GalleryMonthlyGridItem(
                        postcard = postcard,
                        isSelected = postcard.id in selectedIds,
                        shakeTrigger = shakeTrigger,
                        isPondModeOn = isPondModeOn,
                        pondController = pondController,
                        onClick = {
                            onItemClick(postcard.id)
                        },
                        onLongClick = {
                            onItemLongClick(postcard.id)
                        },
                        liftOnOpen = selectedIds.isEmpty(),
                        bookmark = bookmarks?.get(postcard.id),
                        randomReveal = randomReveal,
                        randomCards = randomCards,
                        randomReducedMotion = randomReducedMotion
                    )
                }
            }
        }
    }
}

/**
 * 월별 보기의 썸네일 한 칸. 평상시(연못 모드 꺼짐)에는 실제 시각 요소(사진·
 * 선택 표시·뒷면 편지 배지)만 그리는 가벼운 [StampCardContent]를 그대로
 * 쓰고, 날짜 자리만 "일(day)"로 바꾼다.
 *
 * 연못 모드가 켜졌을 때만 물리·기울임 연출이 붙은 무거운 [StampCard]로
 * 바꿔 그린다(평상시 렌더링 비용은 그대로 유지).
 */
@Composable
private fun GalleryMonthlyGridItem(
    postcard: Postcard,
    isSelected: Boolean,
    shakeTrigger: Int,
    isPondModeOn: Boolean,
    pondController: PondController,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    pondLeaf: ImageBitmap? = null,
    // 선택 모드가 아니라 탭이 상세로 가는 경우에만 집어 드는 움직임을 보여준다.
    liftOnOpen: Boolean = false,
    bookmark: BookmarkDesign? = null,
    randomReveal: GalleryRandomRevealState? = null,
    randomCards: GalleryRandomCardRegistry? = null,
    randomReducedMotion: Boolean = false
) {
    val dayLabel = remember(postcard.capturedAt) {
        Instant.ofEpochMilli(postcard.capturedAt)
            .atZone(ZoneId.systemDefault())
            .format(monthlyGridDayLabelFormatter)
    }

    if (isPondModeOn) {
        StampCard(
            postcard = postcard,
            isSelected = isSelected,
            shakeTrigger = shakeTrigger,
            isPondModeOn = true,
            pondController = pondController,
            // 108일차: 연못은 월 헤더가 없어 "일"만으론 의미가 없으므로 날짜 줄을 그리지 않는다.
            dateLabelOverride = "",
            pondLeaf = pondLeaf,
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        DisposableEffect(postcard.id, randomCards) {
            onDispose { randomCards?.remove(postcard.id) }
        }
        Box(Modifier.onGloballyPositioned { coordinates ->
            val position = coordinates.positionInWindow()
            randomCards?.update(postcard.id, androidx.compose.ui.geometry.Rect(
                position.x, position.y, position.x + coordinates.size.width, position.y + coordinates.size.width
            ))
        }) {
        StampCardContent(
            postcard = postcard,
            isSelected = isSelected,
            dateLabelOverride = dayLabel,
            bookmark = bookmark,
            // 130일차 B: 눌림·누른 쪽 기울기·집어 듦(그리기만, 클릭 처리는 기존과 같은 combinedClickable).
            modifier = Modifier.semantics { selected = isSelected }
                .randomPostcardShake(postcard.id, randomReveal, randomReducedMotion).then(galleryCardPressFeel(
                liftOnOpen = liftOnOpen,
                enabled = randomReveal?.session == null,
                onClick = onClick,
                onLongClick = onLongClick
            ))
        )
        }
    }
}

/**
 * 이번 달의 날짜 칸 목록을 만든다. 1일 이전은 일요일 시작 기준으로 빈
 * 칸(null)을 채우고, 마지막 주도 7의 배수가 되도록 뒤를 null로 채운다 —
 * 실제 존재하지 않는 날짜는 그리지 않되 요일 정렬은 항상 유지한다.
 *
 * 76일차: 갤러리 자체의 "캘린더 보기"는 삭제됐지만, 이 함수는 방문
 * 달력([VisitCalendarDrawer])이 그대로 재사용하므로 남겨둔다.
 */
internal fun calendarCellsFor(yearMonth: YearMonth): List<LocalDate?> {
    val firstOfMonth = yearMonth.atDay(1)
    // DayOfWeek.value: MONDAY=1..SUNDAY=7 → 일요일 시작 기준으로 0~6 변환.
    val leadingBlanks = firstOfMonth.dayOfWeek.value % 7

    val cells = mutableListOf<LocalDate?>()
    repeat(leadingBlanks) { cells.add(null) }
    (1..yearMonth.lengthOfMonth()).forEach { day ->
        cells.add(yearMonth.atDay(day))
    }
    while (cells.size % 7 != 0) {
        cells.add(null)
    }

    return cells
}

/**
 * 검색 결과가 없을 때만 보여주는 전용 빈 화면. "저장된 엽서 없음"
 * 상태([postcards.isEmpty()][GalleryScreen])와는 이미 상위에서 분기되어
 * 있어, 여기 도달했다는 건 항상 엽서는 있지만 검색어에 걸리는 게 없다는
 * 뜻이다.
 */
@Composable
private fun SearchEmptyState(
    query: String,
    paddingValues: PaddingValues,
    bookmarkOnly: Boolean = false
) {
    if (bookmarkOnly) {
        BookmarkFilterEmptyState(query = query, paddingValues = paddingValues)
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                .padding(
                    horizontal = 38.dp,
                    vertical = 32.dp
                )
        ) {
            Text(
                text = "🔍",
                fontSize = 64.sp
            )

            Text(
                text = "'$query' 관련 엽서는 아직 없어.\n다른 기억의 조각을 검색해 봐.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BrutalBlack,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

/** 143일차: 책갈피만 보기에 걸린 엽서가 없을 때. 종이 바탕 위에 글만 둔다(이모지·둥근 칩 없음). */
@Composable
private fun BookmarkFilterEmptyState(query: String, paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (query.isEmpty()) {
                "아직 책갈피를 끼운 엽서가 없어.\n아끼는 엽서를 열고 더보기에서 만들어 봐."
            } else {
                "책갈피 있는 엽서 중에는\n'$query' 관련 엽서가 없어."
            },
            fontSize = 15.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
            color = InkSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/** 책갈피만 보기가 켜져 있음을 알리는 한 줄. 누를 수 있는 영역은 48dp다. */
@Composable
private fun GalleryBookmarkFilterBar(onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GalleryPaperWhite)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "책갈피 있는 엽서만 보는 중",
            fontSize = 13.sp,
            color = InkPrimary,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClear, shape = RoundedCornerShape(2.dp)) {
            Text(text = "모두 보기", fontSize = 13.sp, color = InkSecondary, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** 책갈피가 있는 엽서만 남긴다. [bookmarks]가 아직 null(불러오기 전)이면 아무것도 남기지 않는다. */
internal fun filterPostcardsForBookmark(
    postcards: List<Postcard>,
    bookmarks: Map<Long, BookmarkDesign?>?,
    bookmarkOnly: Boolean
): List<Postcard> =
    if (!bookmarkOnly) postcards else postcards.filter { bookmarks?.containsKey(it.id) == true }

private data class GalleryMonthSection(
    val yearMonth: YearMonth,
    val postcards: List<Postcard>
)

private val monthHeaderLabelFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy년 M월")

/**
 * 이미 정렬된 [postcards] 순서를 그대로 따라 월별로 묶는다. 그룹 자체를
 * 별도로 재정렬하지 않으므로, 최신순/오래된순 어느 쪽으로 들어와도
 * 첫 등장 순서가 곧 월의 표시 순서가 된다.
 */
private fun monthSectionsFor(
    postcards: List<Postcard>
): List<GalleryMonthSection> {
    val grouped = LinkedHashMap<YearMonth, MutableList<Postcard>>()

    postcards.forEach { postcard ->
        val yearMonth =
            YearMonth.from(
                Instant.ofEpochMilli(postcard.capturedAt)
                    .atZone(ZoneId.systemDefault())
            )

        grouped.getOrPut(yearMonth) { mutableListOf() }.add(postcard)
    }

    return grouped.map { (yearMonth, postcardsInMonth) ->
        GalleryMonthSection(
            yearMonth = yearMonth,
            postcards = postcardsInMonth
        )
    }
}

private val searchDateFormatters: List<DateTimeFormatter> = listOf(
    DateTimeFormatter.ofPattern("yyyy"),
    DateTimeFormatter.ofPattern("yyyy-MM"),
    DateTimeFormatter.ofPattern("yyyy.MM.dd"),
    DateTimeFormatter.ofPattern("yyyy년 M월")
)

private fun capturedAtMatchesQuery(capturedAt: Long, query: String): Boolean {
    val zonedCapturedAt =
        Instant.ofEpochMilli(capturedAt).atZone(ZoneId.systemDefault())

    return searchDateFormatters.any { formatter ->
        zonedCapturedAt.format(formatter).contains(query, ignoreCase = true)
    }
}

/**
 * 문구(message)·장소(location)·날짜(capturedAt) 기준으로 [postcards]를 좁힌다.
 * 검색어가 비어 있거나 공백뿐이면 필터를 적용하지 않고 원본 목록을 그대로
 * 반환한다. Room 컬럼이나 저장된 값은 건드리지 않고, capturedAt(epoch millis)을
 * 검색 시점에만 여러 날짜 문자열로 변환해 비교한다.
 */
internal fun filterPostcardsForSearch(
    postcards: List<Postcard>,
    query: String
): List<Postcard> {
    val trimmedQuery = query.trim()

    if (trimmedQuery.isEmpty()) {
        return postcards
    }

    return postcards.filter { postcard ->
        postcard.message.contains(trimmedQuery, ignoreCase = true) ||
            postcard.location?.contains(trimmedQuery, ignoreCase = true) == true ||
            capturedAtMatchesQuery(postcard.capturedAt, trimmedQuery)
    }
}

/**
 * gallery_date_paper_strip.png 안에서 투명 여백을 뺀 실제 종이 영역(px, alpha > 16). 자산을 바꾸면 다시 잰다.
 * 107일차: 자산 2172×724 → 1086×362(라벨 표시 폭 대비 약 4.5배 과대, 갤러리 원본 decode 6MiB 상주)에 맞춰 새 자산에서 다시 잼.
 */
private val GALLERY_DATE_STRIP_SRC_OFFSET = IntOffset(44, 86)
private val GALLERY_DATE_STRIP_SRC_SIZE = IntSize(1006, 189)

/** 종이 띠를 짧은 날짜에 맞춰 너무 찌그러뜨리지 않게 하는 최소 폭. */
private val GALLERY_DATE_STRIP_MIN_WIDTH = 136.dp

/**
 * 100일차: 민트 테이프 그림을 다른 색 테이프로 칠한다. 자산은 그대로 두고 밝기만 남긴 뒤
 * 톤을 곱해, 종이 결·찢긴 가장자리는 살리고 색만 바꾼다. 꾸미기 테이프 색과 무관하다.
 * 101일차: 편집 하단 세부 선택줄의 선택된 종이 라벨도 같은 톤으로 칠한다(PaperLabelSubcategoryNav.kt).
 * 102일차: 바랜 코랄(#E0B09E) → 편집 상위 잡지 오림 삽화의 종이톤과 이어지는 바랜 갈색.
 * 테이프 평균 밝기(≈204)가 #B89A7A 근처로 오도록 맞춘 비율이다.
 */
internal val GALLERY_DATE_STRIP_BROWN_FILTER: ColorFilter = run {
    val toneR = 184f / 204f
    val toneG = 154f / 204f
    val toneB = 122f / 204f
    fun lumaRow(tone: Float) = floatArrayOf(0.299f * tone, 0.587f * tone, 0.114f * tone, 0f, 0f)
    ColorFilter.colorMatrix(
        ColorMatrix(
            lumaRow(toneR) + lumaRow(toneG) + lumaRow(toneB) + floatArrayOf(0f, 0f, 0f, 1f, 0f)
        )
    )
}

/**
 * 95일차: 월 라벨. 화면 폭 단색 띠 없이 뒤의 갤러리 종이 위에, 날짜 글자
 * 뒤에만 작은 가로 띠(gallery_date_paper_strip.png, 민트 테이프 그림을 바랜 갈색으로 칠해 씀)를 깐다. 글자는
 * 그대로 Text이고 종이는 배경일 뿐이다. 장 수와 아래 구분선은 그대로 둔다.
 */
@Composable
private fun GalleryMonthHeader(
    yearMonth: YearMonth,
    postcardCount: Int,
    dateStrip: ImageBitmap
) {
    val label =
        remember(yearMonth) {
            yearMonth.format(monthHeaderLabelFormatter)
        }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = "$label, 엽서 ${postcardCount}장"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    end = 16.dp,
                    top = 6.dp,
                    bottom = 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = GALLERY_DATE_STRIP_MIN_WIDTH)
                    .drawBehind {
                        drawImage(
                            image = dateStrip,
                            srcOffset = GALLERY_DATE_STRIP_SRC_OFFSET,
                            srcSize = GALLERY_DATE_STRIP_SRC_SIZE,
                            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                            colorFilter = GALLERY_DATE_STRIP_BROWN_FILTER,
                            filterQuality = FilterQuality.High
                        )
                    }
                    .padding(horizontal = 18.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${postcardCount}장",
                fontSize = 12.sp,
                color = InkSecondary
            )
        }

        HorizontalDivider(color = PaperDivider, thickness = 1.dp)
    }
}
