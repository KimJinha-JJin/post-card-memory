package com.postcardmemory.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PostcardDao {

    /**
     * 일반 갤러리(3열 그리드/세부 기록 보기 공통 원천)용 조회. 미래로
     * 발송된(futureMailState != 'NONE') 엽서는 여기서 제외해 두 보기가
     * 항상 같은 결과를 보게 한다.
     */
    @Query(
        "SELECT * FROM postcards " +
                "WHERE futureMailState = 'NONE' " +
                "ORDER BY capturedAt DESC"
    )
    fun getAllPostcards(): Flow<List<Postcard>>

    /**
     * 미래 우체통용 조회. 상태와 무관하게 id로 직접 조회하는
     * getPostcardById()와 달리, 배송 중인 엽서만 도착일 오름차순으로 가져온다.
     */
    @Query(
        "SELECT * FROM postcards " +
                "WHERE futureMailState = 'SENT' " +
                "ORDER BY futureMailDeliverAt ASC"
    )
    fun getFutureMailPostcards(): Flow<List<Postcard>>

    /**
     * id로 직접 조회 — 상태 필터를 걸지 않는다. 미래로 발송된 엽서도
     * (내용을 보여주기 위해서가 아니라) 봉인 안내 화면을 그리기 위해
     * DetailViewModel이 이 함수로 로드해야 하므로, 노출 차단은 DAO가 아니라
     * UI 레이어(DetailScreen)의 상태 분기 책임이다.
     */
    @Query(
        "SELECT * FROM postcards " +
                "WHERE id = :id"
    )
    suspend fun getPostcardById(
        id: Long
    ): Postcard?

    @Query(
        """
        UPDATE postcards
        SET futureMailState = 'SENT',
            futureMailDeliverAt = :deliverAt
        WHERE id = :id
        """
    )
    suspend fun sendToFutureMailbox(
        id: Long,
        deliverAt: Long
    )

    /** 개봉: 상태를 되돌리는 것뿐, 파일/이미지 등 어떤 자산도 건드리지 않는다. */
    @Query(
        """
        UPDATE postcards
        SET futureMailState = 'NONE',
            futureMailDeliverAt = NULL
        WHERE id = :id
        """
    )
    suspend fun openFutureMail(
        id: Long
    )

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

    /** 봉투에 넣기/바꾸기 전용. 소인 상태는 건드리지 않는다 — 봉투를 바꿔도 이미 찍은 소인은 유지된다. */
    @Query(
        """
        UPDATE postcards
        SET envelopeStyle = :envelopeStyle
        WHERE id = :id
        """
    )
    suspend fun updatePostcardEnvelopeStyle(
        id: Long,
        envelopeStyle: String?
    )

    @Query(
        """
        UPDATE postcards
        SET envelopePostmarked = :postmarked
        WHERE id = :id
        """
    )
    suspend fun updatePostcardEnvelopePostmarked(
        id: Long,
        postmarked: Boolean
    )

    /** 봉투에서 꺼내기 — 봉투와 소인을 함께 지운다. 엽서 내용·앞면 도장·스티커·낙서는 이 테이블과 무관해 영향받지 않는다. */
    @Query(
        """
        UPDATE postcards
        SET envelopeStyle = NULL,
            envelopePostmarked = 0
        WHERE id = :id
        """
    )
    suspend fun clearPostcardEnvelope(
        id: Long
    )
}