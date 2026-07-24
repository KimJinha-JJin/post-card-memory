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

    @Query(
        """
        UPDATE postcards
        SET imagePath = :imagePath
        WHERE id = :id
        """
    )
    suspend fun updatePostcardImagePath(
        id: Long,
        imagePath: String
    )

    @Query(
        """
        UPDATE postcards
        SET photoEdgeBlur = :photoEdgeBlur
        WHERE id = :id
        """
    )
    suspend fun updatePostcardPhotoEdgeBlur(
        id: Long,
        photoEdgeBlur: Float
    )

    @Query(
        """
        UPDATE postcards
        SET stampPhotoOffsetX = :stampPhotoOffsetX,
            stampPhotoOffsetY = :stampPhotoOffsetY
        WHERE id = :id
        """
    )
    suspend fun updatePostcardStampPhotoOffset(
        id: Long,
        stampPhotoOffsetX: Float,
        stampPhotoOffsetY: Float
    )

    @Query(
        """
        UPDATE postcards
        SET polaroidPhotoOffsetX = :polaroidPhotoOffsetX,
            polaroidPhotoOffsetY = :polaroidPhotoOffsetY
        WHERE id = :id
        """
    )
    suspend fun updatePostcardPolaroidPhotoOffset(
        id: Long,
        polaroidPhotoOffsetX: Float,
        polaroidPhotoOffsetY: Float
    )

    @Query(
        """
        UPDATE postcards
        SET tapedFilmPhotoOffsetX = :tapedFilmPhotoOffsetX,
            tapedFilmPhotoOffsetY = :tapedFilmPhotoOffsetY
        WHERE id = :id
        """
    )
    suspend fun updatePostcardTapedFilmPhotoOffset(
        id: Long,
        tapedFilmPhotoOffsetX: Float,
        tapedFilmPhotoOffsetY: Float
    )

    @Query(
        """
        UPDATE postcards
        SET stampPhotoZoom = :stampPhotoZoom
        WHERE id = :id
        """
    )
    suspend fun updatePostcardStampPhotoZoom(
        id: Long,
        stampPhotoZoom: Float
    )

    @Query(
        """
        UPDATE postcards
        SET polaroidPhotoZoom = :polaroidPhotoZoom
        WHERE id = :id
        """
    )
    suspend fun updatePostcardPolaroidPhotoZoom(
        id: Long,
        polaroidPhotoZoom: Float
    )

    @Query(
        """
        UPDATE postcards
        SET tapedFilmPhotoZoom = :tapedFilmPhotoZoom
        WHERE id = :id
        """
    )
    suspend fun updatePostcardTapedFilmPhotoZoom(
        id: Long,
        tapedFilmPhotoZoom: Float
    )

    /**
     * 템플릿 적용/되돌리기 전용 일괄 업데이트. 개별 필드 업데이트 함수들은
     * 사용자가 슬라이더 하나씩 조작할 때 쓰는 디바운스 저장 경로라, 템플릿처럼
     * 20개 스타일 값을 한 번에 바꾸는 동작에는 값마다 별도 쿼리를 호출하는
     * 대신 이 쿼리 하나로 원자적으로 반영한다. backgroundImagePath는
     * 템플릿이 다루지 않는 값이라 여기서 건드리지 않는다.
     */
    @Query(
        """
        UPDATE postcards
        SET layoutStyle = :layoutStyle,
            backgroundColorArgb = :backgroundColorArgb,
            backgroundPattern = :backgroundPattern,
            backgroundPatternDensity = :backgroundPatternDensity,
            messageFont = :messageFont,
            dateFormat = :dateFormat,
            messageTextScale = :messageTextScale,
            dateTextScale = :dateTextScale,
            photoEdgeBlur = :photoEdgeBlur,
            stampPhotoScale = :stampPhotoScale,
            stampPhotoOffsetX = :stampPhotoOffsetX,
            stampPhotoOffsetY = :stampPhotoOffsetY,
            stampPhotoZoom = :stampPhotoZoom,
            polaroidPhotoScale = :polaroidPhotoScale,
            polaroidPhotoOffsetX = :polaroidPhotoOffsetX,
            polaroidPhotoOffsetY = :polaroidPhotoOffsetY,
            polaroidPhotoZoom = :polaroidPhotoZoom,
            tapedFilmPhotoOffsetX = :tapedFilmPhotoOffsetX,
            tapedFilmPhotoOffsetY = :tapedFilmPhotoOffsetY,
            tapedFilmPhotoZoom = :tapedFilmPhotoZoom
        WHERE id = :id
        """
    )
    suspend fun updatePostcardTemplateStyle(
        id: Long,
        layoutStyle: String,
        backgroundColorArgb: Long,
        backgroundPattern: String,
        backgroundPatternDensity: Float,
        messageFont: String,
        dateFormat: String,
        messageTextScale: Float,
        dateTextScale: Float,
        photoEdgeBlur: Float,
        stampPhotoScale: Float,
        stampPhotoOffsetX: Float,
        stampPhotoOffsetY: Float,
        stampPhotoZoom: Float,
        polaroidPhotoScale: Float,
        polaroidPhotoOffsetX: Float,
        polaroidPhotoOffsetY: Float,
        polaroidPhotoZoom: Float,
        tapedFilmPhotoOffsetX: Float,
        tapedFilmPhotoOffsetY: Float,
        tapedFilmPhotoZoom: Float
    )
}