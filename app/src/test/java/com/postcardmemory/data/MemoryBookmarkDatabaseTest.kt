package com.postcardmemory.data

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** PC의 Robolectric 임시 공간만 사용해. 연결 기기나 실제 앱 DB에는 접근하지 않아. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE, application = Application::class)
class MemoryBookmarkDatabaseTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private fun open(name: String) = Room.databaseBuilder(context, PostcardDatabase::class.java, name)
        .addMigrations(PostcardDatabase.MIGRATION_19_20).allowMainThreadQueries().build()
    private fun name() = "bookmark_${UUID.randomUUID()}.db"
    private inline fun <T> PostcardDatabase.useDatabase(block: (PostcardDatabase) -> T): T =
        try { block(this) } finally { close() }
    private fun postcard(id: Long = 0) = Postcard(id, "fixture-photo-$id", "fixture-title", capturedAt = 123L,
        message = "앞면", backMessage = "옛 편지", backPostscript = "추신", backWrittenAt = 100L)

    @Test fun real19MigrationPreservesEveryLegacyColumnAndDoesNotCreateBookmarks() = runBlocking {
        val file = context.getDatabasePath(name()).apply { parentFile!!.mkdirs() }
        val schema = listOf(File("schemas/com.postcardmemory.data.PostcardDatabase/19.json"),
            File("app/schemas/com.postcardmemory.data.PostcardDatabase/19.json")).first(File::exists)
        val json = org.json.JSONObject(schema.readText()).getJSONObject("database")
        val entity = json.getJSONArray("entities").getJSONObject(0)
        var before = emptyList<String?>()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { raw ->
            raw.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", "postcards"))
            val setup = json.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) raw.execSQL(setup.getString(i))
            raw.execSQL("""INSERT INTO postcards
                (id,imagePath,title,capturedAt,backgroundColorArgb,message,backMessage,backPostscript,backWrittenAt)
                VALUES (7,'legacy-photo','legacy-title',123,4294966263,'앞면','옛 편지','추신',100)""")
            before = raw.rawQuery("SELECT * FROM postcards WHERE id=7", null).use { c ->
                check(c.moveToFirst()); (0 until c.columnCount).map { if (c.isNull(it)) null else c.getString(it) }
            }
            raw.version = 19
        }
        open(file.name).useDatabase { room ->
            val after = room.openHelper.writableDatabase.query("SELECT * FROM postcards WHERE id=7").use { c ->
                check(c.moveToFirst()); (0 until c.columnCount).map { if (c.isNull(it)) null else c.getString(it) }
            }
            assertEquals(before, after)
            assertEquals("옛 편지", room.postcardDao().getPostcardById(7)!!.backMessage)
            assertNull(room.memoryBookmarkDao().get(7))
            room.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        }
    }

    @Test fun repeatedParentUpsertAndDedicatedUpdatesKeepTheBookmark() = runBlocking {
        open(name()).useDatabase { room ->
            val dao = room.postcardDao(); val bookmarks = room.memoryBookmarkDao()
            val id = dao.insertPostcard(postcard())
            val bookmark = MemoryBookmark.from(id, BookmarkDesign(BookmarkShape.CLASSIC, BookmarkPaper.MINT, BookmarkTrace.LINES))
            bookmarks.upsert(bookmark)
            repeat(4) { dao.insertPostcard(dao.getPostcardById(id)!!.copy(message = "수정 $it")) }
            dao.updatePostcardBackground(id, 0xFF123456, "fixture-background")
            dao.updatePostcardBackMessage(id, "본문 수정", 200L, 540)
            assertEquals(bookmark, bookmarks.get(id))
            assertEquals("수정 3", dao.getPostcardById(id)!!.message)
        }
    }

    @Test fun editAndRemoveAreIsolatedAndNeverChangeThePostcard() = runBlocking {
        open(name()).useDatabase { room ->
            val dao = room.postcardDao(); val bookmarks = room.memoryBookmarkDao()
            val a = dao.insertPostcard(postcard()); val b = dao.insertPostcard(postcard())
            val original = dao.getPostcardById(a)
            val other = MemoryBookmark.from(b, BookmarkDesign())
            bookmarks.upsert(MemoryBookmark.from(a, BookmarkDesign()))
            bookmarks.upsert(other)
            bookmarks.upsert(MemoryBookmark.from(a, BookmarkDesign(BookmarkShape.MINI_TAG, BookmarkPaper.BROWN, BookmarkTrace.NONE)))
            assertEquals(BookmarkShape.MINI_TAG, bookmarks.get(a)!!.designOrNull()!!.shape)
            bookmarks.remove(a)
            assertNull(bookmarks.get(a)); assertEquals(original, dao.getPostcardById(a))
            assertEquals(other, bookmarks.get(b))
        }
    }

    @Test fun deletingParentCascadesAndLateSaveCannotResurrectOrphan() = runBlocking {
        open(name()).useDatabase { room ->
            val dao = room.postcardDao(); val bookmarks = room.memoryBookmarkDao()
            val id = dao.insertPostcard(postcard())
            bookmarks.upsert(MemoryBookmark.from(id, BookmarkDesign()))
            dao.deletePostcardById(id)
            assertNull(bookmarks.get(id))
            var failed = false
            try { bookmarks.upsert(MemoryBookmark.from(id, BookmarkDesign())) } catch (_: SQLiteConstraintException) { failed = true }
            assertTrue(failed); assertNull(bookmarks.get(id)); assertNull(dao.getPostcardById(id))
        }
    }

    @Test fun repositoryCannotOverwriteFutureFormatInsideSaveTransaction() = runBlocking {
        open(name()).useDatabase { room ->
            val id = room.postcardDao().insertPostcard(postcard())
            val future = MemoryBookmark(id, "FUTURE", "CUSTOM", "OTHER", 2)
            room.memoryBookmarkDao().upsert(future)
            val repository = MemoryBookmarkRepository(room.memoryBookmarkDao())
            var rejected = false
            try { repository.save(id, BookmarkDesign()) } catch (_: IllegalStateException) { rejected = true }
            assertTrue(rejected)
            assertEquals(future, room.memoryBookmarkDao().get(id))
        }
    }

    @Test fun closedDatabaseSnapshotRestoresParentIdentityAndBookmarkAcrossReopen() = runBlocking {
        val source = name(); val target = name()
        var id = 0L
        val design = BookmarkDesign(BookmarkShape.MINI_TAG, BookmarkPaper.MINT, BookmarkTrace.POSTMARK)
        open(source).useDatabase { room ->
            id = room.postcardDao().insertPostcard(postcard())
            room.memoryBookmarkDao().upsert(MemoryBookmark.from(id, design))
        }
        context.getDatabasePath(source).copyTo(context.getDatabasePath(target))
        open(target).useDatabase { restored ->
            assertEquals(id, restored.postcardDao().getPostcardById(id)!!.id)
            assertEquals(design, restored.memoryBookmarkDao().get(id)!!.designOrNull())
            restored.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        }
    }

    @Test fun everyPaperSavesPerPostcardAndSurvivesReopen() = runBlocking {
        val file = name()
        val saved = mutableMapOf<Long, BookmarkDesign>()
        open(file).useDatabase { room ->
            val repository = MemoryBookmarkRepository(room.memoryBookmarkDao())
            BookmarkPaper.entries.forEachIndexed { i, paper ->
                val id = room.postcardDao().insertPostcard(postcard())
                val design = BookmarkDesign(BookmarkShape.entries[i % 3], paper, BookmarkTrace.entries[i % 3])
                repository.save(id, design)
                repository.save(id, design) // 같은 저장을 반복해도 한 장만 남아.
                saved[id] = design
            }
        }
        open(file).useDatabase { room ->
            val rows = room.memoryBookmarkDao().observeAll().first()
            assertEquals(saved, rows.associate { it.postcardId to it.designOrNull() })
            assertEquals(saved.size, rows.size)
        }
    }

    @Test fun galleryFlowFollowsSaveRemoveAndParentDeletion() = runBlocking {
        open(name()).useDatabase { room ->
            val repository = MemoryBookmarkRepository(room.memoryBookmarkDao())
            val a = room.postcardDao().insertPostcard(postcard()); val b = room.postcardDao().insertPostcard(postcard())
            val seen = Channel<Set<Long>>(Channel.UNLIMITED)
            val job = launch(Dispatchers.IO) {
                repository.observeAll().collect { rows -> seen.send(rows.map { it.postcardId }.toSet()) }
            }
            suspend fun awaitIds(expected: Set<Long>) = withTimeout(10_000) {
                while (seen.receive() != expected) Unit
            }
            awaitIds(emptySet())
            repository.save(a, BookmarkDesign()); awaitIds(setOf(a))
            repository.save(b, BookmarkDesign()); awaitIds(setOf(a, b))
            repository.remove(a); awaitIds(setOf(b))
            room.postcardDao().deletePostcardById(b); awaitIds(emptySet())
            job.cancelAndJoin()
        }
    }

    @Test fun concurrentSaveRemoveAndDeletionNeverLeaveAnOrphan() = runBlocking {
        open(name()).useDatabase { room ->
            val repository = MemoryBookmarkRepository(room.memoryBookmarkDao())
            repeat(20) { round ->
                val id = room.postcardDao().insertPostcard(postcard())
                val keep = room.postcardDao().insertPostcard(postcard())
                repository.save(keep, BookmarkDesign(paper = BookmarkPaper.ROSE))
                val jobs = listOf(
                    async(Dispatchers.IO) {
                        BookmarkPaper.entries.forEach { paper ->
                            try { repository.save(id, BookmarkDesign(paper = paper)) } catch (_: SQLiteConstraintException) { }
                        }
                    },
                    async(Dispatchers.IO) { if (round % 2 == 0) repository.remove(id) },
                    async(Dispatchers.IO) { room.postcardDao().deletePostcardById(id) }
                )
                jobs.awaitAll()
                assertNull(room.postcardDao().getPostcardById(id))
                assertNull(repository.get(id))
                assertEquals(BookmarkPaper.ROSE, repository.get(keep)!!.designOrNull()!!.paper)
            }
            room.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        }
    }

    /** schema 19 백업 파일을 되돌려 놓은 상황: 닫힌 사본을 여러 번 열어도 한 번만 migration되고 엽서가 남아. */
    @Test fun restoredSchema19CopyMigratesOnceAndAcceptsBookmarks() = runBlocking {
        val source = context.getDatabasePath(name()).apply { parentFile!!.mkdirs() }
        val schema = listOf(File("schemas/com.postcardmemory.data.PostcardDatabase/19.json"),
            File("app/schemas/com.postcardmemory.data.PostcardDatabase/19.json")).first(File::exists)
        val json = org.json.JSONObject(schema.readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(source, null).use { raw ->
            raw.execSQL(json.getJSONArray("entities").getJSONObject(0).getString("createSql").replace("\${TABLE_NAME}", "postcards"))
            val setup = json.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) raw.execSQL(setup.getString(i))
            for (id in listOf(3, 8)) raw.execSQL("""INSERT INTO postcards
                (id,imagePath,title,capturedAt,backgroundColorArgb,message) VALUES ($id,'p$id','t',1,4294966263,'m$id')""")
            raw.version = 19
        }
        val restored = name()
        source.copyTo(context.getDatabasePath(restored))
        open(restored).useDatabase { room ->
            MemoryBookmarkRepository(room.memoryBookmarkDao()).save(8, BookmarkDesign(paper = BookmarkPaper.BLUE))
        }
        open(restored).useDatabase { room ->
            assertEquals("m3", room.postcardDao().getPostcardById(3)!!.message)
            assertEquals("m8", room.postcardDao().getPostcardById(8)!!.message)
            assertEquals(listOf(8L), room.memoryBookmarkDao().observeAll().first().map { it.postcardId })
            assertEquals(20, room.openHelper.readableDatabase.version)
        }
    }
}
