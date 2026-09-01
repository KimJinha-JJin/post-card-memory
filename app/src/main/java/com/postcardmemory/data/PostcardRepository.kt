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

    suspend fun openFutureMail(
        id: Long
    ) {
        dao.openFutureMail(id)
    }

    suspend fun insertPostcard(
        postcard: Postcard
    ): Long =
        dao.insertPostcard(postcard)

    suspend fun deletePostcard(
        postcard: Postcard
    ) {
        dao.deletePostcard(postcard)
    }

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

    suspend fun updatePostcardMessageFont(
        id: Long,
        messageFont: String
    ) {
        dao.updatePostcardMessageFont(
            id = id,
            messageFont = messageFont
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

    suspend fun updatePostcardDateFormat(
        id: Long,
        dateFormat: String
    ) {
        dao.updatePostcardDateFormat(
            id = id,
            dateFormat = dateFormat
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

    suspend fun updatePostcardDateTextScale(
        id: Long,
        dateTextScale: Float
    ) {
        dao.updatePostcardDateTextScale(
            id = id,
            dateTextScale = dateTextScale
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

    suspend fun updatePostcardEnvelopeStyle(
        id: Long,
        envelopeStyle: String?
    ) {
        dao.updatePostcardEnvelopeStyle(
            id = id,
            envelopeStyle = envelopeStyle
        )
    }

    suspend fun updatePostcardEnvelopePostmarked(
        id: Long,
        postmarked: Boolean
    ) {
        dao.updatePostcardEnvelopePostmarked(
            id = id,
            postmarked = postmarked
        )
    }

    suspend fun clearPostcardEnvelope(
        id: Long
    ) {
        dao.clearPostcardEnvelope(id)
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
        backMessage: String
    ) {
        dao.updatePostcardBackMessage(
            id = id,
            backMessage = backMessage
        )
    }
}