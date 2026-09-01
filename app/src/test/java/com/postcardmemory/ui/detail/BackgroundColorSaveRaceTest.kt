package com.postcardmemory.ui.detail

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DetailViewModel.updateBackgroundColor의 경합 안전성 검증.
 *
 * updateBackgroundColor는 Context/Room/Hilt에 묶여 있어 Robolectric·mockk
 * 없이 순수 JUnit에서 직접 호출할 수 없다(StyleSaveRaceTest,
 * TemplateStyleSaveRollbackTest와 동일한 제약). 대신 실제 구현과 동일한
 * 구조 — 공유 styleWriteMutex로 DAO 쓰기를 직렬화하고, Mutex를 획득한
 * 순간 호출 당시 캡처값이 아니라 그 시점의 화면 상태를 다시 읽어서 쓰는
 * 구조 — 를 가짜 화면 상태·가짜 Room·가짜 파일 시스템으로 재현해 검증한다.
 *
 * 이 테스트가 특히 겨냥하는 지점은 DAO의 updatePostcardBackground가
 * backgroundColorArgb와 backgroundImagePath를 **한 쿼리로 함께 UPDATE**
 * 한다는 사실이다. 배경색만 바꾸는 저장도 경로 컬럼을 같이 쓰게 되므로,
 * 경로를 고정값으로 넘기면 그 사이 반영된 더 최신 경로를 오래된 저장이
 * 지워버린다.
 *
 * beforeWrite/afterWrite는 실제 코드의 두 지점에 대응한다.
 * - beforeWrite: Dispatchers.IO로 디스패치돼 실제 DAO 쓰기가 시작되기까지의
 *   구간. 완료 순서를 뒤섞으려면 Mutex 진입 *전*에 둬야 한다.
 * - afterWrite: withContext(Dispatchers.IO) 블록이 끝나고 다시 메인으로
 *   복귀하기까지의 구간. 이 사이에 사용자가 색을 한 번 더 고를 수 있다.
 *
 * 참고: 현재 앱에서 backgroundImagePath를 non-null로 만드는 UI 경로는 없다
 * (호출자가 없던 updateBackgroundImage/removeBackgroundImage는 dead code
 * 정리로 제거됐다). 아래 saveBackgroundImagePath는 "경로 컬럼에 쓰는 다른
 * 저장이 존재할 때" updateBackgroundColor가 그 값을 보존하는지 확인하기
 * 위한 대역이며, 오늘 그런 호출자가 있다고 주장하는 것이 아니다.
 */
class BackgroundColorSaveRaceTest {

    /** Postcard에서 배경 저장이 다루는 두 필드만 축약한 화면 상태 모델. */
    private data class FakeUiState(
        val backgroundColorArgb: Long,
        val backgroundImagePath: String?
    )

    /**
     * 실제 Room을 대신하는 가짜 저장소. updatePostcardBackground가 두 컬럼을
     * 한 번에 UPDATE하는 것과 동일하게, 쓰기는 항상 두 값을 함께 받는다.
     */
    private class FakeRoom(
        var backgroundColorArgb: Long,
        var backgroundImagePath: String?
    ) {
        val writeLog = mutableListOf<String>()

        fun updateBackground(
            colorArgb: Long,
            imagePath: String?
        ) {
            backgroundColorArgb = colorArgb
            backgroundImagePath = imagePath
            writeLog += "background=($colorArgb,$imagePath)"
        }
    }

    /** 배경 이미지 파일이 실제로 남아 있는지 확인하기 위한 가짜 파일 시스템. */
    private class FakeFileSystem {
        val existingFiles = mutableSetOf<String>()
        val deletedFiles = mutableListOf<String>()

        fun delete(path: String?) {
            if (path.isNullOrBlank()) {
                return
            }

            existingFiles.remove(path)
            deletedFiles += path
        }

        fun exists(path: String): Boolean =
            existingFiles.contains(path)
    }

