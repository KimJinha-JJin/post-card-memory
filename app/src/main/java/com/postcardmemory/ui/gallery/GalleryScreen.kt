package com.postcardmemory.ui.gallery

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.postcardmemory.R
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.components.StampCard
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
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.sqrt

private val ViewModeSaver = Saver<GalleryViewMode, String>(
    save = { it.name },
    restore = { GalleryViewMode.valueOf(it) }
)

private val PlayModeSaver = Saver<GalleryPlayMode, String>(
    save = { it.name },
    restore = { GalleryPlayMode.valueOf(it) }
)

private val SortOrderSaver = Saver<GallerySortOrder, String>(
    save = { it.name },
    restore = { GallerySortOrder.valueOf(it) }
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
    val isPlayModeOn = playMode != GalleryPlayMode.NONE

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

    var viewMode by rememberSaveable(stateSaver = ViewModeSaver) {
        mutableStateOf(GalleryViewMode.COMPACT_GRID)
    }

    var sortOrder by rememberSaveable(stateSaver = SortOrderSaver) {
        mutableStateOf(GallerySortOrder.NEWEST)
    }

    var playModeMenuExpanded by remember {
        mutableStateOf(false)
    }

    var viewMenuExpanded by remember {
        mutableStateOf(false)
    }

    var sortMenuExpanded by remember {
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
                            modifier = Modifier.weight(1f)
                        )

                        Box {
                            IconButton(
                                onClick = {
                                    playModeMenuExpanded = true
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            color = if (isPlayModeOn) {
                                                SunsetGold.copy(alpha = 0.16f)
                                            } else {
                                                Color.Transparent
                                            },
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEmotions,
                                        contentDescription = if (isPlayModeOn) {
                                            "현재 놀이 모드: ${playModeLabel(playMode)}"
                                        } else {
                                            "놀이 모드 선택"
                                        },
                                        tint = if (isPlayModeOn) {
                                            SunsetGold
                                        } else {
                                            InkSecondary
                                        },
                                        modifier = Modifier
                                            .size(20.dp)
                                            .semantics {
                                                stateDescription = if (isPlayModeOn) {
                                                    "켜짐"
                                                } else {
                                                    "꺼짐"
                                                }
                                            }
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = playModeMenuExpanded,
                                onDismissRequest = {
                                    playModeMenuExpanded = false
                                }
                            ) {
                                PlayModeMenuItem(
                                    emoji = "💧",
                                    label = "엽서의 연못",
                                    selected = isPondModeOn,
                                    onClick = {
                                        selectedIds = emptySet()
                                        playMode = if (isPondModeOn) {
                                            GalleryPlayMode.NONE
                                        } else {
                                            GalleryPlayMode.POND
                                        }
                                        viewMode = GalleryViewMode.COMPACT_GRID
                                        playModeMenuExpanded = false
                                    }
                                )

                                PlayModeMenuItem(
                                    emoji = "🐑",
                                    label = "양떼목장",
                                    selected = isSheepRanchModeOn,
                                    onClick = {
                                        selectedIds = emptySet()
                                        playMode = if (isSheepRanchModeOn) {
                                            GalleryPlayMode.NONE
                                        } else {
                                            GalleryPlayMode.SHEEP_RANCH
                                        }
                                        viewMode = GalleryViewMode.COMPACT_GRID
                                        playModeMenuExpanded = false
                                    }
                                )

                                PlayModeMenuItem(
                                    emoji = "🏎️",
                                    label = "엽서 쫑쫑컵",
                                    selected = isRaceModeOn,
                                    onClick = {
                                        selectedIds = emptySet()
                                        playMode = if (isRaceModeOn) {
                                            GalleryPlayMode.NONE
                                        } else {
                                            GalleryPlayMode.RACE
                                        }
                                        viewMode = GalleryViewMode.COMPACT_GRID
                                        playModeMenuExpanded = false
                                    }
                                )
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
                                    contentDescription = "보기 방식 변경",
                                    tint = InkSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = viewMenuExpanded,
                                onDismissRequest = {
                                    viewMenuExpanded = false
                                }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "3열 그리드 보기",
                                            color = InkPrimary,
                                            fontWeight = if (viewMode == GalleryViewMode.COMPACT_GRID) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    },
                                    modifier = if (viewMode == GalleryViewMode.COMPACT_GRID) {
                                        Modifier.background(SunsetGold.copy(alpha = 0.16f))
                                    } else {
                                        Modifier
                                    },
                                    onClick = {
                                        viewMode = GalleryViewMode.COMPACT_GRID
                                        viewMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "세부 기록 보기",
                                            color = InkPrimary,
                                            fontWeight = if (viewMode == GalleryViewMode.DETAIL_LIST) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    },
                                    modifier = if (viewMode == GalleryViewMode.DETAIL_LIST) {
                                        Modifier.background(SunsetGold.copy(alpha = 0.16f))
                                    } else {
                                        Modifier
                                    },
                                    onClick = {
                                        playMode = GalleryPlayMode.NONE
                                        viewMode = GalleryViewMode.DETAIL_LIST
                                        viewMenuExpanded = false
                                    }
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = {
                                    sortMenuExpanded = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "정렬 방식 변경",
                                    tint = InkSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = {
                                    sortMenuExpanded = false
                                }
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

                    HorizontalDivider(color = SurfaceGray, thickness = 1.dp)
                }
            }
        },

        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(
                    onClick = onNavigateToCamera,
                    containerColor = BrutalWhite,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 6.dp
                    ),
                    modifier = Modifier.size(56.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_camera_button),
                        contentDescription = "카메라",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                    )
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
            val displayedPostcards = remember(postcards, sortOrder) {
                when (sortOrder) {
                    GallerySortOrder.NEWEST ->
                        postcards.sortedByDescending { it.capturedAt }

                    GallerySortOrder.OLDEST ->
                        postcards.sortedBy { it.capturedAt }
                }
            }

            when (viewMode) {
                GalleryViewMode.COMPACT_GRID -> {
                    GalleryGrid(
                        postcards = displayedPostcards,
                        selectedIds = selectedIds,
                        shakeTrigger = shakeTrigger,
                        isPondModeOn = isPondModeOn,
                        pondController = pondController,
                        paddingValues = paddingValues,
                        onItemClick = ::handleItemClick,
                        onItemLongClick = ::handleItemLongClick
                    )
                }

                GalleryViewMode.DETAIL_LIST -> {
                    GalleryDetailList(
                        postcards = displayedPostcards,
                        selectedIds = selectedIds,
                        paddingValues = paddingValues,
                        onItemClick = ::handleItemClick,
                        onItemLongClick = ::handleItemLongClick
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

private fun playModeLabel(mode: GalleryPlayMode): String =
    when (mode) {
        GalleryPlayMode.POND -> "엽서의 연못"
        GalleryPlayMode.SHEEP_RANCH -> "양떼목장"
        GalleryPlayMode.RACE -> "엽서 쫑쫑컵"
        GalleryPlayMode.NONE -> ""
    }

@Composable
private fun PlayModeMenuItem(
    emoji: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = InkPrimary,
                fontWeight = if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
            )
        },
        leadingIcon = {
            Text(
                text = emoji,
                fontSize = 16.sp
            )
        },
        trailingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SunsetGold
                )
            }
        },
        modifier = if (selected) {
            Modifier.background(SunsetGold.copy(alpha = 0.16f))
        } else {
            Modifier
        },
        onClick = onClick
    )
}

@Composable
private fun GalleryGrid(
    postcards: List<Postcard>,
    selectedIds: Set<Long>,
    shakeTrigger: Int,
    isPondModeOn: Boolean,
    pondController: PondController,
    paddingValues: PaddingValues,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit
) {
    var originInWindow by remember { mutableStateOf(Offset.Zero) }
    val gridState = rememberLazyGridState()

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
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit
) {
    val monthSections = remember(postcards) {
        monthSectionsFor(postcards)
    }

    LazyColumn(
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
