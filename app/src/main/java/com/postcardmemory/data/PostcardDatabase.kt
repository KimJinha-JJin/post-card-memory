package com.postcardmemory.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Postcard::class],
    version = 18,
    exportSchema = false
)
abstract class PostcardDatabase : RoomDatabase() {

    abstract fun postcardDao(): PostcardDao

    companion object {

        val MIGRATION_1_2 =
            object : Migration(
                startVersion = 1,
                endVersion = 2
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN message TEXT NOT NULL DEFAULT ''
                        """.trimIndent()
                    )
                }
            }

        val MIGRATION_2_3 =
            object : Migration(
                startVersion = 2,
                endVersion = 3
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        CREATE TABLE postcards_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            imagePath TEXT NOT NULL,
                            title TEXT NOT NULL,
                            capturedAt INTEGER NOT NULL,
                            location TEXT,
                            message TEXT NOT NULL,
                            backgroundColorArgb INTEGER NOT NULL,
                            backgroundImagePath TEXT
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        INSERT INTO postcards_new (
                            id,
                            imagePath,
                            title,
                            capturedAt,
                            location,
                            message,
                            backgroundColorArgb,
                            backgroundImagePath
                        )
                        SELECT
                            id,
                            imagePath,
                            title,
                            capturedAt,
                            location,
                            message,
                            4294966263,
                            NULL
                        FROM postcards
                        """.trimIndent()
                    )

                    database.execSQL(
                        "DROP TABLE postcards"
                    )

                    database.execSQL(
                        """
                        ALTER TABLE postcards_new
                        RENAME TO postcards
                        """.trimIndent()
                    )
                }
            }

        val MIGRATION_3_4 =
            object : Migration(
                startVersion = 3,
                endVersion = 4
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN backgroundPattern TEXT NOT NULL DEFAULT 'NONE'
                        """.trimIndent()
                    )
                }
            }

        val MIGRATION_4_5 =
            object : Migration(
                startVersion = 4,
                endVersion = 5
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN messageFont TEXT NOT NULL DEFAULT 'SERIF'
                        """.trimIndent()
                    )
                }
            }

        val MIGRATION_5_6 =
            object : Migration(
                startVersion = 5,
                endVersion = 6
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN layoutStyle TEXT NOT NULL DEFAULT 'STANDARD'
                        """.trimIndent()
                    )
                }
            }

        val MIGRATION_6_7 =
            object : Migration(
                startVersion = 6,
                endVersion = 7
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN dateFormat TEXT NOT NULL DEFAULT 'DOT'
                        """.trimIndent()
                    )
                }
            }

        val MIGRATION_7_8 =
            object : Migration(
                startVersion = 7,
                endVersion = 8
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN messageTextScale REAL NOT NULL DEFAULT 1.0
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN dateTextScale REAL NOT NULL DEFAULT 1.0
                        """.trimIndent()
                    )
                }
            }

        val MIGRATION_8_9 =
            object : Migration(
                startVersion = 8,
                endVersion = 9
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN backgroundPatternDensity REAL NOT NULL DEFAULT 1.0
                        """.trimIndent()
                    )
                }
            }

        val MIGRATION_9_10 =
            object : Migration(
                startVersion = 9,
                endVersion = 10
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN stampPhotoScale REAL NOT NULL DEFAULT 1.0
                        """.trimIndent()
                    )
                }
            }

        val MIGRATION_10_11 =
            object : Migration(
                startVersion = 10,
                endVersion = 11
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN polaroidPhotoScale REAL NOT NULL DEFAULT 1.0
                        """.trimIndent()
                    )
                }
            }

        val MIGRATION_11_12 =
            object : Migration(
                startVersion = 11,
                endVersion = 12
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN photoEdgeBlur REAL NOT NULL DEFAULT 0.0
                        """.trimIndent()
                    )
                }
            }

        val MIGRATION_12_13 =
            object : Migration(
                startVersion = 12,
                endVersion = 13
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN stampPhotoOffsetX REAL NOT NULL DEFAULT 0.0
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN stampPhotoOffsetY REAL NOT NULL DEFAULT 0.0
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN polaroidPhotoOffsetX REAL NOT NULL DEFAULT 0.0
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN polaroidPhotoOffsetY REAL NOT NULL DEFAULT 0.0
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN tapedFilmPhotoOffsetX REAL NOT NULL DEFAULT 0.0
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN tapedFilmPhotoOffsetY REAL NOT NULL DEFAULT 0.0
                        """.trimIndent()
                    )
                }
            }

        val MIGRATION_13_14 =
            object : Migration(
                startVersion = 13,
                endVersion = 14
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN stampPhotoZoom REAL NOT NULL DEFAULT 1.0
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN polaroidPhotoZoom REAL NOT NULL DEFAULT 1.0
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN tapedFilmPhotoZoom REAL NOT NULL DEFAULT 1.0
                        """.trimIndent()
                    )
                }
            }

        /**
         * 5→6 Migration 도입 당시(STANDARD/PHOTO_FOCUS/AIRY/MAGAZINE/POLAROID
         * 5종 레이아웃 시절) 기본값이 'STANDARD'였고, 이후 Stamp/Polaroid
         * 2종으로 통합되며(0d92834) Kotlin 쪽 기본값만 'STAMP'로 바뀌었을 뿐
         * 이미 저장된 행을 정규화하는 Migration은 그때 추가되지 않았다.
         * updateLayoutStyle()이 한 번도 다시 호출되지 않은 오래된 엽서는
         * 지금도 문자 그대로 'STANDARD' 등 옛 값을 들고 있을 수 있다 —
         * 현재 유효한 4개 값(STAMP/POLAROID/TAPED_FILM/LETTER) 밖의 값을
         * normalizeLayoutStyle()과 동일한 기준으로 STAMP로 정규화한다.
         */
        val MIGRATION_14_15 =
            object : Migration(
                startVersion = 14,
                endVersion = 15
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        UPDATE postcards
                        SET layoutStyle = 'STAMP'
                        WHERE layoutStyle NOT IN ('STAMP', 'POLAROID', 'TAPED_FILM', 'LETTER')
                        """.trimIndent()
                    )
                }
            }

        /**
         * "미래의 나에게 보내기" 기능. 기존 행은 전부 futureMailState='NONE',
         * futureMailDeliverAt=NULL로 채워져 지금까지와 동일하게 갤러리에
         * 보인다(DEFAULT 'NONE' + ADD COLUMN은 기존 행에도 소급 적용됨).
         */
        val MIGRATION_15_16 =
            object : Migration(
                startVersion = 15,
                endVersion = 16
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN futureMailState TEXT NOT NULL DEFAULT 'NONE'
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN futureMailDeliverAt INTEGER DEFAULT NULL
                        """.trimIndent()
                    )
                }
            }

        /**
         * 봉투 기능. 기존 행은 전부 envelopeStyle=NULL, envelopePostmarked=0으로
         * 채워져 지금까지와 동일하게 봉투 없는 엽서 보기로 열린다(DEFAULT +
         * ADD COLUMN은 기존 행에도 소급 적용됨). 미니 도장·앞면 우편 소인(seal_states
         * 파일)은 이 테이블과 무관해 전혀 영향받지 않는다.
         */
        val MIGRATION_16_17 =
            object : Migration(
                startVersion = 16,
                endVersion = 17
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN envelopeStyle TEXT DEFAULT NULL
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN envelopePostmarked INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )
                }
            }

        /**
         * 엽서 뒷면 편지 기능. 기존 행은 전부 backRecipientModifier='',
         * backMessage=''로 채워져 뒤집었을 때 빈 편지 상태로 정상 진입한다
         * (DEFAULT + ADD COLUMN은 기존 행에도 소급 적용됨). 앞면 데이터·
         * 봉투 휴면 필드는 이 migration과 무관해 전혀 영향받지 않는다.
         */
        val MIGRATION_17_18 =
            object : Migration(
                startVersion = 17,
                endVersion = 18
            ) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN backRecipientModifier TEXT NOT NULL DEFAULT ''
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE postcards
                        ADD COLUMN backMessage TEXT NOT NULL DEFAULT ''
                        """.trimIndent()
                    )
                }
            }
    }
}