    private class FakeViewModel(
        initialColorArgb: Long = COLOR_INITIAL,
        initialImagePath: String? = null
    ) {
        var ui: FakeUiState? =
            FakeUiState(
                backgroundColorArgb = initialColorArgb,
                backgroundImagePath = initialImagePath
            )

        val room =
            FakeRoom(
                backgroundColorArgb = initialColorArgb,
                backgroundImagePath = initialImagePath
            )

        val files = FakeFileSystem()

        val errors = mutableListOf<String>()

        /** 다른 개별 스타일 저장·템플릿 저장과 공유하는 그 Mutex. */
        private val styleWriteMutex = Mutex()

        init {
            if (initialImagePath != null) {
                files.existingFiles += initialImagePath
            }
        }

        /** DetailViewModel.updateBackgroundColor와 동일한 형태. */
        fun updateBackgroundColor(
            scope: CoroutineScope,
            backgroundColorArgb: Long,
            failWith: Exception? = null,
            beforeWrite: suspend () -> Unit = {},
            afterWrite: suspend () -> Unit = {}
        ): Job {
            val currentUi = ui ?: return Job().apply { complete() }

            val previousBackgroundColorArgb =
                currentUi.backgroundColorArgb

            // 낙관적 갱신 — 배경색만 바꾼다(경로는 이 저장의 소관이 아니다).
            ui = currentUi.copy(
                backgroundColorArgb = backgroundColorArgb
            )

            return scope.launch {
                try {
                    beforeWrite()

                    styleWriteMutex.withLock {
                        // Mutex를 획득한 이 순간, 색과 경로를 **둘 다** 다시 읽는다.
                        val latest = ui ?: return@withLock
                        if (failWith != null) throw failWith
                        room.updateBackground(
                            colorArgb = latest.backgroundColorArgb,
                            imagePath = latest.backgroundImagePath
                        )
                    }

                    afterWrite()

                    // 커밋한 값은 정의상 커밋 시점의 화면 값이므로 되쓰지 않는다.
                    // 배경 이미지 파일도 이 저장이 지우지 않는다.
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    // 자기가 낙관적으로 쓴 배경색이 아직 화면에 남아 있을 때만 되돌린다.
                    if (ui?.backgroundColorArgb == backgroundColorArgb) {
                        ui = ui?.copy(
                            backgroundColorArgb =
                                previousBackgroundColorArgb
                        )
                    }
                    errors += "background color save failed"
                }
            }
        }

        /**
         * 경로 컬럼에 쓰는 다른 저장의 대역. 같은 Mutex를 쓰고 Mutex 획득
         * 시점의 최신 색을 다시 읽는, updateBackgroundColor와 대칭인 형태다.
         */
        fun saveBackgroundImagePath(
            scope: CoroutineScope,
            imagePath: String?,
            beforeWrite: suspend () -> Unit = {}
        ): Job {
            val currentUi = ui ?: return Job().apply { complete() }

            ui = currentUi.copy(
                backgroundImagePath = imagePath
            )

            if (imagePath != null) {
                files.existingFiles += imagePath
            }

            return scope.launch {
                beforeWrite()

                styleWriteMutex.withLock {
                    val latest = ui ?: return@withLock
                    room.updateBackground(
                        colorArgb = latest.backgroundColorArgb,
                        imagePath = latest.backgroundImagePath
                    )
                }
            }
        }
    }

    // ---- 1. 배경색 변경은 기존 배경 이미지 경로와 파일을 건드리지 않는다 ----

    @Test
    fun colorSave_preservesExistingImagePathAndFile() = runBlocking {
        val vm = FakeViewModel(initialImagePath = PATH_OLD)

        vm.updateBackgroundColor(this, COLOR_NEW).join()

        assertEquals(COLOR_NEW, vm.room.backgroundColorArgb)
        assertEquals(PATH_OLD, vm.room.backgroundImagePath)
        assertEquals(PATH_OLD, vm.ui?.backgroundImagePath)
        // 배경색 변경은 이미지 파일의 수명주기를 소유하지 않는다.
        assertTrue(vm.files.deletedFiles.isEmpty())
        assertTrue(vm.files.exists(PATH_OLD))
    }

    // ---- 2. 오래된 배경색 저장이 더 최신 이미지 경로를 지우면 안 된다 ----

