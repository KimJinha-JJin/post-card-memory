package com.postcardmemory.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class PostcardRepository @Inject constructor(
    private val dao: PostcardDao
) {

    fun getAllPostcards(): Flow<List<Postcard>> =
        dao.getAllPostcards()

    fun getFutureMailPostcards(): Flow<List<Postcard>> =
        dao.getFutureMailPostcards()

    suspend fun getPostcardById(
        id: Long
    ): Postcard? =
        dao.getPostcardById(id)

    suspend fun sendToFutureMailbox(
        id: Long,
        deliverAt: Long
    ) {
        dao.sendToFutureMailbox(
            id = id,
            deliverAt = deliverAt
        )
    }

    /** 같은 도착일 묶음을 한 번에(원자적으로) 개봉한다. */
    suspend fun openFutureMailGroup(
        ids: List<Long>
    ) {
        dao.openFutureMailGroup(ids)
    }

    suspend fun insertPostcard(
        postcard: Postcard
    ): Long =
        dao.insertPostcard(postcard)

    // 이 삭제는 Room 행만 지우고 엽서 소유 파일은 건드리지 않는다.
    // 사용자 삭제 흐름은 직접 호출하지 않고, DB 삭제가 성공한 뒤에만 파일을
    // 정리하는 PostcardDeletionManager를 거친다.
    suspend fun deletePostcardById(
        id: Long
    ) {
        dao.deletePostcardById(id)
    }

    suspend fun updatePostcardMessage(
        id: Long,
        message: String
    ) {
        dao.updatePostcardMessage(
            id = id,
            message = message
        )
    }

    suspend fun updatePostcardBackground(
        id: Long,
        backgroundColorArgb: Long,
        backgroundImagePath: String?
    ) {
        dao.updatePostcardBackground(
            id = id,
            backgroundColorArgb = backgroundColorArgb,
            backgroundImagePath = backgroundImagePath
        )
    }

    suspend fun updatePostcardBackgroundPattern(
        id: Long,
        backgroundPattern: String
    ) {
        dao.updatePostcardBackgroundPattern(
            id = id,
            backgroundPattern = backgroundPattern
        )
    }

    suspend fun updatePostcardLayoutStyle(
        id: Long,
        layoutStyle: String
    ) {
        dao.updatePostcardLayoutStyle(
            id = id,
            layoutStyle = layoutStyle
        )
    }

    suspend fun updatePostcardMessageTextScale(
        id: Long,
        messageTextScale: Float
    ) {
        dao.updatePostcardMessageTextScale(
            id = id,
            messageTextScale = messageTextScale
        )
    }

    suspend fun updatePostcardBackgroundPatternDensity(
        id: Long,
        backgroundPatternDensity: Float
    ) {
        dao.updatePostcardBackgroundPatternDensity(
            id = id,
            backgroundPatternDensity = backgroundPatternDensity
        )
    }

    suspend fun updatePostcardStampPhotoScale(
        id: Long,
        stampPhotoScale: Float
    ) {
        dao.updatePostcardStampPhotoScale(
            id = id,
            stampPhotoScale = stampPhotoScale
        )
    }

    suspend fun updatePostcardPolaroidPhotoScale(
        id: Long,
        polaroidPhotoScale: Float
    ) {
        dao.updatePostcardPolaroidPhotoScale(
            id = id,
            polaroidPhotoScale = polaroidPhotoScale
        )
    }

    suspend fun updatePostcardPhotoEdgeBlur(
        id: Long,
        photoEdgeBlur: Float
    ) {
        dao.updatePostcardPhotoEdgeBlur(
            id = id,
            photoEdgeBlur = photoEdgeBlur
        )
    }

    suspend fun updatePostcardStampPhotoOffset(
        id: Long,
        stampPhotoOffsetX: Float,
        stampPhotoOffsetY: Float
    ) {
        dao.updatePostcardStampPhotoOffset(
            id = id,
            stampPhotoOffsetX = stampPhotoOffsetX,
            stampPhotoOffsetY = stampPhotoOffsetY
        )
    }

    suspend fun updatePostcardPolaroidPhotoOffset(
        id: Long,
        polaroidPhotoOffsetX: Float,
        polaroidPhotoOffsetY: Float
    ) {
        dao.updatePostcardPolaroidPhotoOffset(
            id = id,
            polaroidPhotoOffsetX = polaroidPhotoOffsetX,
            polaroidPhotoOffsetY = polaroidPhotoOffsetY
        )
    }

    suspend fun updatePostcardTapedFilmPhotoOffset(
        id: Long,
        tapedFilmPhotoOffsetX: Float,
        tapedFilmPhotoOffsetY: Float
    ) {
        dao.updatePostcardTapedFilmPhotoOffset(
            id = id,
            tapedFilmPhotoOffsetX = tapedFilmPhotoOffsetX,
            tapedFilmPhotoOffsetY = tapedFilmPhotoOffsetY
        )
    }

    suspend fun updatePostcardStampPhotoZoom(
        id: Long,
        stampPhotoZoom: Float
    ) {
        dao.updatePostcardStampPhotoZoom(
            id = id,
            stampPhotoZoom = stampPhotoZoom
        )
    }

    suspend fun updatePostcardPolaroidPhotoZoom(
        id: Long,
        polaroidPhotoZoom: Float
    ) {
        dao.updatePostcardPolaroidPhotoZoom(
            id = id,
            polaroidPhotoZoom = polaroidPhotoZoom
        )
    }

    suspend fun updatePostcardTapedFilmPhotoZoom(
        id: Long,
        tapedFilmPhotoZoom: Float
    ) {
        dao.updatePostcardTapedFilmPhotoZoom(
            id = id,
            tapedFilmPhotoZoom = tapedFilmPhotoZoom
        )
    }

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
    ) {
        dao.updatePostcardTemplateStyle(
            id = id,
            layoutStyle = layoutStyle,
            backgroundColorArgb = backgroundColorArgb,
            backgroundPattern = backgroundPattern,
            backgroundPatternDensity = backgroundPatternDensity,
            messageFont = messageFont,
            dateFormat = dateFormat,
            messageTextScale = messageTextScale,
            dateTextScale = dateTextScale,
            photoEdgeBlur = photoEdgeBlur,
            stampPhotoScale = stampPhotoScale,
            stampPhotoOffsetX = stampPhotoOffsetX,
            stampPhotoOffsetY = stampPhotoOffsetY,
            stampPhotoZoom = stampPhotoZoom,
            polaroidPhotoScale = polaroidPhotoScale,
            polaroidPhotoOffsetX = polaroidPhotoOffsetX,
            polaroidPhotoOffsetY = polaroidPhotoOffsetY,
            polaroidPhotoZoom = polaroidPhotoZoom,
            tapedFilmPhotoOffsetX = tapedFilmPhotoOffsetX,
            tapedFilmPhotoOffsetY = tapedFilmPhotoOffsetY,
            tapedFilmPhotoZoom = tapedFilmPhotoZoom
        )
    }

    suspend fun updatePostcardBackRecipientModifier(
        id: Long,
        backRecipientModifier: String
    ) {
        dao.updatePostcardBackRecipientModifier(
            id = id,
            backRecipientModifier = backRecipientModifier
        )
    }

    suspend fun updatePostcardBackMessage(
        id: Long,
        backMessage: String,
        writtenAt: Long? = null,
        offsetMinutes: Int? = null
    ) {
        dao.updatePostcardBackMessage(
            id = id,
            backMessage = backMessage,
            writtenAt = writtenAt,
            offsetMinutes = offsetMinutes
        )
    }

    suspend fun updatePostcardBackPostscript(id: Long, postscript: String?) {
        dao.updatePostcardBackPostscript(id, postscript)
    }
}
