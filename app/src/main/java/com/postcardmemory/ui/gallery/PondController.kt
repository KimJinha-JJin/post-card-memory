package com.postcardmemory.ui.gallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** 엽서의 연못 모드에서 발사된 엽서가 지나가거나 부딪힌 자리에 남는 파문 한 개. */
data class PondRipple(
    val id: Long,
    val center: Offset,
    val bornAtMillis: Long,
    val maxRadiusPx: Float,
    val lifespanMillis: Long = 700L
)

/** 한 엽서가 발사되어 주변 엽서를 밀어낼 때 발생하는 충격 이벤트. */
data class PondImpulse(
    val sourceId: Long,
    val center: Offset,
    val sequence: Long
)

private const val MAX_RIPPLES = 20

/**
 * 연못 모드 그리드 하나가 공유하는 임시(화면 한정) 상태.
 *
 * 카드 중심 좌표는 각 StampCard가 스스로 들고 있고(restCenterInWindow), 발사된
 * 카드는 lastImpulse만 갱신하면 다른 카드들이 각자 snapshotFlow로 구독해 스스로
 * 거리·반응 여부를 판단한다 — 따라서 이 컨트롤러는 카드별 위치를 따로 보관하지
 * 않는다. 어떤 저장 데이터도 담지 않으며, 화면을 벗어나면 그냥 버려진다.
 */
class PondController {
    var gridBoundsInWindow: Rect = Rect.Zero

    val ripples = mutableStateListOf<PondRipple>()

    var lastImpulse by mutableStateOf<PondImpulse?>(null)
        private set

    private var rippleIdSeq = 0L
    private var impulseSeq = 0L

    fun addRipple(centerInWindow: Offset, maxRadiusPx: Float, lifespanMillis: Long = 700L) {
        if (ripples.size >= MAX_RIPPLES) {
            ripples.removeAt(0)
        }
        ripples.add(
            PondRipple(
                id = rippleIdSeq++,
                center = centerInWindow,
                bornAtMillis = System.currentTimeMillis(),
                maxRadiusPx = maxRadiusPx,
                lifespanMillis = lifespanMillis
            )
        )
    }

    fun pruneRipples(nowMillis: Long) {
        if (ripples.isEmpty()) return
        ripples.removeAll { nowMillis - it.bornAtMillis > it.lifespanMillis }
    }

    fun clearRipples() {
        ripples.clear()
    }

    fun notifyLaunch(sourceId: Long, centerInWindow: Offset) {
        lastImpulse = PondImpulse(sourceId, centerInWindow, impulseSeq++)
    }

    /** 연못 모드를 끄거나 화면을 벗어날 때 이 그리드의 모든 임시 상태를 정리한다. */
    fun reset() {
        ripples.clear()
        lastImpulse = null
    }
}
