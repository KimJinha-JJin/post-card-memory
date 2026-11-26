package com.postcardmemory.ui.gallery

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.futuremail.FutureMailGroup
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.ui.theme.SunsetGold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@Composable
internal fun GalleryMemoryScalePage(
    postcards: List<Postcard>,
    paddingValues: PaddingValues,
    paperTile: ImageBitmap,
    futureMailGroups: List<FutureMailGroup>?,
    onOpenFutureMailbox: () -> Unit,
    isPageVisible: Boolean
) {
    val today = rememberTodayDate()
    val zone = ZoneId.systemDefault()
    var periodName by rememberSaveable { mutableStateOf(MemoryScalePeriod.ALL.name) }
    val period = runCatching { MemoryScalePeriod.valueOf(periodName) }.getOrDefault(MemoryScalePeriod.ALL)
    var year by rememberSaveable { mutableStateOf(today.year) }
    var month by rememberSaveable { mutableStateOf(today.monthValue) }
    val date = LocalDate.of(year, month, 1)
    val measured = remember(postcards, period, date, zone) { memoryScalePostcards(postcards, period, date, zone) }
    // 정렬이나 꾸미기 저장으로 재생하지 않고, 대상 id 집합이 바뀔 때 재생한다.
    val targetIds = remember(measured) { measured.map { it.id }.toSet() }
    val count = measured.size
    val capacity = remember(count) { memoryScaleCapacity(count) }
    val years = remember(postcards, today.year, zone) {
        (postcards.map { Instant.ofEpochMilli(it.capturedAt).atZone(zone).year } + today.year).distinct().sortedDescending()
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val resolver = LocalContext.current.contentResolver
    fun readReducedMotion() = runCatching {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)
    var reducedMotion by remember(resolver) { mutableStateOf(readReducedMotion()) }
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle, resolver) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (resumed) reducedMotion = readReducedMotion()
        }
        val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { reducedMotion = readReducedMotion() }
        }
        lifecycle.addObserver(observer)
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, settingsObserver)
        onDispose {
            lifecycle.removeObserver(observer)
            resolver.unregisterContentObserver(settingsObserver)
        }
    }
    val active = isPageVisible && resumed
    // 같은 장수의 기간 전환에도 반응하고, 일반 재구성에서는 키가 바뀌지 않는다.
    val measurementKey = listOf(periodName, if (period != MemoryScalePeriod.ALL) year else null,
        if (period == MemoryScalePeriod.MONTH) month else null, targetIds)
    val elapsed = remember(measurementKey) { mutableLongStateOf(0L) }
    var finished by remember(measurementKey) { mutableStateOf(count == 0 || reducedMotion) }
    LaunchedEffect(measurementKey, active, reducedMotion) {
        if (!active || reducedMotion || count == 0) {
            elapsed.longValue = MEMORY_SCALE_DURATION_MS
            finished = true
            return@LaunchedEffect
        }
        elapsed.longValue = 0
        finished = false
        val start = withFrameNanos { it }
        do {
            elapsed.longValue = withFrameNanos { (it - start) / 1_000_000 }
        } while (elapsed.longValue < MEMORY_SCALE_DURATION_MS)
        elapsed.longValue = MEMORY_SCALE_DURATION_MS
        finished = true
    }
    val countText = String.format(Locale.KOREA, "%,d", count)
    val capacityText = String.format(Locale.KOREA, "%,d", capacity)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            start = 16.dp, end = 16.dp,
            top = paddingValues.calculateTopPadding() + 4.dp,
            bottom = paddingValues.calculateBottomPadding() + futureMailDioramaBottomReserveDp().dp + 24.dp
        ), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("기억 저울", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = InkPrimary,
            modifier = Modifier.fillMaxWidth().semantics { heading() })
        MemoryScalePeriodSelector(period, year, month, years,
            onPeriod = { periodName = it.name }, onYear = { year = it }, onMonth = { month = it })
        MemoryScaleDrawing(count, paperTile, elapsedMs = { elapsed.longValue },
            modifier = Modifier.widthIn(max = 336.dp).fillMaxWidth())
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clearAndSetSemantics { contentDescription = "기억 저울, 엽서 ${count}장, 최대 눈금 ${capacity}장" }) {
            Text(if (finished) "${countText}장" else "측정 중", fontSize = 28.sp, color = InkPrimary)
            Text("눈금 0–${capacityText}장", fontSize = 12.sp, color = InkSecondary)
        }
        Spacer(Modifier.height(4.dp))
        // 스크롤 안에서 높이를 확보해 짧은 화면에서도 기존 진입점을 유지한다.
        if (futureMailGroups != null) {
            GalleryFutureMailDioramaSlot(groups = futureMailGroups, onOpenFutureMailbox = onOpenFutureMailbox,
                modifier = Modifier.fillMaxWidth(), keepVisible = true, topGap = 8.dp)
        }
    }
}

@Composable
private fun MemoryScalePeriodSelector(
    period: MemoryScalePeriod, year: Int, month: Int, years: List<Int>,
    onPeriod: (MemoryScalePeriod) -> Unit, onYear: (Int) -> Unit, onMonth: (Int) -> Unit
) {
    var periodMenu by remember { mutableStateOf(false) }
    var yearMenu by remember { mutableStateOf(false) }
    var monthMenu by remember { mutableStateOf(false) }
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Box {
            TextButton(onClick = { periodMenu = true }) {
                Text(when (period) { MemoryScalePeriod.ALL -> "전체 ▾"; MemoryScalePeriod.YEAR -> "연간 ▾"; MemoryScalePeriod.MONTH -> "월간 ▾" }, color = InkPrimary)
            }
            DropdownMenu(expanded = periodMenu, onDismissRequest = { periodMenu = false },
                shape = RectangleShape, containerColor = PaperSurface) {
                MemoryScalePeriod.entries.forEach { item ->
                    MemoryScaleMenuItem(text = when (item) { MemoryScalePeriod.ALL -> "전체"; MemoryScalePeriod.YEAR -> "연간"; MemoryScalePeriod.MONTH -> "월간" },
                        selected = period == item,
                        onClick = { onPeriod(item); periodMenu = false })
                }
            }
        }
        if (period != MemoryScalePeriod.ALL) Box {
            TextButton(onClick = { yearMenu = true }) { Text("${year}년 ▾", color = InkPrimary) }
            DropdownMenu(expanded = yearMenu, onDismissRequest = { yearMenu = false },
                shape = RectangleShape, containerColor = PaperSurface) {
                years.forEach { item -> MemoryScaleMenuItem(text = "${item}년", selected = year == item,
                    onClick = { onYear(item); yearMenu = false }) }
            }
        }
        if (period == MemoryScalePeriod.MONTH) Box {
            TextButton(onClick = { monthMenu = true }) { Text("${month}월 ▾", color = InkPrimary) }
            DropdownMenu(expanded = monthMenu, onDismissRequest = { monthMenu = false },
                shape = RectangleShape, containerColor = PaperSurface) {
                (1..12).forEach { item -> MemoryScaleMenuItem(text = "${item}월", selected = month == item,
                    onClick = { onMonth(item); monthMenu = false }) }
            }
        }
    }
}

/** 갤러리 정렬 메뉴와 같은 종이색·직각 형태·선택 강조를 쓴다. */
@Composable
private fun MemoryScaleMenuItem(text: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, color = InkPrimary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        modifier = (if (selected) Modifier.background(SunsetGold.copy(alpha = 0.16f)) else Modifier)
            .semantics { this.selected = selected },
        onClick = onClick
    )
}
