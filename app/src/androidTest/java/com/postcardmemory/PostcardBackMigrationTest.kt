package com.postcardmemory

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.postcardmemory.data.Postcard
import com.postcardmemory.data.PostcardDatabase
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostcardBackMigrationTest {
    @Test fun migrateRealVersion18AndVerifyPreservationAndNewWrites() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "day66_test_${UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        var room: PostcardDatabase? = null
        try {
            val schema = JSONObject(instrumentation.context.assets.open(
                "com.postcardmemory.data.PostcardDatabase/18.json"
            ).bufferedReader().use { it.readText() }).getJSONObject("database")
            SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
                val entities = schema.getJSONArray("entities")
                for (index in 0 until entities.length()) {
                    val entity = entities.getJSONObject(index)
                    db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                }
                val setup = schema.getJSONArray("setupQueries")
                for (index in 0 until setup.length()) db.execSQL(setup.getString(index))
                db.execSQL("INSERT INTO postcards (id,imagePath,title,capturedAt,backgroundColorArgb,backMessage) VALUES (1,'legacy-photo','legacy-title',123,4294966263,'옛 편지')")
                db.version = 18
            }
            fun open() = Room.databaseBuilder(context, PostcardDatabase::class.java, name)
                .addMigrations(PostcardDatabase.MIGRATION_18_19).build()
            room = open()
            val dao = room.postcardDao()
            val old = checkNotNull(dao.getPostcardById(1)) // Room validates migrated schema here
            assertEquals("옛 편지", old.backMessage)
            assertEquals("legacy-photo", old.imagePath)
            assertEquals(123L, old.capturedAt)
            assertNull(old.backPostscript)
            assertNull(old.backWrittenAt)
            assertNull(old.backWrittenOffsetMinutes)
            assertFalse(old.backWritingRecordEnabled)
            dao.updatePostcardBackMessage(1, "옛 편지 수정", 1000, 540)
            assertNull(dao.getPostcardById(1)!!.backWrittenAt)

            val id = dao.insertPostcard(Postcard(imagePath = "new-photo", title = "new-title"))
            dao.updatePostcardBackMessage(id, "첫 본문", 2000, 540)
            dao.updatePostcardBackPostscript(id, "추신")
            dao.updatePostcardBackMessage(id, "수정", 9000, -300)
            room.close()
            room = open()
            val restored = room.postcardDao().getPostcardById(id)!!
            assertEquals("수정", restored.backMessage)
            assertEquals("추신", restored.backPostscript)
            assertEquals(2000L, restored.backWrittenAt)
            assertEquals(540, restored.backWrittenOffsetMinutes)
            room.postcardDao().updatePostcardBackMessage(id, "", null, null)
            room.postcardDao().updatePostcardBackPostscript(id, null)
            val cleared = room.postcardDao().getPostcardById(id)!!
            assertNull(cleared.backPostscript)
            assertEquals(2000L, cleared.backWrittenAt)
            assertEquals(540, cleared.backWrittenOffsetMinutes)
        } finally {
            room?.close()
            context.deleteDatabase(name) // isolated test database only
        }
    }
}
