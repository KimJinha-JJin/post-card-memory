package com.postcardmemory.ui.gallery

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.postcardmemory.R
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.components.StampCard
import com.postcardmemory.ui.components.StampCardContent
import com.postcardmemory.ui.components.PinkingPhotoShape
import com.postcardmemory.ui.components.PostcardDateFormat
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.BrutalWhite
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
import java.io.File
import coil.compose.AsyncImage
import kotlin.math.sqrt

private val PlayModeSaver = Saver<GalleryPlayMode, String>(
    save = { it.name },
    restore = { GalleryPlayMode.valueOf(it) }
)

private val SortOrderSaver = Saver<GallerySortOrder, String>(
    save = { it.name },
    restore = { GallerySortOrder.valueOf(it) }
)

private val PageFormatSaver = Saver<GalleryPageFormat, String>(
    save = { it.name },
    restore = { saved ->
        runCatching { GalleryPageFormat.valueOf(saved) }
            .getOrDefault(GalleryPageFormat.THREE_COLUMN)
    }
)

/**
 * 활성 보기 형식 집합을 이름 목록 문자열로 저장/복원한다. [GalleryPageFormat.THREE_COLUMN]은
 * 최소 1개 보기를 보장하는 안전 보기라 복원 결과에 항상 포함시킨다 —
 * 저장된 문자열이 비어있거나, 알 수 없는 이름이 섞여 있거나(향후 enum
 * 변경), 실수로 3단 보기가 빠진 채 저장됐어도 항상 최소 활성 보기를
 * 보장한다.
 */
private val ActivePageFormatsSaver = Saver<Set<GalleryPageFormat>, String>(
    save = { formats -> formats.joinToString(",") { it.name } },
    restore = { saved ->
        val restored = saved
            .split(",")
            .mapNotNull { name ->
                runCatching { GalleryPageFormat.valueOf(name) }.getOrNull()
            }
            .toSet()

        restored + GalleryPageFormat.THREE_COLUMN
    }
)

private val CalendarVisibleMonthSaver = Saver<YearMonth, String>(
    save = { it.toString() },
    restore = { saved ->
        runCatching { YearMonth.parse(saved) }.getOrDefault(YearMonth.now())
    }
)

