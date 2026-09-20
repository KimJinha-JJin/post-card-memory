package com.postcardmemory

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.postcardmemory.data.Postcard
import com.postcardmemory.data.PostcardDatabase
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * 79일차: `1 → 18` 구간은 실제 v1 schema 실행 검증이 약하다고 판단해 추가한
 * migration chain 테스트. `PostcardBackMigrationTest`(18→19, `18.json` schema
 * 자산 사용)와 달리 v1~v17은 exportSchema 자산이 없다(`exportSchema=true`가
 * 2026-08-31 "Add migration baseline" 커밋에서 처음 켜졌다). 대신 초기 커밋
 * (`8d95bc35`)의 `Postcard.kt`/`PostcardDatabase.kt`가 정의한 v1 schema를 그대로
 * 손으로 재현하고, 이후 모든 열 추가는 상상이 아니라 `PostcardDatabase`에 이미
 * 있는 실제 `MIGRATION_x_y` 객체를 그대로 실행해서 얻는다 — 별도 fixture나
 * 새 dependency 없이 production migration 코드 자체가 fixture다.
 *
 * v1 원본 schema(참고, 재현 대상):
 * ```
 * CREATE TABLE postcards (
 *   id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
 *   imagePath TEXT NOT NULL,
 *   title TEXT NOT NULL,
 *   capturedAt INTEGER NOT NULL,
 *   location TEXT
 * )
 * ```
 */
class PostcardFullMigrationChainTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun createV1Database(name: String): SupportSQLiteOpenHelper {
        val factory = FrameworkSQLiteOpenHelperFactory()
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE postcards (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            imagePath TEXT NOT NULL,
                            title TEXT NOT NULL,
                            capturedAt INTEGER NOT NULL,
                            location TEXT
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // 이 테스트는 raw helper로 초기 상태만 만들고, 이후 버전 전환은
                    // production MIGRATION_x_y 객체를 직접 호출하거나 Room이 처리한다.
                }
            })
            .build()
        return factory.create(configuration)
    }

    /** v1의 옛 사용자 데이터가 전체 migration chain(1→19)을 지나 실제 Room DAO로 살아남는지 확인한다. */
    @Test fun migrateFromVersion1PreservesLegacyDataThroughFullChain() = runBlocking {
        val name = "day79_v1chain_${UUID.randomUUID()}.db"
        var room: PostcardDatabase? = null
        try {
            createV1Database(name).use { helper ->
                helper.writableDatabase.use { db ->
                    db.execSQL(
                        """
                        INSERT INTO postcards (id, imagePath, title, capturedAt, location)
                        VALUES (1, 'legacy-photo', 'legacy-title', 1000, '옛 장소')
                        """.trimIndent()
                    )
                    db.version = 1
                }
            }

            room = Room.databaseBuilder(context, PostcardDatabase::class.java, name)
                .addMigrations(
                    PostcardDatabase.MIGRATION_1_2,
                    PostcardDatabase.MIGRATION_2_3,
                    PostcardDatabase.MIGRATION_3_4,
                    PostcardDatabase.MIGRATION_4_5,
                    PostcardDatabase.MIGRATION_5_6,
                    PostcardDatabase.MIGRATION_6_7,
                    PostcardDatabase.MIGRATION_7_8,
                    PostcardDatabase.MIGRATION_8_9,
                    PostcardDatabase.MIGRATION_9_10,
                    PostcardDatabase.MIGRATION_10_11,
                    PostcardDatabase.MIGRATION_11_12,
                    PostcardDatabase.MIGRATION_12_13,
                    PostcardDatabase.MIGRATION_13_14,
                    PostcardDatabase.MIGRATION_14_15,
                    PostcardDatabase.MIGRATION_15_16,
                    PostcardDatabase.MIGRATION_16_17,
                    PostcardDatabase.MIGRATION_17_18,
                    PostcardDatabase.MIGRATION_18_19
                )
                .build()

            val dao = room.postcardDao()
            val old = checkNotNull(dao.getPostcardById(1)) // Room이 이 시점에 최종 schema를 검증한다.

            // 원래 v1 값은 그대로 보존된다.
            assertEquals("legacy-photo", old.imagePath)
            assertEquals("legacy-title", old.title)
            assertEquals(1000L, old.capturedAt)
            assertEquals("옛 장소", old.location)

            // 2→3의 하드코딩 backfill: 옛 행은 모두 이 값으로 채워진다(MIGRATION_2_3 참고).
            assertEquals(4294966263L, old.backgroundColorArgb)
            assertNull(old.backgroundImagePath)
            assertEquals("", old.message)

            // 3→18 사이 ADD COLUMN 기본값들이 옛 행에도 소급 적용된다.
            assertEquals("NONE", old.backgroundPattern)
            assertEquals("SERIF", old.messageFont)
            assertEquals("STAMP", old.layoutStyle)
            assertEquals("DOT", old.dateFormat)
            assertEquals(1.0f, old.messageTextScale)
            assertEquals("NONE", old.futureMailState)
            assertNull(old.futureMailDeliverAt)
            assertNull(old.envelopeStyle)
            assertFalse(old.envelopePostmarked)
            assertEquals("", old.backRecipientModifier)
            assertEquals("", old.backMessage)
            assertNull(old.backPostscript)
            assertNull(old.backWrittenAt)
            assertFalse(old.backWritingRecordEnabled)

            // 최신 DAO write/read가 migrate된 행 위에서 정상 동작한다.
            dao.updatePostcardBackMessage(1, "새 편지", 5000, 540)
            assertEquals("새 편지", dao.getPostcardById(1)!!.backMessage)

            val newId = dao.insertPostcard(Postcard(imagePath = "new-photo", title = "new-title"))
            assertNotNull(dao.getPostcardById(newId))

            room.close()
            room = Room.databaseBuilder(context, PostcardDatabase::class.java, name).build()
            val restored = room.postcardDao().getPostcardById(1)!!
            assertEquals("새 편지", restored.backMessage)
            assertEquals(4294966263L, restored.backgroundColorArgb)
        } finally {
            room?.close()
            context.deleteDatabase(name)
        }
    }

    /**
     * layoutStyle이 STAMP/POLAROID 2종으로 통합되기 전(v5→v6 시절 STANDARD 기본값,
     * 이후 AIRY 등 폐기값)에 저장된 옛 값이 14→15에서 실제로 정규화되는지 확인한다.
     * v14 상태까지는 production MIGRATION_1_2..MIGRATION_13_14를 그대로 실행해
     * 도달하고, 그 위에 옛 폐기값 'AIRY'를 직접 심는다.
     */
    @Test fun migration14to15NormalizesLegacyLayoutStyleValue() = runBlocking {
        val name = "day79_v14chain_${UUID.randomUUID()}.db"
        var room: PostcardDatabase? = null
        try {
            createV1Database(name).use { helper ->
                helper.writableDatabase.use { db ->
                    db.execSQL(
                        """
                        INSERT INTO postcards (id, imagePath, title, capturedAt, location)
                        VALUES (1, 'legacy-photo', 'legacy-title', 1000, NULL)
                        """.trimIndent()
                    )
                    PostcardDatabase.MIGRATION_1_2.migrate(db)
                    PostcardDatabase.MIGRATION_2_3.migrate(db)
                    PostcardDatabase.MIGRATION_3_4.migrate(db)
                    PostcardDatabase.MIGRATION_4_5.migrate(db)
                    PostcardDatabase.MIGRATION_5_6.migrate(db)
                    PostcardDatabase.MIGRATION_6_7.migrate(db)
                    PostcardDatabase.MIGRATION_7_8.migrate(db)
                    PostcardDatabase.MIGRATION_8_9.migrate(db)
                    PostcardDatabase.MIGRATION_9_10.migrate(db)
                    PostcardDatabase.MIGRATION_10_11.migrate(db)
                    PostcardDatabase.MIGRATION_11_12.migrate(db)
                    PostcardDatabase.MIGRATION_12_13.migrate(db)
                    PostcardDatabase.MIGRATION_13_14.migrate(db)
                    // v14 상태에 폐기된 레이아웃 값을 직접 심는다.
                    db.execSQL("UPDATE postcards SET layoutStyle = 'AIRY' WHERE id = 1")
                    db.version = 14
                }
            }

            room = Room.databaseBuilder(context, PostcardDatabase::class.java, name)
                .addMigrations(
                    PostcardDatabase.MIGRATION_14_15,
                    PostcardDatabase.MIGRATION_15_16,
                    PostcardDatabase.MIGRATION_16_17,
                    PostcardDatabase.MIGRATION_17_18,
                    PostcardDatabase.MIGRATION_18_19
                )
                .build()

            val normalized = checkNotNull(room.postcardDao().getPostcardById(1))
            assertEquals("STAMP", normalized.layoutStyle)
        } finally {
            room?.close()
            context.deleteDatabase(name)
        }
    }
}