    @Test
    fun staleColorSave_doesNotWipeNewerImagePath() = runBlocking {
        val vm = FakeViewModel()
        val letColorProceed = CompletableDeferred<Unit>()

        // 1. 배경색 변경 시작 — 실제 쓰기 직전까지만 진행하고 멈춘다.
        val colorJob = vm.updateBackgroundColor(
            scope = this,
            backgroundColorArgb = COLOR_NEW,
            beforeWrite = { letColorProceed.await() }
        )

        // 2. 그 사이 경로가 새로 설정되고 먼저 커밋된다.
        vm.saveBackgroundImagePath(this, PATH_NEW).join()

        // 3. 뒤늦게 배경색 저장이 실제 쓰기를 시도한다.
        letColorProceed.complete(Unit)
        colorJob.join()

        // 늦게 커밋한 배경색 저장이 Mutex 안에서 다시 읽은 경로는 이미
        // PATH_NEW이므로 null로 덮어쓰지 않는다.
        assertEquals(PATH_NEW, vm.room.backgroundImagePath)
        assertEquals(COLOR_NEW, vm.room.backgroundColorArgb)
        assertEquals(vm.ui?.backgroundImagePath, vm.room.backgroundImagePath)
        assertEquals(vm.ui?.backgroundColorArgb, vm.room.backgroundColorArgb)
        assertTrue(vm.files.exists(PATH_NEW))
        assertTrue(vm.files.deletedFiles.isEmpty())
    }

    // ---- 3. 배경색 저장 실패가 더 최신 이미지 경로를 되돌리면 안 된다 ----

    @Test
    fun failedColorSave_doesNotRollbackNewerImagePath() = runBlocking {
        val vm = FakeViewModel(initialImagePath = PATH_OLD)
        val letColorProceed = CompletableDeferred<Unit>()

        val colorJob = vm.updateBackgroundColor(
            scope = this,
            backgroundColorArgb = COLOR_NEW,
            failWith = IllegalStateException("db failed"),
            beforeWrite = { letColorProceed.await() }
        )

        vm.saveBackgroundImagePath(this, PATH_NEW).join()

        letColorProceed.complete(Unit)
        colorJob.join()

        assertEquals(1, vm.errors.size)
        // 실패한 저장이 되돌리는 건 자기 배경색뿐이다.
        assertEquals(COLOR_INITIAL, vm.ui?.backgroundColorArgb)
        // 더 최신 경로는 화면에서도 Room에서도 그대로 유지된다.
        assertEquals(PATH_NEW, vm.ui?.backgroundImagePath)
        assertEquals(PATH_NEW, vm.room.backgroundImagePath)
        assertTrue(vm.files.exists(PATH_NEW))
        assertTrue(vm.files.deletedFiles.isEmpty())
    }

    // ---- 4. 배경색 저장 실패가 더 최신 배경색을 되돌리면 안 된다 ----

    @Test
    fun failedColorSave_doesNotRollbackNewerColor() = runBlocking {
        val vm = FakeViewModel()
        val letStaleProceed = CompletableDeferred<Unit>()

        val staleJob = vm.updateBackgroundColor(
            scope = this,
            backgroundColorArgb = COLOR_NEW,
            failWith = IllegalStateException("db failed"),
            beforeWrite = { letStaleProceed.await() }
        )

        // 사용자가 곧바로 다른 색을 고르고, 그 저장은 정상 커밋된다.
        vm.updateBackgroundColor(this, COLOR_NEWEST).join()

        letStaleProceed.complete(Unit)
        staleJob.join()

        assertEquals(1, vm.errors.size)
        // 마지막 사용자 조작이 살아남는다.
        assertEquals(COLOR_NEWEST, vm.ui?.backgroundColorArgb)
        assertEquals(COLOR_NEWEST, vm.room.backgroundColorArgb)
    }

    // ---- 5. 커밋 후 복귀한 오래된 저장이 최신 배경색을 되돌리면 안 된다 ----

    @Test
    fun staleColorSave_afterCommit_doesNotRewriteNewerColor() = runBlocking {
        val vm = FakeViewModel()
        val reachedAfterWrite = CompletableDeferred<Unit>()
        val letStaleResume = CompletableDeferred<Unit>()

        // 1. COLOR_NEW 저장이 Room 커밋까지 마치고 메인 복귀 직전에 멈춘다.
        val staleJob = vm.updateBackgroundColor(
            scope = this,
            backgroundColorArgb = COLOR_NEW,
            afterWrite = {
                reachedAfterWrite.complete(Unit)
                letStaleResume.await()
            }
        )
        reachedAfterWrite.await()

        // 2. 그 사이 사용자가 색을 한 번 더 고른다(낙관적 갱신은 즉시 반영).
        val freshJob = vm.updateBackgroundColor(this, COLOR_NEWEST)

        // 3. 오래된 저장이 복귀한다 — 자신이 커밋한 COLOR_NEW를 화면에 되쓰면 안 된다.
        letStaleResume.complete(Unit)
        staleJob.join()
        freshJob.join()

        assertTrue(vm.errors.isEmpty())
        assertEquals(COLOR_NEWEST, vm.ui?.backgroundColorArgb)
        assertEquals(COLOR_NEWEST, vm.room.backgroundColorArgb)
    }

