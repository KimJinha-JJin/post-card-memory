package com.postcardmemory

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.postcardmemory.data.Postcard
import com.postcardmemory.data.PostcardDao
import com.postcardmemory.data.PostcardDatabase
import com.postcardmemory.data.PostcardRepository
import com.postcardmemory.utils.PostcardDeletionManager
import java.io.File
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PostcardDeletionManager.deletePostcard의 DB-우선 정책(DB 삭제 성공 후에만
 * 파일을 정리하고, 실패하면 파일을 전혀 건드리지 않는다)을 실제 Room DAO +
 * 실제 filesDir로 검증한다.
 *
 * cleanupPostcardOwnedAssets 자체의 파일 정리 동작은 PostcardDeletionManagerTest
 * (JVM, TemporaryFolder)가 이미 촘촘히 덮고 있다. 그 파일이 스스로 밝히듯,
 * PostcardRepository가 Room DAO(구체 클래스가 만들어내는 생성 코드)를 감싸고
 * 있어 "DB 삭제가 예외로 실패하면 파일 정리 자체가 호출되지 않는다"는 게이트는
 * 순수 JUnit만으로 재현할 수 없어 지금까지 코드 리뷰로만 확인돼 있었다. 이
 * 파일은 그 게이트만, 기존 PostcardBackSaveTest/PostcardBackgroundColorSaveRaceTest와
 * 같은 `object : PostcardDao by dao { override ... }` 패턴으로 실행 가능하게 만든다.
 */
class PostcardDeletionOrchestrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** 이 테스트 전용 하위 디렉터리에 실제 소유 파일 하나를 만들어 둔다. */
    private fun ownedImageFile(postcardId: Long): File {
        val file = File(
            context.filesDir,
            "deletion_orchestration_test/$postcardId/image.jpg"
        )
        file.parentFile?.mkdirs()
        file.writeText("test-image")
        return file
    }

    @Test fun databaseDeleteFailure_deletesZeroFilesAndKeepsDbRow() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, PostcardDatabase::class.java).build()
        val dao = db.postcardDao()
        val id = dao.insertPostcard(Postcard(imagePath = "placeholder", title = "test"))
        val imageFile = ownedImageFile(id)
        val postcard = dao.getPostcardById(id)!!.copy(imagePath = imageFile.path)

        val failingDao = object : PostcardDao by dao {
            override suspend fun deletePostcardById(id: Long) {
                throw IOException("injected delete failure")
            }
        }
        val manager = PostcardDeletionManager(context, PostcardRepository(failingDao))

        try {
            val result = manager.deletePostcard(postcard)

            assertFalse("DB 삭제가 예외로 실패하면 databaseDeleted는 false여야 한다", result.databaseDeleted)
            assertTrue("실패 시 삭제된 자산이 하나도 없어야 한다", result.deletedAssets.isEmpty())
            assertTrue("실패 시 missing으로도 기록되면 안 된다(정리 자체를 시도하지 않음)", result.missingAssets.isEmpty())
            assertTrue("실패 시 failedAssets도 비어 있어야 한다(정리 자체를 시도하지 않음)", result.failedAssets.isEmpty())
            assertTrue("DB 삭제 실패 시 사용자 파일은 절대 지워지면 안 된다", imageFile.exists())
            assertNotNull("DB 행도 그대로 남아 있어야 한다", dao.getPostcardById(id))
        } finally {
            imageFile.parentFile?.deleteRecursively()
            db.close()
        }
    }

    @Test fun databaseDeleteSuccess_thenOwnedFilesAreActuallyDeleted() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, PostcardDatabase::class.java).build()
        val dao = db.postcardDao()
        val id = dao.insertPostcard(Postcard(imagePath = "placeholder", title = "test"))
        val imageFile = ownedImageFile(id)
        val postcard = dao.getPostcardById(id)!!.copy(imagePath = imageFile.path)

        val manager = PostcardDeletionManager(context, PostcardRepository(dao))

        try {
            val result = manager.deletePostcard(postcard)

            assertTrue("DB 삭제가 성공하면 databaseDeleted는 true여야 한다", result.databaseDeleted)
            assertTrue("image" in result.deletedAssets)
            assertFalse("DB 삭제 성공 후에는 소유 파일이 실제로 지워져야 한다", imageFile.exists())
            assertNull("DB 행도 실제로 사라져야 한다", dao.getPostcardById(id))
        } finally {
            imageFile.parentFile?.deleteRecursively()
            db.close()
        }
    }

    @Test fun deletingSameManagerCallTwice_secondCallIsIdempotentAndSafe() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, PostcardDatabase::class.java).build()
        val dao = db.postcardDao()
        val id = dao.insertPostcard(Postcard(imagePath = "placeholder", title = "test"))
        val imageFile = ownedImageFile(id)
        val postcard = dao.getPostcardById(id)!!.copy(imagePath = imageFile.path)

        val manager = PostcardDeletionManager(context, PostcardRepository(dao))

        try {
            val first = manager.deletePostcard(postcard)
            assertTrue(first.isFullSuccess)

            // 이미 지워진 엽서를 같은 매니저로 다시 지워도(id는 더 이상 DB에 없음),
            // DELETE WHERE id=:id는 0행에 대해서도 예외 없이 성공하므로 두 번째
            // 호출도 안전하게 끝나야 한다(다른 엽서의 자산을 건드리지 않고, 크래시 없음).
            val second = manager.deletePostcard(postcard)
            assertTrue("중복 삭제 요청도 DB 단계에서 예외 없이 성공 처리된다", second.databaseDeleted)
            assertEquals(emptyList<String>(), second.deletedAssets)
            assertTrue("image" in second.missingAssets)
            assertTrue(second.isFullSuccess)
        } finally {
            imageFile.parentFile?.deleteRecursively()
            db.close()
        }
    }
}
