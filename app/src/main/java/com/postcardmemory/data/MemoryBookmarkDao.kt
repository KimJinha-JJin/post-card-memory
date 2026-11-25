package com.postcardmemory.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryBookmarkDao {
    @Query("SELECT * FROM memory_bookmarks")
    fun observeAll(): Flow<List<MemoryBookmark>>

    @Query("SELECT * FROM memory_bookmarks WHERE postcardId = :postcardId")
    fun observe(postcardId: Long): Flow<MemoryBookmark?>

    @Query("SELECT * FROM memory_bookmarks WHERE postcardId = :postcardId")
    suspend fun get(postcardId: Long): MemoryBookmark?

    @Upsert
    suspend fun upsert(bookmark: MemoryBookmark)

    @Transaction
    suspend fun saveCurrentFormat(bookmark: MemoryBookmark) {
        val existing = get(bookmark.postcardId)
        check(existing == null || existing.formatVersion == 1) { "지원하지 않는 책갈피 형식이야." }
        upsert(bookmark)
    }

    @Query("DELETE FROM memory_bookmarks WHERE postcardId = :postcardId")
    suspend fun remove(postcardId: Long)
}
