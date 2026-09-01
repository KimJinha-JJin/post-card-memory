package com.postcardmemory.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Room Migration의 SQL 정확성은 schema JSON을 사용하는 계측 테스트가 맡아야
 * 한다. 이 순수 JUnit 안전망은 그 전 단계에서 현재 DB version까지의 연속
 * Migration 선언·등록 누락과 schema export 비활성화를 빠르게 잡는다.
 */
class PostcardMigrationRegistrationStructureTest {

    private fun sourceText(vararg candidates: String): String {
        val file = candidates
            .map(::File)
            .firstOrNull(File::exists)
            ?: error("소스 파일을 찾을 수 없음(cwd=${File(".").absolutePath})")

        return file.readText()
    }

    private val databaseText: String by lazy {
        sourceText(
            "src/main/java/com/postcardmemory/data/PostcardDatabase.kt",
            "app/src/main/java/com/postcardmemory/data/PostcardDatabase.kt"
        )
    }

    private val databaseModuleText: String by lazy {
        sourceText(
            "src/main/java/com/postcardmemory/di/DatabaseModule.kt",
            "app/src/main/java/com/postcardmemory/di/DatabaseModule.kt"
        )
    }

    private val databaseVersion: Int by lazy {
        Regex("""version\s*=\s*(\d+)""")
            .find(databaseText)
            ?.groupValues
            ?.get(1)
            ?.toInt()
            ?: error("PostcardDatabase version을 찾을 수 없음")
    }

    private val expectedMigrationPairs: List<Pair<Int, Int>> by lazy {
        (1 until databaseVersion).map { start -> start to start + 1 }
    }

    @Test
    fun schemaExport_isEnabled_andCurrentSchemaFileExists() {
        assertTrue(
            "Room schema export가 꺼져 있으면 다음 Migration의 기준 JSON을 남길 수 없다",
            Regex("""exportSchema\s*=\s*true""").containsMatchIn(databaseText)
        )

        val relativeSchemaPath =
            "schemas/com.postcardmemory.data.PostcardDatabase/$databaseVersion.json"
        val schemaFile = listOf(
            File(relativeSchemaPath),
            File("app/$relativeSchemaPath")
        ).firstOrNull(File::exists)

        assertTrue(
            "현재 DB version $databaseVersion schema JSON이 없다",
            schemaFile != null
        )
        assertTrue(
            "schema JSON의 version이 PostcardDatabase와 다르다",
            schemaFile!!.readText().contains("\"version\": $databaseVersion")
        )
    }

    @Test
    fun migrationDeclarations_areContinuousThroughCurrentVersion() {
        val declaredPairs =
            Regex("""val\s+MIGRATION_(\d+)_(\d+)""")
                .findAll(databaseText)
                .map { match ->
                    match.groupValues[1].toInt() to
                        match.groupValues[2].toInt()
                }
                .toList()

        assertEquals(expectedMigrationPairs, declaredPairs)
    }

    @Test
    fun databaseModule_registersEveryDeclaredMigrationInOrder() {
        val addMigrationsStart =
            databaseModuleText.indexOf(".addMigrations(")
        assertTrue("DatabaseModule.addMigrations를 찾을 수 없다", addMigrationsStart >= 0)

        val addMigrationsEnd =
            databaseModuleText.indexOf("\n            )", addMigrationsStart)
        assertTrue("DatabaseModule.addMigrations 끝을 찾을 수 없다", addMigrationsEnd >= 0)

        val registeredPairs =
            Regex("""PostcardDatabase\.MIGRATION_(\d+)_(\d+)""")
                .findAll(
                    databaseModuleText.substring(
                        addMigrationsStart,
                        addMigrationsEnd
                    )
                )
                .map { match ->
                    match.groupValues[1].toInt() to
                        match.groupValues[2].toInt()
                }
                .toList()

        assertEquals(expectedMigrationPairs, registeredPairs)
    }
}