    // ---- 6. 취소는 실패가 아니다 ----

    @Test
    fun cancelledColorSave_recordsNoErrorAndDoesNotRollback() = runBlocking {
        val vm = FakeViewModel(initialImagePath = PATH_OLD)
        val neverCompletes = CompletableDeferred<Unit>()

        val staleJob = vm.updateBackgroundColor(
            scope = this,
            backgroundColorArgb = COLOR_NEW,
            beforeWrite = { neverCompletes.await() }
        )

        // 사용자가 다른 색을 고르고 정상 저장된 뒤, 오래된 저장이 취소된다.
        vm.updateBackgroundColor(this, COLOR_NEWEST).join()
        staleJob.cancel()
        staleJob.join()

        assertTrue(staleJob.isCancelled)
        // 취소된 저장은 오류를 남기지 않는다(Snackbar 없음).
        assertTrue(vm.errors.isEmpty())
        // 최신 화면 상태를 롤백하지도 않는다.
        assertEquals(COLOR_NEWEST, vm.ui?.backgroundColorArgb)
        assertEquals(COLOR_NEWEST, vm.room.backgroundColorArgb)
        // 최신 파일을 지우지도 않는다.
        assertEquals(PATH_OLD, vm.room.backgroundImagePath)
        assertTrue(vm.files.exists(PATH_OLD))
        assertTrue(vm.files.deletedFiles.isEmpty())
    }

    // ---- 7. 실제 저장 실패는 여전히 롤백되고 오류를 남긴다 ----

    @Test
    fun realSaveFailure_stillRollsBackAndRecordsError() = runBlocking {
        val vm = FakeViewModel(initialImagePath = PATH_OLD)

        vm.updateBackgroundColor(
            scope = this,
            backgroundColorArgb = COLOR_NEW,
            failWith = IllegalStateException("db failed")
        ).join()

        assertEquals(1, vm.errors.size)
        assertEquals(COLOR_INITIAL, vm.ui?.backgroundColorArgb)
        assertEquals(COLOR_INITIAL, vm.room.backgroundColorArgb)
        // 실패해도 기존 참조 파일은 유지된다.
        assertEquals(PATH_OLD, vm.room.backgroundImagePath)
        assertTrue(vm.files.exists(PATH_OLD))
    }

    // ---- 8. 실패 후에도 다음 정상 저장은 가능해야 한다 ----

    @Test
    fun afterFailure_nextColorSaveSucceedsNormally() = runBlocking {
        val vm = FakeViewModel(initialImagePath = PATH_OLD)

        vm.updateBackgroundColor(
            scope = this,
            backgroundColorArgb = COLOR_NEW,
            failWith = IllegalStateException("db failed")
        ).join()
        assertEquals(COLOR_INITIAL, vm.ui?.backgroundColorArgb)

        vm.updateBackgroundColor(this, COLOR_NEWEST).join()

        assertEquals(COLOR_NEWEST, vm.ui?.backgroundColorArgb)
        assertEquals(COLOR_NEWEST, vm.room.backgroundColorArgb)
        assertEquals(PATH_OLD, vm.room.backgroundImagePath)
        assertTrue(vm.files.exists(PATH_OLD))
    }

    // ---- 9. 배경 이미지가 없는 엽서에서는 경로가 null로 유지된다 ----

    @Test
    fun colorSave_withoutImage_keepsPathNull() = runBlocking {
        val vm = FakeViewModel()

        vm.updateBackgroundColor(this, COLOR_NEW).join()

        assertEquals(COLOR_NEW, vm.room.backgroundColorArgb)
        assertNull(vm.room.backgroundImagePath)
        assertNull(vm.ui?.backgroundImagePath)
        assertEquals(
            listOf("background=($COLOR_NEW,null)"),
            vm.room.writeLog
        )
    }

    private companion object {
        const val COLOR_INITIAL = 0xFFFFFFFFL
        const val COLOR_NEW = 0xFFFF0000L
        const val COLOR_NEWEST = 0xFF00FF00L
        const val PATH_OLD = "/data/postcard_backgrounds/background_old.jpg"
        const val PATH_NEW = "/data/postcard_backgrounds/background_new.jpg"
    }
}
