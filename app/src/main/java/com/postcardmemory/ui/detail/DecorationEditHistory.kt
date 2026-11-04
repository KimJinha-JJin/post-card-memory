package com.postcardmemory.ui.detail

/**
 * 꾸미기 한 종류의 되돌리기·다시하기 기록. 도장·텍스트 스티커·마스킹테이프·라벨 스티커·낙서가
 * 같은 규칙을 따로 구현하던 것을 122일차에 하나로 모았다.
 *
 * 기록 규칙만 맡는다. 스냅샷을 만들고 화면 상태에 되돌리는 일, 버튼 상태 갱신, 초안 자동저장,
 * 파일 정리는 종류마다 달라서 호출하는 쪽(DetailViewModel)에 남긴다.
 *
 * 스냅샷을 복사하지 않고 받은 참조 그대로 보관한다. 그래서 [S]와 그 안의 목록·항목은 불변이어야
 * 한다(5종 모두 val 필드와 새 목록 대입만 쓴다 — DecorationKindHistoryBehaviorTest에서 고정).
 *
 * 사진 스티커 기록은 밀려난 기록의 파일 참조 판정이 얽혀 있고, 사진 위치·크기 기록은 DB 값을
 * 다뤄서 이 클래스를 쓰지 않는다.
 *
 * 스레드 안전하지 않다. 호출하는 ViewModel의 다른 편집 상태처럼 메인 스레드에서만 쓴다.
 */
internal class DecorationEditHistory<S : Any>(
    private val limit: Int
) {
    private val undoStack = ArrayDeque<S>()
    private val redoStack = ArrayDeque<S>()

    init {
        require(limit > 0) { "기록 상한은 1 이상이어야 한다: $limit" }
    }

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    /**
     * 편집 직전 상태를 기록하고 다시하기 기록을 비운다. 상한을 넘으면 가장 오래된 기록을 버린다.
     *
     * @return 오래된 기록을 버렸으면 true. 버려진 기록이 잡고 있던 파일을 정리할 때 쓴다. 두 기록의
     * 합은 상한을 넘지 않으므로, 기록이 밀려날 때 다시하기 기록은 이미 비어 있다.
     */
    fun record(snapshot: S): Boolean {
        undoStack.addLast(snapshot)
        val droppedOldest = undoStack.size > limit
        if (droppedOldest) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        return droppedOldest
    }

    /** [current]를 다시하기 기록에 넣고 되돌릴 상태를 돌려준다. 기록이 없으면 아무것도 바꾸지 않고 null이다. */
    fun undo(current: S): S? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        return previous
    }

    /**
     * [current]를 되돌리기 기록에 넣고 다시 적용할 상태를 돌려준다. 기록이 없으면 null이다.
     *
     * 여기서는 상한을 검사하지 않는다. 되돌리기·다시하기는 기록을 옮기기만 하고 새 기록은 다시하기를
     * 비우므로, 두 기록을 합쳐도 상한을 넘지 않는다.
     */
    fun redo(current: S): S? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    /** 아직 복원될 수 있는 모든 스냅샷(되돌리기 오래된 순 → 다시하기 오래된 순)의 복사본. 파일 참조 판정용이다. */
    fun retainedSnapshots(): List<S> =
        undoStack + redoStack
}
