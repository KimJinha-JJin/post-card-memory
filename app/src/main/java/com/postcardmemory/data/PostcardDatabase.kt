package com.postcardmemory.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Postcard::class],
    version = 8,
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
    }
}