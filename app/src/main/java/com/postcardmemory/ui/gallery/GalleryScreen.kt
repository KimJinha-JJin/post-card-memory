package com.postcardmemory.ui.gallery

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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MailOutline
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.hilt.navigation.compose.hiltViewModel
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.components.StampCard
import com.postcardmemory.ui.components.StampCardContent
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalCoral
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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.sqrt
import kotlinx.coroutines.launch

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
    val isSheepRanchModeOn = playMode == GalleryPlayMode.SHEEP_RANCH
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

    // 76일차: 월별 보기·기억 밀도 보기 2개만 남아 사용자가 껐다 켤 UI가
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

    // 검색·정렬은 월별 보기 top bar에만 노출된다(위 topBar 참고). 기억
    // 밀도 보기로 넘어간 사이에도 이 상태들이 true로 남아있으면 월별 보기로
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

    fun toggleSelection(id: Long) {
        selectedIds =
            if (id in selectedIds) {
                selectedIds - id
            } else {
                selectedIds + id
            }
    }

    fun handleItemClick(id: Long) {
        if (selectedIds.isNotEmpty()) {
            toggleSelection(id)
        } else {
            playMode = GalleryPlayMode.NONE
            onNavigateToDetail(id)
        }
    }

    fun handleItemLongClick(id: Long) {
        toggleSelection(id)
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
        containerColor = GalleryPaperWhite,

        topBar = {
            if (selectionMode) {
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
                            text = "${selectedIds.size}개 선택",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrutalCoral,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )

                        IconButton(
                            onClick = {
                                showDeleteDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "선택 항목 삭제",
                                tint = GalleryDangerRed
                            )
                        }
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
                                    text = "문구, 장소, 날짜로 검색",
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

                    HorizontalDivider(color = SurfaceGray, thickness = 1.dp)
                }
            } else {
                Column {
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

                        // 검색·정렬은 월별 보기 안에서만 의미가 있다. 기억 밀도
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
                                }
                            }
                        }
                    }

                    // 작은 레트로 탁상시계 + 커피잔. 검색·정렬 아이콘이 있는 타이틀 Row와
                    // 겹치지 않게 그 아래 별도 줄에 둔다. 아래 fillMaxWidth는 시계가 놓이는
                    // 자리의 폭일 뿐이고, 시계 바디 자체는 내부 글자 폭에 맞춰 스스로 닫힌다
                    // (GalleryRetroClockFace의 IntrinsicSize.Min) — 폭을 여기서 제한하지 않는다.
                    GalleryRetroClock(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    HorizontalDivider(color = SurfaceGray, thickness = 1.dp)
                }
            }
        }
    ) { paddingValues ->

        if (isSheepRanchModeOn || isRaceModeOn) {
            val ranchPostcards = remember(postcards) {
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

            SheepRanchStage(
                postcards = ranchPostcards,
                paddingValues = paddingValues,
                onPostcardClick = ::handleItemClick,
                raceEnabled = isRaceModeOn
            )
        } else if (postcards.isEmpty()) {
            // 62일차 2차: 엽서가 하나도 없을 때의 빈 상태는 어떤 보기
            // 형식을 보고 있든 공통이어야 한다(작업지시서 30절 — 보기마다
            // 서로 다른 빈 상태를 만들지 않는다). pager 진입 전에 걸러
            // 앞으로 추가될 보기(월별/타임라인/캘린더/우표/기억 밀도)도
            // 자동으로 같은 빈 상태를 쓰게 한다.
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
                        text = "아직 추억이 없어요\n첫 번째 사진을 찍어봐요!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrutalBlack,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        } else {
            // 62일차 보기 시스템 1차: 기존 3단 갤러리(그리드/세부 기록 토글
            // 포함)를 통째로 "3단 보기" 페이지 하나로 편입했다. 활성 보기가
            // 기본값(3단 보기 하나)뿐이면 페이지가 1개라 HorizontalPager는
            // 사실상 아무것도 바꾸지 않는 얇은 래퍼일 뿐이고, 기존 동작은
            // 그대로다. GalleryPlayMode(연못/양떼목장/쫑쫑컵)는 이 보기
            // 시스템과 무관하게 위에서 먼저 분기되어 그대로 전체 화면을
            // 차지한다(기존 우선순위 유지).
            //
            // 2차: 검색·정렬 적용 결과(displayedPostcards)는 3단 보기뿐
            // 아니라 월별 보기도 함께 쓰는 "정렬된 postcard 데이터"라
            // (작업지시서 10절) 여기 pager 레벨에서 한 번만 계산해 두
            // 페이지에 동일하게 내려보낸다 — 3단 보기를 벗어나면 검색은
            // 항상 비워지므로(위 LaunchedEffect) 다른 보기에서는 사실상
            // sortOrder만 반영된다.
            val displayedPostcards = remember(postcards, sortOrder, searchQuery) {
                val filtered = filterPostcardsForSearch(postcards, searchQuery)

                when (sortOrder) {
                    GallerySortOrder.NEWEST ->
                        filtered.sortedByDescending { it.capturedAt }

                    GallerySortOrder.OLDEST ->
                        filtered.sortedBy { it.capturedAt }
                }
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
            // 버린다 — 바로 아래 44절 보정 effect가 참조하는 lastKnownFormat
            // 자체가 오염된다. pagerState.currentPage 변화에만 반응해야
            // "실제로 페이지가 이동한 뒤"에만 currentPageFormat을 갱신한다.
            LaunchedEffect(pagerState.currentPage) {
                orderedActiveFormats.getOrNull(pagerState.currentPage)?.let { format ->
                    currentPageFormat = format
                }
            }

            // 활성 보기 목록이 바뀌었을 때(설정에서 보기를 켜거나 끔) 지금
            // 보고 있던 보기가 여전히 활성 상태면 그 보기로, 방금 꺼진
            // 보기를 보고 있었다면 가장 가까운 유효한 페이지로 이동한다.
            // 인덱스만 clamp하면 중간 페이지가 꺼졌을 때 엉뚱한 보기로
            // 이동할 수 있어(작업지시서 44절), 보기 자체를 기준으로 찾는다.
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
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (orderedActiveFormats.getOrNull(page)) {
                        GalleryPageFormat.DENSITY ->
                            GalleryDensityPage(
                                postcards = displayedPostcards,
                                paddingValues = pageContentPaddingValues
                            )

                        GalleryPageFormat.MONTHLY, null ->
                            GalleryMonthlyGridPage(
                                postcards = displayedPostcards,
                                selectedIds = selectedIds,
                                shakeTrigger = shakeTrigger,
                                isPondModeOn = isPondModeOn,
                                pondController = pondController,
                                paddingValues = pageContentPaddingValues,
                                searchQuery = searchQuery,
                                gridState = monthlyGridState,
                                onItemClick = ::handleItemClick,
                                onItemLongClick = ::handleItemLongClick
                            )
                    }
                }

                if (showPageIndicator) {
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

        GalleryFabCluster(
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
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp)
        )
    }

    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            containerColor = PaperSurface,
            titleContentColor = BrutalBlack,
            textContentColor = BrutalBlack,
            title = {
                Text(
                    text = "${selectedIds.size}개를 삭제할까요?",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "선택한 사진은 삭제 후 복구할 수 없어요."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val idsToDelete = selectedIds

                        showDeleteDialog = false
                        selectedIds = emptySet()

                        viewModel.deletePostcards(idsToDelete)
                    }
                ) {
                    Text(
                        text = "삭제",
                        color = GalleryDangerRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text(
                        text = "취소",
                        color = GraphiteAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

// 63일차 추가 구현: 기존 카메라 FAB 단일 진입점을 + 확장 클러스터로
// 개편해 엽서 생성·미래 우체통·특별한 갤러리(연못/양떼목장/쫑쫑컵) 3종을
// 우측 하단 한 곳에서 바로 펼쳐 접근하게 한다. 좌측 패널
// (구 [GalleryFeatureDrawer])이 담당하던 두 진입 기능은 전부 이 클러스터로
// 흡수되어 좌측 패널 자체는 제거됐다 — 아이콘은 새로 만들지 않고 각 기능이
// 이미 쓰던 것([PondDrawerIcon] 등)을 그대로 재사용한다.
private val GalleryFabAnchorSize = 52.dp
private val GalleryFabPrimarySize = 48.dp
private val GalleryFabMiniSize = 36.dp
// 같은 웜 뉴트럴 계열 안에서 단계와 소속만 구분한다.
private val GalleryFabPrimaryColor = lerp(InkSecondary, PaperTray, 0.18f)
private val GalleryFabChildSelectedColor = lerp(PaperTray, SunsetGold, 0.24f)
// 롱프레스 드래그 selection boundary를 시각적 원보다 살짝 넓힌다(조준 게임 방지).
private val GalleryFabDragHitSlop = 8.dp
private val GalleryFabPulseColor = PaperSurface
// 68일차 2차 후속: 선택 링은 물방울 pulse와 헷갈리지 않도록 별도의 따뜻한
// 강조색을 쓴다(연못/양떼목장 등 기존 selected 배경에 쓰는 SunsetGold 계열과 통일).
private val GalleryFabSelectionRingColor = SunsetGold

// 68일차 1차 후속: 실기기 QA 결과 LocalHapticFeedback.performHapticFeedback()이
// 손끝에 전혀 느껴지지 않는다고 확인됨 — 기기의 "터치 피드백" 시스템 설정이나
// SegmentTick/Confirm 같은 최신 상수의 API 레벨 지원 여부에 따라 조용히
// 무반응일 수 있는 것으로 판단, 원인을 앱 코드 안에서 통제 가능한 가장 작은
// 표준 수단인 Vibrator.vibrate(VibrationEffect)로 직접 교체한다(minSdk 26부터
// createOneShot 사용 가능, 새 프레임워크 도입 없음).
private const val GalleryFabHapticLongPressDurationMs = 35L
private const val GalleryFabHapticLongPressAmplitude = 190
private const val GalleryFabHapticSegmentTickDurationMs = 12L
private const val GalleryFabHapticSegmentTickAmplitude = 110
private const val GalleryFabHapticConfirmDurationMs = 22L
private const val GalleryFabHapticConfirmAmplitude = 160
// 68일차 2차 후속: + 짧은 탭 전용 — 롱프레스의 "또잉"보다 가볍고 기능 확정의
// "톡"보다도 더 짧고 가벼운, 메뉴를 열기 전 손끝에 주는 최소한의 답.
private const val GalleryFabHapticAnchorTapDurationMs = 10L
private const val GalleryFabHapticAnchorTapAmplitude = 90

private fun vibrateGalleryFab(context: Context, durationMillis: Long, amplitude: Int) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    if (!vibrator.hasVibrator()) return
    vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, amplitude))
}

// 68일차 추가: + 롱프레스 후 손을 떼지 않고 드래그해 바로 실행하는 빠른 조작용
// 대상 식별자. 기존 탭 실행 흐름과 동일한 콜백을 그대로 재사용한다(dispatch 참고).
private enum class GalleryFabDragTarget {
    CAMERA, FUTURE_MAILBOX, SPECIAL_GALLERY_TOGGLE, POND, SHEEP_RANCH, RACE
}

@Composable
private fun GalleryFabCluster(
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

    // 펼침 상태가 바뀌면 자식의 수동 접힘도 초기화한다. 별도 navigation 상태는 없다.
    var childrenExpanded by remember(expanded) { mutableStateOf(true) }
    val anchorRotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(
            durationMillis = if (expanded) 160 else 90,
            delayMillis = if (expanded) 0 else 150,
            easing = FastOutSlowInEasing
        ),
        label = "galleryFabAnchorRotation"
    )

    val context = LocalContext.current
    val density = LocalDensity.current
    val currentExpanded by rememberUpdatedState(expanded)
    val currentOnToggle by rememberUpdatedState(onToggle)
    val currentOnNavigateToCamera by rememberUpdatedState(onNavigateToCamera)
    val currentOnNavigateToFutureMailbox by rememberUpdatedState(onNavigateToFutureMailbox)
    val currentOnPlayModeSelected by rememberUpdatedState(onPlayModeSelected)

    // 각 draggable 대상의 실제 터치 영역(윈도우 좌표). 등장 애니메이션이
    // 50% 미만이거나 사라진 대상은 null로 비워 hit-test에서 제외한다.
    val dragTargetBounds = remember { mutableStateMapOf<GalleryFabDragTarget, Rect>() }
    fun updateDragTargetBounds(target: GalleryFabDragTarget, bounds: Rect?) {
        if (bounds == null) {
            dragTargetBounds.remove(target)
        } else {
            dragTargetBounds[target] = bounds
        }
    }
    fun hitTestDragTarget(windowPosition: Offset): GalleryFabDragTarget? {
        val slopPx = with(density) { GalleryFabDragHitSlop.toPx() }
        var best: GalleryFabDragTarget? = null
        var bestDistanceSq = Float.MAX_VALUE
        dragTargetBounds.forEach { (target, bounds) ->
            val inflated = bounds.inflate(slopPx)
            if (inflated.contains(windowPosition)) {
                val dx = inflated.center.x - windowPosition.x
                val dy = inflated.center.y - windowPosition.y
                val distanceSq = dx * dx + dy * dy
                if (distanceSq < bestDistanceSq) {
                    bestDistanceSq = distanceSq
                    best = target
                }
            }
        }
        return best
    }

    // 기존 Tap onClick과 롱프레스 드래그 release가 정확히 같은 동작을
    // 실행하도록 대상별 실행 경로를 한 곳에만 둔다.
    fun dispatchDragTarget(target: GalleryFabDragTarget) {
        when (target) {
            GalleryFabDragTarget.CAMERA -> currentOnNavigateToCamera()
            GalleryFabDragTarget.FUTURE_MAILBOX -> currentOnNavigateToFutureMailbox()
            GalleryFabDragTarget.SPECIAL_GALLERY_TOGGLE -> childrenExpanded = !childrenExpanded
            GalleryFabDragTarget.POND -> currentOnPlayModeSelected(GalleryPlayMode.POND)
            GalleryFabDragTarget.SHEEP_RANCH -> currentOnPlayModeSelected(GalleryPlayMode.SHEEP_RANCH)
            GalleryFabDragTarget.RACE -> currentOnPlayModeSelected(GalleryPlayMode.RACE)
        }
    }

    var anchorPressed by remember { mutableStateOf(false) }
    var longPressActive by remember { mutableStateOf(false) }
    var dragCandidate by remember { mutableStateOf<GalleryFabDragTarget?>(null) }
    var anchorCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val anchorScale by animateFloatAsState(
        targetValue = if (anchorPressed || longPressActive) 0.94f else 1f,
        animationSpec = tween(90),
        label = "galleryFabAnchorPress"
    )
    // 68일차 1차 후속: "꾸욱 눌렀더니 메뉴가 열린다"가 아니라 "꾸욱 잡았더니
    // 버튼이 반응한다"는 감각을 위해, 롱프레스가 확정되는 순간에만 한 번의
    // 뚜렷한 탄성(오버슈트 후 정착)을 anchorScale 위에 곱해서 얹는다.
    var longPressPunchTrigger by remember { mutableStateOf(0) }
    val anchorPunch = remember { Animatable(1f) }
    LaunchedEffect(longPressPunchTrigger) {
        if (longPressPunchTrigger > 0) {
            anchorPunch.snapTo(1f)
            anchorPunch.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 220
                    1f at 0
                    1.22f at 90 using FastOutSlowInEasing
                    1f at 220 using FastOutSlowInEasing
                }
            )
        }
    }
    // 68일차 2차 후속: 짧은 탭도 같은 anchorPunch를 재사용하되, 롱프레스보다
    // 훨씬 가벼운 peak/duration으로 눌렀다는 최소한의 시각 답만 준다.
    var tapPunchTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(tapPunchTrigger) {
        if (tapPunchTrigger > 0) {
            anchorPunch.snapTo(1f)
            anchorPunch.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 150
                    1f at 0
                    1.09f at 60 using FastOutSlowInEasing
                    1f at 150 using FastOutSlowInEasing
                }
            )
        }
    }

    // 펼친 버튼의 48dp 터치 영역도 부모 layout 안에 둔다. 빈 공간은 입력을 소비하지 않는다.
    Box(modifier = modifier.size(width = 160.dp, height = 216.dp)) {
        // 작은 3개는 부모(-48, -100)의 왼쪽·왼쪽 위·위에 붙는다.
        GalleryFabShortcut(
            expanded = expanded && childrenExpanded,
            offsetX = (-100).dp,
            offsetY = (-92).dp,
            originX = (-48).dp,
            originY = (-100).dp,
            enterDelayMillis = 120,
            size = GalleryFabMiniSize,
            backgroundColor = if (playMode == GalleryPlayMode.POND) GalleryFabChildSelectedColor else PaperTray,
            dragTarget = GalleryFabDragTarget.POND,
            isDragSelected = dragCandidate == GalleryFabDragTarget.POND,
            onDragTargetBoundsChanged = ::updateDragTargetBounds,
            onClick = { dispatchDragTarget(GalleryFabDragTarget.POND) }
        ) {
            Icon(
                imageVector = PondDrawerIcon,
                contentDescription = "엽서의 연못",
                tint = InkPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        GalleryFabShortcut(
            expanded = expanded && childrenExpanded,
            offsetX = (-92).dp,
            offsetY = (-144).dp,
            originX = (-48).dp,
            originY = (-100).dp,
            enterDelayMillis = 160,
            size = GalleryFabMiniSize,
            backgroundColor = if (playMode == GalleryPlayMode.SHEEP_RANCH) GalleryFabChildSelectedColor else PaperTray,
            dragTarget = GalleryFabDragTarget.SHEEP_RANCH,
            isDragSelected = dragCandidate == GalleryFabDragTarget.SHEEP_RANCH,
            onDragTargetBoundsChanged = ::updateDragTargetBounds,
            onClick = { dispatchDragTarget(GalleryFabDragTarget.SHEEP_RANCH) }
        ) {
            Icon(
                imageVector = SheepDrawerIcon,
                contentDescription = "양떼목장",
                tint = InkPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        GalleryFabShortcut(
            expanded = expanded && childrenExpanded,
            offsetX = (-40).dp,
            offsetY = (-156).dp,
            originX = (-48).dp,
            originY = (-100).dp,
            enterDelayMillis = 200,
            size = GalleryFabMiniSize,
            backgroundColor = if (playMode == GalleryPlayMode.RACE) GalleryFabChildSelectedColor else PaperTray,
            dragTarget = GalleryFabDragTarget.RACE,
            isDragSelected = dragCandidate == GalleryFabDragTarget.RACE,
            onDragTargetBoundsChanged = ::updateDragTargetBounds,
            onClick = { dispatchDragTarget(GalleryFabDragTarget.RACE) }
        ) {
            Icon(
                imageVector = CheckFlagDrawerIcon,
                contentDescription = "엽서 쫑쫑컵",
                tint = InkPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        // 주요 3개는 동일한 시간에 anchor에서 펼쳐진다. 특별한 갤러리는
        // 카메라·미래 우체통을 잇는 선의 위쪽 중심부에 자리해 큰 3개가
        // 하나의 군집으로 읽히게 한다.
        GalleryFabShortcut(
            expanded = expanded,
            offsetX = (-48).dp,
            offsetY = (-100).dp,
            size = GalleryFabPrimarySize,
            backgroundColor = InkSecondary,
            dragTarget = GalleryFabDragTarget.SPECIAL_GALLERY_TOGGLE,
            isDragSelected = dragCandidate == GalleryFabDragTarget.SPECIAL_GALLERY_TOGGLE,
            onDragTargetBoundsChanged = ::updateDragTargetBounds,
            onClick = { dispatchDragTarget(GalleryFabDragTarget.SPECIAL_GALLERY_TOGGLE) },
            modifier = Modifier.semantics {
                stateDescription = if (childrenExpanded) "하위 기능 펼쳐짐" else "하위 기능 접힘"
            }
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "특별한 갤러리",
                tint = PaperSurface,
                modifier = Modifier.size(24.dp)
            )
        }

        GalleryFabShortcut(
            expanded = expanded,
            offsetX = (-64).dp,
            offsetY = (-44).dp,
            size = GalleryFabPrimarySize,
            backgroundColor = GalleryFabPrimaryColor,
            dragTarget = GalleryFabDragTarget.FUTURE_MAILBOX,
            isDragSelected = dragCandidate == GalleryFabDragTarget.FUTURE_MAILBOX,
            onDragTargetBoundsChanged = ::updateDragTargetBounds,
            onClick = { dispatchDragTarget(GalleryFabDragTarget.FUTURE_MAILBOX) }
        ) {
            Icon(
                imageVector = Icons.Default.MailOutline,
                contentDescription = "미래 우체통",
                tint = PaperSurface,
                modifier = Modifier.size(24.dp)
            )
        }

        GalleryFabShortcut(
            expanded = expanded,
            offsetX = 0.dp,
            offsetY = (-68).dp,
            size = GalleryFabPrimarySize,
            backgroundColor = GalleryFabPrimaryColor,
            dragTarget = GalleryFabDragTarget.CAMERA,
            isDragSelected = dragCandidate == GalleryFabDragTarget.CAMERA,
            onDragTargetBoundsChanged = ::updateDragTargetBounds,
            onClick = { dispatchDragTarget(GalleryFabDragTarget.CAMERA) }
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "카메라",
                tint = PaperSurface,
                modifier = Modifier.size(24.dp)
            )
        }

        // + anchor — 전체 펼침/접힘의 기준점. 펼쳐지면 45도 회전해 자연스럽게
        // 닫기(×) 표시로 읽히게 한다(새 아이콘을 추가하지 않는다).
        // 68일차 추가: 기존 짧은 탭(펼침/접힘)은 detectTapGestures로 그대로
        // 유지하고, 그 옆에 detectDragGesturesAfterLongPress를 별도
        // pointerInput으로 얹어 롱프레스+드래그 빠른 조작을 추가한다 — 두
        // detector가 같은 포인터 스트림을 각자 관찰하다가, 롱프레스가
        // 인식되는 순간 드래그 쪽이 소비를 시작해 탭 쪽 gesture가 자연히
        // 취소되므로 이중 실행 걱정 없이 공존한다.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(GalleryFabAnchorSize)
                .onGloballyPositioned { anchorCoordinates = it }
                .graphicsLayer {
                    scaleX = anchorScale * anchorPunch.value
                    scaleY = anchorScale * anchorPunch.value
                }
                .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
                .background(InkPrimary, CircleShape)
                // 커스텀 pointerInput으로 바뀌어도 TalkBack 등 접근성 서비스는
                // 실제 터치 제스처가 아니라 이 semantics onClick 액션으로
                // 여전히 기존 짧은 탭(펼침/접힘)에 접근할 수 있어야 한다.
                .semantics(mergeDescendants = true) {
                    contentDescription = if (expanded) "바로가기 닫기" else "바로가기 열기"
                    role = Role.Button
                    onClick(label = null) {
                        currentOnToggle()
                        true
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            anchorPressed = true
                            tryAwaitRelease()
                            anchorPressed = false
                        },
                        onTap = {
                            tapPunchTrigger++
                            vibrateGalleryFab(
                                context,
                                GalleryFabHapticAnchorTapDurationMs,
                                GalleryFabHapticAnchorTapAmplitude
                            )
                            currentOnToggle()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            longPressActive = true
                            dragCandidate = null
                            longPressPunchTrigger++
                            vibrateGalleryFab(
                                context,
                                GalleryFabHapticLongPressDurationMs,
                                GalleryFabHapticLongPressAmplitude
                            )
                            if (!currentExpanded) {
                                currentOnToggle()
                            }
                        },
                        onDragEnd = {
                            val finalCandidate = dragCandidate
                            longPressActive = false
                            dragCandidate = null
                            if (finalCandidate != null) {
                                vibrateGalleryFab(
                                    context,
                                    GalleryFabHapticConfirmDurationMs,
                                    GalleryFabHapticConfirmAmplitude
                                )
                                dispatchDragTarget(finalCandidate)
                            }
                        },
                        onDragCancel = {
                            longPressActive = false
                            dragCandidate = null
                        }
                    ) { change, _ ->
                        change.consume()
                        val windowPosition = anchorCoordinates?.localToWindow(change.position)
                        val newCandidate = windowPosition?.let(::hitTestDragTarget)
                        if (newCandidate != dragCandidate) {
                            dragCandidate = newCandidate
                            if (newCandidate != null) {
                                vibrateGalleryFab(
                                    context,
                                    GalleryFabHapticSegmentTickDurationMs,
                                    GalleryFabHapticSegmentTickAmplitude
                                )
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = PaperSurface,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(anchorRotation)
            )
        }
    }
}

@Composable
private fun BoxScope.GalleryFabShortcut(
    expanded: Boolean,
    offsetX: Dp,
    offsetY: Dp,
    size: Dp,
    backgroundColor: Color,
    onClick: () -> Unit,
    originX: Dp = 0.dp,
    originY: Dp = 0.dp,
    enterDelayMillis: Int = 0,
    dragTarget: GalleryFabDragTarget? = null,
    isDragSelected: Boolean = false,
    onDragTargetBoundsChanged: ((GalleryFabDragTarget, Rect?) -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isChild = size == GalleryFabMiniSize
    val transition = updateTransition(expanded, label = "galleryShortcutVisibility")
    val progress by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(if (isChild) 140 else 180, enterDelayMillis, LinearOutSlowInEasing)
            } else {
                tween(if (isChild) 90 else 110, if (isChild) 0 else 70, FastOutSlowInEasing)
            }
        },
        label = "galleryShortcutProgress"
    ) { shown -> if (shown) 1f else 0f }

    // 등장 대기·퇴장 중에는 입력을 막고, 퇴장 완료 후 터치 영역까지 제거한다.
    if (transition.currentState || transition.targetState || transition.isRunning) {
        val context = LocalContext.current
        // 탭 실행과 드래그 selection 진입이 같은 "퐁" 링 펄스 + 탄성 펄스를 공유한다.
        var pulseTrigger by remember { mutableStateOf(0) }
        val pulseProgress = remember { Animatable(1f) }
        LaunchedEffect(pulseTrigger) {
            if (pulseTrigger > 0) {
                pulseProgress.snapTo(0f)
                pulseProgress.animateTo(1f, tween(220, easing = FastOutLinearInEasing))
            }
        }
        LaunchedEffect(isDragSelected) {
            if (isDragSelected) {
                pulseTrigger++
            }
        }
        // 68일차 1차 후속: "소심하게 커짐"이 아니라 "통 하고 튀어나옴"을 위해
        // 두 스케일을 분리한다 — heldScale은 선택 유지 중 지속되는 살짝 커진
        // 상태("잡힘"), punchScale은 선택 진입/탭 순간에만 한 번 오버슈트했다가
        // 정착하는 단발성 탄성("통!"). spring 반복 bounce는 쓰지 않는다.
        val heldScale by animateFloatAsState(
            targetValue = if (isDragSelected) 1.10f else 1f,
            animationSpec = tween(90, easing = FastOutSlowInEasing),
            label = "galleryFabHeldScale"
        )
        val punchScale = remember { Animatable(1f) }
        LaunchedEffect(pulseTrigger) {
            if (pulseTrigger > 0) {
                punchScale.snapTo(1f)
                punchScale.animateTo(
                    targetValue = 1f,
                    animationSpec = keyframes {
                        durationMillis = 200
                        1f at 0
                        1.16f at 70 using FastOutSlowInEasing
                        1f at 200 using FastOutSlowInEasing
                    }
                )
            }
        }

        // 68일차 2차 후속: "선택되었다는 시각적 확신"을 위한 동그란 선택 링.
        // 드래그 후보로 유지되는 동안은 지속적으로 보이고(dragRingAlpha), 탭
        // 처럼 후보 상태가 없는 즉시 실행에서는 같은 pulseTrigger에 얹혀
        // 짧게 나타났다 자연스럽게 사라진다(tapRingAlpha) — 체크 표시가
        // 아니라 선택 순간을 감싸는 "포옹" 이미지라 둘 다 fade로만 처리한다.
        val dragRingAlpha by animateFloatAsState(
            targetValue = if (isDragSelected) 1f else 0f,
            animationSpec = tween(if (isDragSelected) 80 else 150),
            label = "galleryFabDragRingAlpha"
        )
        val tapRingAlpha = remember { Animatable(0f) }
        LaunchedEffect(pulseTrigger) {
            if (pulseTrigger > 0) {
                tapRingAlpha.snapTo(1f)
                tapRingAlpha.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
            }
        }
        val ringAlpha = maxOf(dragRingAlpha, tapRingAlpha.value)

        if (dragTarget != null && onDragTargetBoundsChanged != null) {
            DisposableEffect(dragTarget) {
                onDispose { onDragTargetBoundsChanged(dragTarget, null) }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = offsetX, y = offsetY)
                .size(48.dp)
                .graphicsLayer {
                    alpha = progress
                    scaleX = 0.82f + 0.18f * progress
                    scaleY = scaleX
                    translationX = (originX - offsetX).toPx() * (1f - progress)
                    translationY = (originY - offsetY).toPx() * (1f - progress)
                }
                .then(modifier)
                .onGloballyPositioned { coordinates ->
                    if (dragTarget != null && onDragTargetBoundsChanged != null) {
                        val hitTestEnabled = expanded && progress >= 0.5f
                        onDragTargetBoundsChanged(
                            dragTarget,
                            if (hitTestEnabled) coordinates.boundsInWindow() else null
                        )
                    }
                }
                .clip(CircleShape)
                .clickable(
                    enabled = expanded && progress >= 0.5f,
                    role = Role.Button,
                    onClick = {
                        pulseTrigger++
                        vibrateGalleryFab(
                            context,
                            GalleryFabHapticConfirmDurationMs,
                            GalleryFabHapticConfirmAmplitude
                        )
                        onClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = heldScale * punchScale.value
                        scaleY = heldScale * punchScale.value
                    }
                    .background(backgroundColor, CircleShape)
                    .drawWithContent {
                        drawContent()
                        val baseRadius = this.size.minDimension / 2f
                        // 선택 링: 버튼 테두리 살짝 안쪽에 고정 반경으로 — 탭이든
                        // 드래그 후보 진입이든 "지금 이게 선택됨"을 또렷이 감싼다.
                        if (ringAlpha > 0f) {
                            drawCircle(
                                color = GalleryFabSelectionRingColor,
                                radius = baseRadius - 2.5.dp.toPx(),
                                alpha = ringAlpha * 0.9f,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                        // 물방울 pulse: 중심에서 빠르게 퍼졌다 옅어지고 사라짐. 한 번
                        // 터치/선택 진입당 한 번만 재생되며 링과는 색/움직임으로 구분된다.
                        if (pulseTrigger > 0 && pulseProgress.value < 1f) {
                            val ringProgress = pulseProgress.value
                            drawCircle(
                                color = GalleryFabPulseColor,
                                radius = baseRadius * (1f + 0.7f * ringProgress),
                                alpha = (1f - ringProgress) * 0.65f,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

private val PondDrawerIcon: ImageVector =
    ImageVector.Builder(
        name = "PondDrawerIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 8f)
            curveTo(6f, 6.4f, 8f, 6.4f, 10f, 8f)
            curveTo(12f, 9.6f, 14f, 9.6f, 16f, 8f)
            curveTo(17.5f, 6.8f, 19f, 6.8f, 20f, 8f)

            moveTo(4f, 12f)
            curveTo(6f, 10.4f, 8f, 10.4f, 10f, 12f)
            curveTo(12f, 13.6f, 14f, 13.6f, 16f, 12f)
            curveTo(17.5f, 10.8f, 19f, 10.8f, 20f, 12f)

            moveTo(4f, 16f)
            curveTo(6f, 14.4f, 8f, 14.4f, 10f, 16f)
            curveTo(12f, 17.6f, 14f, 17.6f, 16f, 16f)
            curveTo(17.5f, 14.8f, 19f, 14.8f, 20f, 16f)
        }
    }.build()

private val SheepDrawerIcon: ImageVector =
    ImageVector.Builder(
        name = "SheepDrawerIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6.5f, 17.5f)
            lineTo(6.5f, 20f)
            lineTo(8.2f, 20f)
            lineTo(8.2f, 17.8f)
            close()

            moveTo(14.2f, 17.8f)
            lineTo(14.2f, 20f)
            lineTo(15.9f, 20f)
            lineTo(15.9f, 17.5f)
            close()
        }

        path(fill = SolidColor(Color.Black)) {
            moveTo(6.8f, 16.8f)
            curveTo(4.7f, 16.1f, 3.6f, 14.4f, 4f, 12.4f)
            curveTo(4.4f, 10.5f, 6f, 9.3f, 7.8f, 9.5f)
            curveTo(8.5f, 7.9f, 10.1f, 7f, 11.8f, 7.3f)
            curveTo(13.5f, 7.6f, 14.6f, 8.8f, 14.8f, 10.3f)
            curveTo(16.5f, 10.4f, 17.8f, 11.7f, 17.9f, 13.4f)
            curveTo(18f, 15.6f, 16.4f, 17.1f, 14.2f, 17.2f)
            lineTo(8.5f, 17.2f)
            curveTo(8f, 17.2f, 7.4f, 17.1f, 6.8f, 16.8f)
            close()
        }

        path(fill = SolidColor(Color.Black)) {
            moveTo(17.1f, 10.3f)
            curveTo(18.8f, 10.1f, 20.2f, 11.3f, 20.3f, 13f)
            curveTo(20.4f, 14.8f, 19.1f, 16.1f, 17.5f, 16.1f)
            curveTo(17.3f, 14.6f, 17.2f, 12.2f, 17.1f, 10.3f)
            close()
        }
    }.build()

private val CheckFlagDrawerIcon: ImageVector =
    ImageVector.Builder(
        name = "CheckFlagDrawerIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 4f)
            lineTo(6.8f, 4f)
            lineTo(6.8f, 20f)
            lineTo(5f, 20f)
            close()
        }

        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6.8f, 5f)
            lineTo(18.6f, 5f)
            lineTo(18.6f, 13f)
            lineTo(6.8f, 13f)
            close()
        }

        path(fill = SolidColor(Color.Black)) {
            moveTo(8.3f, 6.5f)
            lineTo(11.4f, 6.5f)
            lineTo(11.4f, 9.1f)
            lineTo(8.3f, 9.1f)
            close()

            moveTo(14.5f, 6.5f)
            lineTo(17.1f, 6.5f)
            lineTo(17.1f, 9.1f)
            lineTo(14.5f, 9.1f)
            close()

            moveTo(11.4f, 9.1f)
            lineTo(14.5f, 9.1f)
            lineTo(14.5f, 11.6f)
            lineTo(11.4f, 11.6f)
            close()
        }
    }.build()

/** 보기 형식 dot indicator 한 줄이 차지하는 고정 높이. 페이지 콘텐츠는 이만큼을 위쪽 padding으로 더 확보해 겹치지 않는다. */
private val GALLERY_PAGE_INDICATOR_RESERVED_HEIGHT = 28.dp

/**
 * 활성 보기 목록이 바뀐 뒤 pager가 위치해야 할 새 페이지 index를 계산한다
 * (작업지시서 44절 — 보기 비활성화 edge case). 순수 함수라 Compose 없이도
 * 검증 가능하다.
 *
 * - [lastKnownFormat]이 여전히 [activeFormats]에 있으면 그 새 위치로 이동한다
 *   (중간 보기가 꺼져 인덱스가 당겨져도 같은 보기를 계속 보게 된다).
 * - 없으면(지금 보던 보기 자체가 꺼짐) [currentIndex]를 유효 범위로 clamp한다.
 * - [activeFormats]가 비어 있으면 0을 돌려준다(45절에 따라 실제로는 발생하지
 *   않아야 하는 방어적 경로).
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
 * 활성 보기가 둘 이상일 때만 그리는 점 indicator(6절) — 점이면 점답게,
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

internal data class GalleryMemoryDensityMonth(
    val yearMonth: YearMonth,
    val postcards: List<Postcard>
) {
    val count: Int
        get() = postcards.size
}

/**
 * 76일차: 기억밀도는 더 이상 여러 연도를 이어붙여 훑어보는 화면이 아니라
 * 지정한 한 해의 1~12월만 항상 보여주는 연간 그래프다(연도 이동 picker는
 * 기존에 없던 기능이라 새로 만들지 않는다). 기존 Gallery의 날짜 의미와
 * 동일하게 capturedAt을 시스템 시간대의 월로 변환하고, 기록이 없는 달도
 * 0개로 채운다(막대가 사라지지 않고 "0칸"으로 표현됨). 방문 기록이 아니라
 * 포스트카드 생성 시각만 기준으로 삼는다.
 */
internal fun memoryDensityMonthsForYear(
    postcards: List<Postcard>,
    year: Int,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<GalleryMemoryDensityMonth> {
    val grouped = postcards.groupBy { postcard ->
        YearMonth.from(
            Instant.ofEpochMilli(postcard.capturedAt).atZone(zoneId)
        )
    }

    return (1..12).map { month ->
        val yearMonth = YearMonth.of(year, month)
        GalleryMemoryDensityMonth(
            yearMonth = yearMonth,
            postcards = grouped[yearMonth].orEmpty()
        )
    }
}

private const val MEMORY_DENSITY_UNIT_POSTCARDS = 2
private const val MEMORY_DENSITY_MAX_BAR_LEVEL = 6
private const val MEMORY_DENSITY_OVERFLOW_THRESHOLD =
    MEMORY_DENSITY_UNIT_POSTCARDS * MEMORY_DENSITY_MAX_BAR_LEVEL // 12장

/** 1칸 = 엽서 2장, 최대 6칸(12장). 0장은 0칸(막대 없음, 억지 최소 높이를 주지 않는다). */
internal fun memoryDensityBarLevel(count: Int): Int =
    ((count + 1) / MEMORY_DENSITY_UNIT_POSTCARDS).coerceIn(0, MEMORY_DENSITY_MAX_BAR_LEVEL)

/** 12장(6칸) 초과분은 막대를 더 키우지 않고 위에 작은 "+" 표시로만 알린다. */
internal fun memoryDensityHasOverflow(count: Int): Boolean =
    count > MEMORY_DENSITY_OVERFLOW_THRESHOLD

/**
 * 76일차 후속(새싹형): 줄기가 있을 때(1장 이상) 그 위에 얹는 하트가
 * 줄기 단계가 높을수록 더 진하게 보이도록 하는 alpha. 표정 대신 진하기로
 * "많이 자랐다"는 인상만 살짝 보태고, 정확한 비교는 여전히 줄기 길이가
 * 담당한다.
 */
internal fun memoryDensityHeartAlpha(level: Int): Float =
    0.4f + (level.toFloat() / MEMORY_DENSITY_MAX_BAR_LEVEL) * 0.6f

private val MEMORY_DENSITY_STEM_WIDTH = 3.dp
private val MEMORY_DENSITY_STEM_UNIT_HEIGHT = 8.dp
private val MEMORY_DENSITY_PLANT_AREA_HEIGHT =
    MEMORY_DENSITY_STEM_UNIT_HEIGHT * MEMORY_DENSITY_MAX_BAR_LEVEL + 10.dp
private val MEMORY_DENSITY_OVERFLOW_MARK_HEIGHT = 14.dp

/**
 * 76일차: 기억밀도의 새 정의 — "한 해 동안 어느 달에 기억을 많이 남겼는지
 * 조용히 바라보는 화면". dashboard·통계판이 아니라 1월→12월로 흐르는 작은
 * 화단이다. 사진을 보여주거나 탭해서 상세로 들어가는 상호작용은 오늘
 * 범위가 아니다(작업지시서 23·26절) — 순수 조회 화면.
 */
@Composable
private fun GalleryDensityPage(
    postcards: List<Postcard>,
    paddingValues: PaddingValues
) {
    // 이 화면에는 연도 선택 UI가 없다 — year는 "사용자가 고른 연도"가 아니라 순수하게
    // "지금 몇 년인가"다. 그래서 앱을 켜 둔 채 연말 자정을 넘기면 제목과 12칸이 지난해에
    // 머무는 문제가 있었고, 방문 달력과 같은 자정 기준([rememberTodayDate])을 쓴다.
    val year = rememberTodayDate().year
    val months = remember(postcards, year) {
        memoryDensityMonthsForYear(postcards, year)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaperWhite)
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 20.dp,
                bottom = paddingValues.calculateBottomPadding() + 24.dp
            )
    ) {
        Text(
            text = "${year}년",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 76일차 후속(새싹형): 각 달의 줄기·구분선·얼굴을 한 Column에
        // 몰아두면 구분선이 달마다 짧게 끊겨 보인다(실기기 QA 지적) —
        // 줄기 Row와 얼굴 Row를 분리하고 그 사이에 전체 폭 구분선 하나만
        // 둬서 12개월이 하나로 이어진 선 위에 서 있는 모습으로 만든다.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            months.forEach { month ->
                GalleryMemoryDensityStem(
                    month = month,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = PaperDivider,
            thickness = 1.dp
        )

        Spacer(modifier = Modifier.height(5.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            months.forEach { month ->
                GalleryMemoryDensityFoot(
                    month = month,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 월 하나의 위쪽 절반 — overflow 표시와, 수치만큼 자란 줄기 + 줄기 위
 * 하트(진하기만 줄기 단계에 비례). 접근성 설명(월·기억 개수)은 이
 * composable에만 붙이고 아래 [GalleryMemoryDensityFoot]는 별도로 읽히지
 * 않게 한다.
 */
@Composable
private fun GalleryMemoryDensityStem(
    month: GalleryMemoryDensityMonth,
    modifier: Modifier = Modifier
) {
    val level = memoryDensityBarLevel(month.count)
    val hasOverflow = memoryDensityHasOverflow(month.count)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics {
            contentDescription = "${month.yearMonth.monthValue}월, 기억 ${month.count}개"
        }
    ) {
        Box(
            modifier = Modifier.height(MEMORY_DENSITY_OVERFLOW_MARK_HEIGHT),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (hasOverflow) {
                Text(
                    text = "+",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SunsetGold
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .height(MEMORY_DENSITY_PLANT_AREA_HEIGHT)
                .fillMaxWidth()
        ) {
            if (level > 0) {
                Text(
                    text = "♥",
                    fontSize = 11.sp,
                    color = SunsetGold.copy(alpha = memoryDensityHeartAlpha(level)),
                    modifier = Modifier.padding(bottom = 1.dp)
                )

                Box(
                    modifier = Modifier
                        .width(MEMORY_DENSITY_STEM_WIDTH)
                        .height(MEMORY_DENSITY_STEM_UNIT_HEIGHT * level)
                        .background(SunsetGold)
                )
            }
        }
    }
}

/**
 * 월 하나의 아래쪽 절반 — 고정된 무표정 얼굴과 월 숫자. 얼굴은 항상
 * 같은 모양·색("•_•", InkSecondary)이다 — 수치를 표정이나 색으로
 * 평가하지 않고, 자란 길이(위 [GalleryMemoryDensityStem])만으로 양을
 * 보여준다.
 */
@Composable
private fun GalleryMemoryDensityFoot(
    month: GalleryMemoryDensityMonth,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clearAndSetSemantics {}
    ) {
        Text(
            text = "•_•",
            fontSize = 12.sp,
            color = InkSecondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = month.yearMonth.monthValue.toString(),
            fontSize = 11.sp,
            color = InkSecondary
        )
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
 * 76일차: 3단 보기가 삭제되며 그 페이지 전용이던 연못 물리 오버레이
 * (탭/드래그 파문, [PondRippleOverlay])를 이 grid로 그대로 옮겼다.
 * 검색 결과가 없을 때의 안내([SearchEmptyState])도 3단 보기가 맡던
 * 역할을 그대로 이어받는다("엽서가 하나도 없음" 판정은 여전히 호출부인
 * pager 레벨이 먼저 처리한다).
 */
@Composable
private fun GalleryMonthlyGridPage(
    postcards: List<Postcard>,
    selectedIds: Set<Long>,
    shakeTrigger: Int,
    isPondModeOn: Boolean,
    pondController: PondController,
    paddingValues: PaddingValues,
    searchQuery: String,
    gridState: LazyGridState,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit
) {
    if (postcards.isEmpty()) {
        SearchEmptyState(
            query = searchQuery.trim(),
            paddingValues = paddingValues
        )
        return
    }

    val monthSections = remember(postcards) {
        monthSectionsFor(postcards)
    }

    var originInWindow by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaperWhite)
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
            PondRippleOverlay(
                controller = pondController,
                originInWindow = originInWindow,
                modifier = Modifier.fillMaxSize()
            )
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = paddingValues.calculateTopPadding() + 14.dp,
                bottom = paddingValues.calculateBottomPadding() + 88.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            monthSections.forEach { section ->
                item(
                    key = "month_${section.yearMonth}",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    GalleryMonthHeader(
                        yearMonth = section.yearMonth,
                        postcardCount = section.postcards.size
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
                        }
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
 * 76일차: 3단 보기가 삭제되며 그 페이지 전용이던 연못 모드가 이 grid로
 * 이식됐다 — 연못 모드가 켜졌을 때만 물리·기울임 연출이 붙은 무거운
 * [StampCard]로 바꿔 그린다(평상시 렌더링 비용은 그대로 유지).
 */
@Composable
private fun GalleryMonthlyGridItem(
    postcard: Postcard,
    isSelected: Boolean,
    shakeTrigger: Int,
    isPondModeOn: Boolean,
    pondController: PondController,
    onClick: () -> Unit,
    onLongClick: () -> Unit
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
            dateLabelOverride = dayLabel,
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        StampCardContent(
            postcard = postcard,
            isSelected = isSelected,
            dateLabelOverride = dayLabel,
            modifier = Modifier.combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
        )
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
    paddingValues: PaddingValues
) {
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
                text = "'$query' 관련 엽서는 아직 없어요.\n다른 기억의 조각을 검색해 보세요.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BrutalBlack,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

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

@Composable
private fun GalleryMonthHeader(
    yearMonth: YearMonth,
    postcardCount: Int
) {
    val label =
        remember(yearMonth) {
            yearMonth.format(monthHeaderLabelFormatter)
        }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaperField)
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = "$label, 엽서 ${postcardCount}장"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${postcardCount}장",
                fontSize = 12.sp,
                color = InkSecondary
            )
        }

        HorizontalDivider(color = PaperDivider, thickness = 1.dp)
    }
}
