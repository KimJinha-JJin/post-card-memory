package com.postcardmemory.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PostcardDao {

    @Query(
        "SELECT * FROM postcards " +
                "ORDER BY capturedAt DESC"
    )
    fun getAllPostcards(): Flow<List<Postcard>>

    @Query(
        "SELECT * FROM postcards " +
                "WHERE id = :id"
    )
    suspend fun getPostcardById(
        id: Long
    ): Postcard?

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertPostcard(
        postcard: Postcard
    ): Long

    @Delete
    suspend fun deletePostcard(
        postcard: Postcard
    )

    @Query(
        "DELETE FROM postcards " +
                "WHERE id = :id"
    )
    suspend fun deletePostcardById(
        id: Long
    )

    @Query(
        "DELETE FROM postcards " +
                "WHERE id IN (:ids)"
    )
    suspend fun deletePostcardsByIds(
        ids: List<Long>
    )

    @Query(
        "UPDATE postcards " +
                "SET message = :message " +
                "WHERE id = :id"
    )
    suspend fun updatePostcardMessage(
        id: Long,
        message: String
    )

    @Query(
        """
        UPDATE postcards
        SET backgroundColorArgb = :backgroundColorArgb,
            backgroundImagePath = :backgroundImagePath
        WHERE id = :id
        """
    )
    suspend fun updatePostcardBackground(
        id: Long,
        backgroundColorArgb: Long,
        backgroundImagePath: String?
    )

    @Query(
        """
        UPDATE postcards
        SET backgroundPattern = :backgroundPattern
        WHERE id = :id
        """
    )
    suspend fun updatePostcardBackgroundPattern(
        id: Long,
        backgroundPattern: String
    )

    @Query(
        """
        UPDATE postcards
        SET messageFont = :messageFont
        WHERE id = :id
        """
    )
    suspend fun updatePostcardMessageFont(
        id: Long,
        messageFont: String
    )

    @Query(
        """
        UPDATE postcards
        SET layoutStyle = :layoutStyle
        WHERE id = :id
        """
    )
    suspend fun updatePostcardLayoutStyle(
        id: Long,
        layoutStyle: String
    )

    @Query(
        """
        UPDATE postcards
        SET dateFormat = :dateFormat
        WHERE id = :id
        """
    )
    suspend fun updatePostcardDateFormat(
        id: Long,
        dateFormat: String
    )

    @Query(
        """
        UPDATE postcards
        SET messageTextScale = :messageTextScale
        WHERE id = :id
        """
    )
    suspend fun updatePostcardMessageTextScale(
        id: Long,
        messageTextScale: Float
    )

    @Query(
        """
        UPDATE postcards
        SET dateTextScale = :dateTextScale
        WHERE id = :id
        """
    )
    suspend fun updatePostcardDateTextScale(
        id: Long,
        dateTextScale: Float
    )

    @Query(
        """
        UPDATE postcards
        SET backgroundPatternDensity = :backgroundPatternDensity
        WHERE id = :id
        """
    )
    suspend fun updatePostcardBackgroundPatternDensity(
        id: Long,
        backgroundPatternDensity: Float
    )

    @Query(
        """
        UPDATE postcards
        SET stampPhotoScale = :stampPhotoScale
        WHERE id = :id
        """
    )
    suspend fun updatePostcardStampPhotoScale(
        id: Long,
        stampPhotoScale: Float
    )

    @Query(
        """
        UPDATE postcards
        SET polaroidPhotoScale = :polaroidPhotoScale
        WHERE id = :id
        """
    )
    suspend fun updatePostcardPolaroidPhotoScale(
        id: Long,
        polaroidPhotoScale: Float
    )
}