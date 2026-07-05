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

    suspend fun getPostcardById(
        id: Long
    ): Postcard? =
        dao.getPostcardById(id)

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

    suspend fun deletePostcardsByIds(
        ids: List<Long>
    ) {
        dao.deletePostcardsByIds(ids)
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

    suspend fun updatePostcardImagePath(
        id: Long,
        imagePath: String
    ) {
        dao.updatePostcardImagePath(
            id = id,
            imagePath = imagePath
        )
    }
}