package com.postcardmemory.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PostcardDao {
    @Query("SELECT * FROM postcards ORDER BY capturedAt DESC")
    fun getAllPostcards(): Flow<List<Postcard>>

    @Query("SELECT * FROM postcards WHERE id = :id")
    suspend fun getPostcardById(id: Long): Postcard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPostcard(postcard: Postcard): Long

    @Delete
    suspend fun deletePostcard(postcard: Postcard)

    @Query("DELETE FROM postcards WHERE id = :id")
    suspend fun deletePostcardById(id: Long)
}