private val CalendarSelectedDateSaver = Saver<LocalDate?, String>(
    save = { it?.toString() ?: "" },
    restore = { saved ->
        saved.takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
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

    var activePageFormats by rememberSaveable(stateSaver = ActivePageFormatsSaver) {
        mutableStateOf(setOf(GalleryPageFormat.THREE_COLUMN))
    }

    var currentPageFormat by rememberSaveable(stateSaver = PageFormatSaver) {
        mutableStateOf(GalleryPageFormat.THREE_COLUMN)
    }

    // 3단 보기의 스크롤 위치를 여기서 hoist해 페이지 스와이프로 잠시 화면
    // 밖에 나갔다 돌아와도 위치가 초기화되지 않게 한다.
    val gridState = rememberLazyGridState()

    // 월별 보기(62일차 2차)는 3단 보기와 완전히 다른 LazyVerticalGrid
    // 인스턴스라 같은 gridState를 공유하면 안 된다 — 별도로 hoist한다.
    val monthlyGridState = rememberLazyGridState()

    // 타임라인 보기(62일차 3차)도 자신만의 LazyColumn을 쓰므로 별도 hoist.
    val timelineListState = rememberLazyListState()

    // 캘린더 보기(62일차 4차)는 월 grid가 고정 높이라 Lazy 없이 일반
    // Column + verticalScroll을 쓰지만, 스크롤 위치는 다른 페이지와 동일한
    // 이유로 여기서 hoist한다.
    val calendarScrollState = rememberScrollState()

    // 우표 보기 역시 별도 LazyVerticalGrid 인스턴스이므로 3단/월별 보기와
    // 스크롤 상태를 공유하지 않는다.
    val stampGridState = rememberLazyGridState()

    // 기억 밀도 보기는 연도별 월 점을 세로로 훑으므로 전용 list state를 쓴다.
    val densityListState = rememberLazyListState()
    var densitySelectedMonthKey by rememberSaveable { mutableStateOf<String?>(null) }

    fun toggleActivePageFormat(format: GalleryPageFormat) {
        if (format == GalleryPageFormat.THREE_COLUMN) {
            // 최소 1개 보기를 보장하는 안전 보기라 끌 수 없다(45절).
            return
        }

        activePageFormats = if (format in activePageFormats) {
            activePageFormats - format
        } else {
            activePageFormats + format
        }
    }

    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    var isSearchActive by rememberSaveable {
        mutableStateOf(false)
    }

    var viewMenuExpanded by remember {
        mutableStateOf(false)
    }

    var sortMenuExpanded by remember {
        mutableStateOf(false)
    }

    // 검색·정렬은 3단 보기 top bar에만 노출된다(위 topBar 참고). 다른 보기
    // 형식으로 넘어간 사이에도 이 상태들이 true로 남아있으면 3단 보기로
    // 돌아왔을 때 검색창이나 드롭다운이 탭 없이 저절로 열려 보인다 — 3단
    // 보기를 벗어나는 순간 정리한다. 보기 형식 메뉴는 모든 페이지의 공통
    // 진입점이므로 여기서 닫지 않는다.
    LaunchedEffect(currentPageFormat) {
        if (currentPageFormat != GalleryPageFormat.THREE_COLUMN) {
            isSearchActive = false
            searchQuery = ""
            sortMenuExpanded = false
        }
    }

    var fabMenuExpanded by remember {
        mutableStateOf(false)
    }

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
                        Text(
                            text = "포스트카드 메모리",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // 검색·정렬은 3단 보기 안에서만 의미가 있다. 보기 형식
                        // 관리는 어느 페이지에서도 다음 swipe 대상을 바꿀 수
                        // 있어야 하므로 기존 우측 상단 위치에 항상 노출한다.
                        if (currentPageFormat == GalleryPageFormat.THREE_COLUMN) {
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

                        Box {
                            IconButton(
                                onClick = {
                                    viewMenuExpanded = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "보기 형식 관리",
                                    tint = InkSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = viewMenuExpanded,
                                onDismissRequest = {
                                    viewMenuExpanded = false
                                },
                                shape = RectangleShape,
                                containerColor = PaperSurface
                            ) {
                                GalleryPageFormat.entries.forEach { format ->
                                    GalleryPageFormatMenuItem(
                                        format = format,
                                        checked = format in activePageFormats,
                                        locked = format == GalleryPageFormat.THREE_COLUMN,
                                        onToggle = {
                                            toggleActivePageFormat(format)
                                        }
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
                                selectedIds = selectedIds,
                                paddingValues = pageContentPaddingValues,
                                listState = densityListState,
                                selectedMonth = densitySelectedMonthKey?.let(YearMonth::parse),
                                onMonthSelected = { yearMonth ->
                                    densitySelectedMonthKey = yearMonth?.toString()
                                },
                                onItemClick = ::handleItemClick,
                                onItemLongClick = ::handleItemLongClick
                            )

                        GalleryPageFormat.STAMP ->
                            GalleryStampPage(
                                postcards = displayedPostcards,
                                selectedIds = selectedIds,
                                paddingValues = pageContentPaddingValues,
                                gridState = stampGridState,
                                onItemClick = ::handleItemClick,
                                onItemLongClick = ::handleItemLongClick
                            )

                        GalleryPageFormat.CALENDAR ->
                            GalleryCalendarPage(
                                postcards = displayedPostcards,
                                selectedIds = selectedIds,
                                paddingValues = pageContentPaddingValues,
                                scrollState = calendarScrollState,
                                onItemClick = ::handleItemClick,
                                onItemLongClick = ::handleItemLongClick
                            )

                        GalleryPageFormat.MONTHLY ->
                            GalleryMonthlyGridPage(
                                postcards = displayedPostcards,
                                selectedIds = selectedIds,
                                paddingValues = pageContentPaddingValues,
                                gridState = monthlyGridState,
                                onItemClick = ::handleItemClick,
                                onItemLongClick = ::handleItemLongClick
                            )

                        GalleryPageFormat.TIMELINE ->
                            GalleryTimelinePage(
                                postcards = displayedPostcards,
                                selectedIds = selectedIds,
                                paddingValues = pageContentPaddingValues,
                                listState = timelineListState,
                                onItemClick = ::handleItemClick,
                                onItemLongClick = ::handleItemLongClick
                            )

                        GalleryPageFormat.THREE_COLUMN, null ->
                            GalleryThreeColumnPage(
                                displayedPostcards = displayedPostcards,
                                selectedIds = selectedIds,
                                shakeTrigger = shakeTrigger,
                                isPondModeOn = isPondModeOn,
                                pondController = pondController,
                                paddingValues = pageContentPaddingValues,
                                searchQuery = searchQuery,
                                gridState = gridState,
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
            exit = fadeOut(tween(120)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
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
private val GalleryFabAnchorSize = 56.dp
private val GalleryFabPrimarySize = 52.dp
private val GalleryFabMiniSize = 40.dp

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

    val anchorRotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        label = "galleryFabAnchorRotation"
    )

    Box(modifier = modifier) {
        // 특별한 갤러리 3종 — 서로 가까이 묶어 하나의 작은 하위 군집으로
        // 읽히게 하되, 이를 감싸는 Card/box/capsule은 두지 않는다(거리와
        // 배치만으로 그룹을 표현).
        GalleryFabShortcut(
            expanded = expanded,
            offsetX = (-52).dp,
            offsetY = (-148).dp,
            size = GalleryFabMiniSize,
            backgroundColor = if (playMode == GalleryPlayMode.POND) SunsetGold else BrutalWhite,
            onClick = { onPlayModeSelected(GalleryPlayMode.POND) }
        ) {
            Icon(
                imageVector = PondDrawerIcon,
                contentDescription = "엽서의 연못",
                tint = InkSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        GalleryFabShortcut(
            expanded = expanded,
            offsetX = (-100).dp,
            offsetY = (-128).dp,
            size = GalleryFabMiniSize,
            backgroundColor = if (playMode == GalleryPlayMode.SHEEP_RANCH) SunsetGold else BrutalWhite,
            onClick = { onPlayModeSelected(GalleryPlayMode.SHEEP_RANCH) }
        ) {
            Icon(
                imageVector = SheepDrawerIcon,
                contentDescription = "양떼목장",
                tint = InkSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        GalleryFabShortcut(
            expanded = expanded,
            offsetX = (-136).dp,
            offsetY = (-88).dp,
            size = GalleryFabMiniSize,
            backgroundColor = if (playMode == GalleryPlayMode.RACE) SunsetGold else BrutalWhite,
            onClick = { onPlayModeSelected(GalleryPlayMode.RACE) }
        ) {
            Icon(
                imageVector = CheckFlagDrawerIcon,
                contentDescription = "엽서 쫑쫑컵",
                tint = InkSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        // 엽서 생성 / 미래 우체통 — 주요 바로가기, 작은 군집보다 크고
        // anchor에 더 가깝게 배치해 위계를 준다.
        GalleryFabShortcut(
            expanded = expanded,
            offsetX = (-72).dp,
            offsetY = (-56).dp,
            size = GalleryFabPrimarySize,
            backgroundColor = BrutalWhite,
            onClick = onNavigateToFutureMailbox
        ) {
            Icon(
                imageVector = Icons.Default.MailOutline,
                contentDescription = "미래 우체통",
                tint = InkSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        GalleryFabShortcut(
            expanded = expanded,
            offsetX = 0.dp,
            offsetY = (-76).dp,
            size = GalleryFabPrimarySize,
            backgroundColor = BrutalWhite,
            onClick = onNavigateToCamera
        ) {
            Image(
                painter = painterResource(R.drawable.ic_camera_button),
                contentDescription = "카메라",
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
            )
        }

        // + anchor — 전체 펼침/접힘의 기준점. 펼쳐지면 45도 회전해 자연스럽게
        // 닫기(×) 표시로 읽히게 한다(새 아이콘을 추가하지 않는다).
        FloatingActionButton(
            onClick = onToggle,
            containerColor = BrutalWhite,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 3.dp,
                pressedElevation = 6.dp
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(GalleryFabAnchorSize)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (expanded) "바로가기 닫기" else "바로가기 열기",
                tint = InkPrimary,
                modifier = Modifier
                    .size(28.dp)
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
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.6f),
        exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.6f),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = offsetX, y = offsetY)
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = backgroundColor,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            ),
            modifier = Modifier.size(size)
        ) {
            content()
        }
    }
}

@Composable
private fun GalleryPageFormatMenuItem(
    format: GalleryPageFormat,
    checked: Boolean,
    locked: Boolean,
    onToggle: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = format.label,
                color = InkPrimary,
                fontWeight = if (checked) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
            )
        },
        trailingIcon = {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SunsetGold
                )
            }
        },
        enabled = !locked,
        onClick = onToggle
    )
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

/**
 * 아직 구현되지 않은 보기 형식의 최소 placeholder(21절 — 구조 확인용
 * 최소 placeholder만 두고 대량으로 가짜 UI를 만들지 않는다). 카드나 배경
 * 박스 없이 안내 문구 하나만 둔다.
 */
@Composable
private fun GalleryComingSoonPage(
    format: GalleryPageFormat,
    paddingValues: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaperWhite)
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${format.label}는 곧 만나볼 수 있어요.",
            fontSize = 15.sp,
            color = InkSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

private val STAMP_PAPER_PADDING = 8.dp

/**
 * 62일차 5차: 저장된 기억을 기념우표처럼 감상하는 2열 보기. 기존 3단
 * 보기보다 사진을 크게 두고, 카드나 panel을 덧씌우지 않은 채 우표 자체만
 * 반복 배치한다. Lazy grid라 화면에 보이는 항목만 구성·로딩한다.
 */
@Composable
private fun GalleryStampPage(
    postcards: List<Postcard>,
    selectedIds: Set<Long>,
    paddingValues: PaddingValues,
    gridState: LazyGridState,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = paddingValues.calculateTopPadding() + 16.dp,
            bottom = paddingValues.calculateBottomPadding() + 88.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaperWhite)
    ) {
        lazyGridItems(
            items = postcards,
            key = { postcard -> postcard.id }
        ) { postcard ->
            GalleryStampGridItem(
                postcard = postcard,
                isSelected = postcard.id in selectedIds,
                onClick = { onItemClick(postcard.id) },
                onLongClick = { onItemLongClick(postcard.id) }
            )
        }
    }
}

/**
 * 우표 한 장. [PinkingPhotoShape] 바깥선 안에 종이 여백과 원본 thumbnail을
 * 함께 두어 한 객체로 읽히게 한다. 선택은 우표를 가리는 overlay 대신 기존
 * Gallery의 작은 coral 점만 사용한다.
 */
@Composable
private fun GalleryStampGridItem(
    postcard: Postcard,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(PinkingPhotoShape)
                .background(PaperSurface)
                .padding(STAMP_PAPER_PADDING)
        ) {
            AsyncImage(
                model = File(postcard.imagePath),
                contentDescription = postcard.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(14.dp)
                        .background(
                            color = BrutalCoral,
                            shape = CircleShape
                        )
                )
            }
        }

        Text(
            text = PostcardDateFormat.formatIso(postcard.capturedAt),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = GraphiteAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp)
        )
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
 * 기존 Gallery의 날짜 의미와 동일하게 capturedAt을 시스템 시간대의 월로
 * 변환한다. 첫 기록 연도부터 마지막 기록 연도까지 매년 12개월을 모두 채워
 * 기록이 없던 기간도 화면에서 사라지지 않게 한다.
 */
internal fun memoryDensityMonthsFor(
    postcards: List<Postcard>,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<GalleryMemoryDensityMonth> {
    if (postcards.isEmpty()) return emptyList()

    val grouped = postcards.groupBy { postcard ->
        YearMonth.from(
            Instant.ofEpochMilli(postcard.capturedAt).atZone(zoneId)
        )
    }
    val firstYear = grouped.keys.minOf { it.year }
    val lastYear = grouped.keys.maxOf { it.year }

    // 밀도 차트의 시간축은 정렬 메뉴와 무관하게 과거→현재로 고정한다.
    // 같은 위치가 늘 같은 시기를 뜻해야 분포 변화를 비교하기 쉽다.
    return (firstYear..lastYear).flatMap { year ->
        (1..12).map { month ->
            val yearMonth = YearMonth.of(year, month)
            GalleryMemoryDensityMonth(
                yearMonth = yearMonth,
                postcards = grouped[yearMonth].orEmpty()
            )
        }
    }
}

internal fun memoryDensityIntensity(count: Int, maxCount: Int): Float {
    if (count <= 0) return 0f
    return (count.toFloat() / maxCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
}

internal fun selectedMemoryDensityMonth(
    months: List<GalleryMemoryDensityMonth>,
    requested: YearMonth?
): GalleryMemoryDensityMonth? =
    months.firstOrNull { month ->
        month.yearMonth == requested && month.count > 0
    }

/**
 * 62일차 6차: 사진을 감상하는 대신 기록이 몰린 시기와 빈 시기를 멀리서
 * 보는 월 단위 기억 밀도 보기. 초록 사각 격자 대신 따뜻한 원형 기억 점의
 * 크기와 농도로 분포를 표현한다.
 */
@Composable
private fun GalleryDensityPage(
    postcards: List<Postcard>,
    selectedIds: Set<Long>,
    paddingValues: PaddingValues,
    listState: LazyListState,
    selectedMonth: YearMonth?,
    onMonthSelected: (YearMonth?) -> Unit,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit
) {
    val densityMonths = remember(postcards) {
        memoryDensityMonthsFor(postcards)
    }
    val yearGroups = remember(densityMonths) {
        densityMonths.groupBy { it.yearMonth.year }.toList()
    }
    val maxCount = densityMonths.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val selectedBucket = selectedMemoryDensityMonth(densityMonths, selectedMonth)

    LaunchedEffect(selectedMonth, selectedBucket) {
        if (selectedMonth != null && selectedBucket == null) {
            onMonthSelected(null)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = paddingValues.calculateTopPadding() + 16.dp,
            bottom = paddingValues.calculateBottomPadding() + 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(26.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaperWhite)
    ) {
        lazyColumnItems(
            items = yearGroups,
            key = { (year, _) -> year }
        ) { (year, months) ->
            Column {
                Text(
                    text = year.toString(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                months.chunked(6).forEach { halfYear ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        halfYear.forEach { month ->
                            GalleryMemoryDensityDot(
                                month = month,
                                maxCount = maxCount,
                                isSelected = month.yearMonth == selectedBucket?.yearMonth,
                                onClick = {
                                    if (month.count > 0) {
                                        onMonthSelected(
                                            month.yearMonth.takeUnless { it == selectedMonth }
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (selectedBucket != null && selectedBucket.yearMonth.year == year) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        selectedBucket.postcards.forEach { postcard ->
                            StampCardContent(
                                postcard = postcard,
                                isSelected = postcard.id in selectedIds,
                                dateLabelOverride = "",
                                modifier = Modifier
                                    .width(TIMELINE_PHOTO_WIDTH)
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { onItemClick(postcard.id) },
                                        onLongClick = { onItemLongClick(postcard.id) }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryMemoryDensityDot(
    month: GalleryMemoryDensityMonth,
    maxCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val intensity = memoryDensityIntensity(month.count, maxCount)
    val dotSize = if (month.count == 0) 6.dp else (10f + 22f * sqrt(intensity)).dp
    val dotColor = when {
        isSelected -> BrutalCoral
        month.count == 0 -> PaperDivider.copy(alpha = 0.7f)
        else -> SunsetGold.copy(alpha = 0.35f + 0.65f * intensity)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .then(
                if (month.count > 0) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .semantics {
                contentDescription =
                    "${month.yearMonth.year}년 ${month.yearMonth.monthValue}월, 기억 ${month.count}개"
            }
            .padding(vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(
                        color = dotColor,
                        shape = CircleShape
                    )
            )
        }

        Text(
            text = month.yearMonth.monthValue.toString(),
            fontSize = 10.sp,
            color = InkSecondary
        )
    }
}

/**
 * 항상 3열 grid를 보여주는 "3단 보기" 페이지. 검색·정렬 적용 결과
 * ([displayedPostcards])와 진짜 빈
 * 상태(엽서가 하나도 없음) 판정을 호출부(pager 레벨)가 먼저 처리한다 —
 * 빈 상태는 월별 보기 등 다른 보기와 공통으로 써야 해서(30절) 특정 페이지
 * 안에 가두지 않는다. 이 페이지는 "검색 결과가 없음"만 자기 몫으로 남긴다.
 */
@Composable
private fun GalleryThreeColumnPage(
    displayedPostcards: List<Postcard>,
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
    if (displayedPostcards.isEmpty()) {
        SearchEmptyState(
            query = searchQuery.trim(),
            paddingValues = paddingValues
        )
    } else {
        GalleryGrid(
            postcards = displayedPostcards,
            selectedIds = selectedIds,
            shakeTrigger = shakeTrigger,
            isPondModeOn = isPondModeOn,
            pondController = pondController,
            paddingValues = paddingValues,
            gridState = gridState,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick
        )
    }
}

/** 월별 보기 grid 셀에 쓰는 "일(day)만" 표기 — 월 헤더가 이미 연/월을 보여준다. */
private val monthlyGridDayLabelFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd")

/**
 * 62일차 2차: 월별로 묶어 보여주는 grid 보기. 월마다 둥근 카드로 감싸지
 * 않고(작업지시서 22절), 기존 [GalleryMonthHeader]("세부 기록 보기"의 날짜
 * 헤더와 동일한 컴포넌트, 배경색만 있는 평면 헤더)를 그대로 재사용해
 * 하나의 [LazyVerticalGrid] 안에서 전체 폭 헤더와 3열 썸네일을 번갈아
 * 그린다 — `LazyColumn` 안에 `LazyVerticalGrid`를 중첩하지 않고
 * `item(span = { GridItemSpan(maxLineSpan) })`으로 헤더에만 전체 폭을
 * 줘서 스크롤 컨테이너를 하나로 유지한다.
 */
@Composable
private fun GalleryMonthlyGridPage(
    postcards: List<Postcard>,
    selectedIds: Set<Long>,
    paddingValues: PaddingValues,
    gridState: LazyGridState,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit
) {
    val monthSections = remember(postcards) {
        monthSectionsFor(postcards)
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
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaperWhite)
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

/**
 * 월별 보기의 썸네일 한 칸. 3단 보기의 [StampCard]는 연못 모드 물리·기울임
 * 연출이 붙은 무거운 wrapper라 재사용하지 않고, 실제 시각 요소(사진·선택
 * 표시·뒷면 편지 배지)만 그리는 [StampCardContent]를 가져와 날짜 자리만
 * "일(day)"로 바꾼다 — 사진 렌더링 자체(테두리 모양, crop)는 3단 보기와
 * 완전히 같은 컴포넌트를 공유한다.
 */
@Composable
private fun GalleryMonthlyGridItem(
    postcard: Postcard,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dayLabel = remember(postcard.capturedAt) {
        Instant.ofEpochMilli(postcard.capturedAt)
            .atZone(ZoneId.systemDefault())
            .format(monthlyGridDayLabelFormatter)
    }

    StampCardContent(
        postcard = postcard,
        isSelected = isSelected,
        dateLabelOverride = dayLabel,
        modifier = Modifier.combinedClickable(
            // 3단 보기가 쓰는 StampCard 컴포저블의 연못 모드 꺼짐 분기와
            // 동일하게 기본 Material 리플을 끈다 — 우표를 늘어놓은 평면
            // 문법과 리플이 섞이지 않게.
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick
        )
    )
}

private val TIMELINE_MARKER_COLUMN_WIDTH = 20.dp
private val TIMELINE_DOT_SIZE = 10.dp
private val TIMELINE_LINE_WIDTH = 2.dp
private val TIMELINE_ENTRY_BOTTOM_SPACING = 28.dp
private val TIMELINE_PHOTO_WIDTH = 96.dp

/**
 * 62일차 3차: 기억이 흘러온 순서를 보여주는 타임라인 보기. 항목마다 새
 * Card를 만들지 않고(23절), 날짜 + 점 + 연결선 + 사진만으로 구성한다.
 * 같은 날짜의 여러 postcard는 [daySectionsFor]로 자연스럽게 한 항목에
 * 묶는다(DB 변경 없음). 마지막 항목 아래로는 선을 그리지 않는다.
 */
@Composable
private fun GalleryTimelinePage(
    postcards: List<Postcard>,
    selectedIds: Set<Long>,
    paddingValues: PaddingValues,
    listState: LazyListState,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit
) {
    val daySections = remember(postcards) {
        daySectionsFor(postcards)
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = paddingValues.calculateTopPadding() + 14.dp,
            bottom = paddingValues.calculateBottomPadding() + 88.dp
        ),
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaperWhite)
    ) {
        itemsIndexed(
            items = daySections,
            key = { _, section -> "day_${section.date}" }
        ) { index, section ->
            GalleryTimelineEntry(
                section = section,
                isLast = index == daySections.lastIndex,
                selectedIds = selectedIds,
                onItemClick = onItemClick,
                onItemLongClick = onItemLongClick
            )
        }
    }
}

@Composable
private fun GalleryTimelineEntry(
    section: GalleryDaySection,
    isLast: Boolean,
    selectedIds: Set<Long>,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit
) {
    val dayLabel = remember(section.date) {
        section.date.format(timelineDayLabelFormatter)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(TIMELINE_MARKER_COLUMN_WIDTH)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(TIMELINE_DOT_SIZE)
                    .background(
                        color = SunsetGold,
                        shape = CircleShape
                    )
            )

            if (!isLast) {
                Box(
                    // fillMaxHeight()는 Column의 "전체" 들어온 높이 제약에
                    // 맞추려 해서 앞선 dot 높이만큼 밖으로 넘친다(Column의
                    // 비-weight 자식은 서로 예산을 나눠 쓰지 않음) — weight(1f)로
                    // dot이 쓰고 남은 높이만 정확히 채운다.
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(TIMELINE_LINE_WIDTH)
                        .weight(1f)
                        .background(PaperDivider)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = TIMELINE_ENTRY_BOTTOM_SPACING)
        ) {
            Text(
                text = dayLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                section.postcards.forEach { postcard ->
                    StampCardContent(
                        postcard = postcard,
                        isSelected = postcard.id in selectedIds,
                        // 날짜는 이미 위 dayLabel 하나로 표시하므로 사진마다
                        // 반복하지 않는다(22절과 같은 원칙 — 상위에 표시된
                        // 날짜를 항목마다 되풀이하지 않는다).
                        dateLabelOverride = "",
                        modifier = Modifier
                            .width(TIMELINE_PHOTO_WIDTH)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onItemClick(postcard.id) },
                                onLongClick = { onItemLongClick(postcard.id) }
                            )
                    )
                }
            }
        }
    }
}

private val calendarMonthLabelFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy년 M월")
private val CALENDAR_WEEKDAY_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")
private val CALENDAR_DAY_CIRCLE_SIZE = 30.dp
private val CALENDAR_DAY_DOT_SIZE = 4.dp

/**
 * 이번 달의 날짜 칸 목록을 만든다. 1일 이전은 일요일 시작 기준으로 빈
 * 칸(null)을 채우고, 마지막 주도 7의 배수가 되도록 뒤를 null로 채운다 —
 * 실제 존재하지 않는 날짜는 그리지 않되 요일 정렬은 항상 유지한다.
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
 * 62일차 4차: 날짜에서 기억으로 들어가는 캘린더 보기. 월 grid는 고정
 * 높이라 Lazy 없이 일반 Column + verticalScroll을 쓴다(24절). 날짜 칸
 * 안에는 사진을 억지로 넣지 않고 작은 점 하나로 "이 날 기록 있음"만
 * 표시하고(24절), 날짜를 선택하면 그 날의 사진을 grid 아래에 기존
 * StampCardContent로 보여준다 — 새 modal/bottom sheet 없이 같은 페이지
 * 안에서 기존 클릭(onItemClick → 상세 화면 이동) 경로를 그대로 재사용한다.
 */
@Composable
private fun GalleryCalendarPage(
    postcards: List<Postcard>,
    selectedIds: Set<Long>,
    paddingValues: PaddingValues,
    scrollState: ScrollState,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit
) {
    var visibleMonth by rememberSaveable(stateSaver = CalendarVisibleMonthSaver) {
        mutableStateOf(YearMonth.now())
    }

    var selectedDate by rememberSaveable(stateSaver = CalendarSelectedDateSaver) {
        mutableStateOf<LocalDate?>(null)
    }

    val postcardsByDate = remember(postcards) {
        daySectionsFor(postcards).associate { it.date to it.postcards }
    }

    val today = remember { LocalDate.now() }
    val cells = remember(visibleMonth) { calendarCellsFor(visibleMonth) }
    val selectedDayPostcards = selectedDate?.let { postcardsByDate[it] }.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaperWhite)
            .verticalScroll(scrollState)
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 14.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    selectedDate = null
                    visibleMonth = visibleMonth.minusMonths(1)
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "이전 달",
                    tint = InkSecondary
                )
            }

            Text(
                text = visibleMonth.format(calendarMonthLabelFormatter),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    selectedDate = null
                    visibleMonth = visibleMonth.plusMonths(1)
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "다음 달",
                    tint = InkSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            CALENDAR_WEEKDAY_LABELS.forEach { label ->
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = InkSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    GalleryCalendarDayCell(
                        date = date,
                        isToday = date == today,
                        isSelected = date != null && date == selectedDate,
                        hasPostcards = date != null && postcardsByDate.containsKey(date),
                        onClick = {
                            selectedDate = if (selectedDate == date) null else date
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (selectedDayPostcards.isNotEmpty()) {
            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = selectedDate?.format(timelineDayLabelFormatter).orEmpty(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                selectedDayPostcards.forEach { postcard ->
                    StampCardContent(
                        postcard = postcard,
                        isSelected = postcard.id in selectedIds,
                        dateLabelOverride = "",
                        modifier = Modifier
                            .width(TIMELINE_PHOTO_WIDTH)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onItemClick(postcard.id) },
                                onLongClick = { onItemLongClick(postcard.id) }
                            )
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(paddingValues.calculateBottomPadding() + 88.dp)
        )
    }
}

/**
 * 날짜 칸 하나. 사진을 넣지 않고 날짜 숫자 + (기록이 있으면) 작은 점만
 * 그린다(24절 — 작은 화면에서 날짜·thumbnail·선택 상태가 서로 경쟁하지
 * 않게). 선택된 날짜는 원형 배경으로, 오늘은 굵은 글씨로 구분한다.
 */
@Composable
private fun GalleryCalendarDayCell(
    date: LocalDate?,
    isToday: Boolean,
    isSelected: Boolean,
    hasPostcards: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(
                if (date != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(CALENDAR_DAY_CIRCLE_SIZE)
                        .background(
                            color = if (isSelected) SunsetGold else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 13.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) PaperSurface else InkPrimary
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Box(
                    modifier = Modifier
                        .size(CALENDAR_DAY_DOT_SIZE)
                        .background(
                            color = if (hasPostcards) SunsetGold else Color.Transparent,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun GalleryGrid(
    postcards: List<Postcard>,
    selectedIds: Set<Long>,
    shakeTrigger: Int,
    isPondModeOn: Boolean,
    pondController: PondController,
    paddingValues: PaddingValues,
    gridState: LazyGridState,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit
) {
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
            lazyGridItems(
                items = postcards,
                key = { postcard ->
                    postcard.id
                }
            ) { postcard ->
                StampCard(
                    postcard = postcard,
                    isSelected = postcard.id in selectedIds,
                    shakeTrigger = shakeTrigger,
                    isPondModeOn = isPondModeOn,
                    pondController = if (isPondModeOn) pondController else null,
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

@Composable
private fun GalleryDetailList(
    postcards: List<Postcard>,
    selectedIds: Set<Long>,
    paddingValues: PaddingValues,
    listState: LazyListState,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit
) {
    val monthSections = remember(postcards) {
        monthSectionsFor(postcards)
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = paddingValues.calculateBottomPadding() + 88.dp
        ),
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaperWhite)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    )
            ) {
                Text(
                    text = "날짜",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GraphiteAccent,
                    modifier = Modifier.width(96.dp)
                )

                Text(
                    text = "내용",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GraphiteAccent
                )
            }

            HorizontalDivider(color = SurfaceGray, thickness = 1.dp)
        }

        monthSections.forEach { section ->
            item(key = "month_${section.yearMonth}") {
                GalleryMonthHeader(
                    yearMonth = section.yearMonth,
                    postcardCount = section.postcards.size
                )
            }

            lazyColumnItems(
                items = section.postcards,
                key = { postcard ->
                    postcard.id
                }
            ) { postcard ->
                PostcardDetailRow(
                    postcard = postcard,
                    isSelected = postcard.id in selectedIds,
                    onClick = {
                        onItemClick(postcard.id)
                    },
                    onLongClick = {
                        onItemLongClick(postcard.id)
                    }
                )

                HorizontalDivider(color = SurfaceGray, thickness = 1.dp)
            }
        }
    }
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

private data class GalleryDaySection(
    val date: LocalDate,
    val postcards: List<Postcard>
)

/** 연도 경계에서도 헷갈리지 않도록 월별 헤더와 동일하게 연도를 항상 포함한다. */
private val timelineDayLabelFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy년 M월 d일")

/**
 * [monthSectionsFor]와 같은 방식(이미 정렬된 순서를 그대로 따르는
 * `LinkedHashMap` 그룹핑)으로, 같은 날짜의 여러 postcard를 하나의 타임라인
 * 항목으로 자연스럽게 묶는다(23절) — 새 날짜 필드나 DB 변경 없음.
 */
private fun daySectionsFor(
    postcards: List<Postcard>
): List<GalleryDaySection> {
    val grouped = LinkedHashMap<LocalDate, MutableList<Postcard>>()

    postcards.forEach { postcard ->
        val date =
            Instant.ofEpochMilli(postcard.capturedAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

        grouped.getOrPut(date) { mutableListOf() }.add(postcard)
    }

    return grouped.map { (date, postcardsOnDate) ->
        GalleryDaySection(
            date = date,
            postcards = postcardsOnDate
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
