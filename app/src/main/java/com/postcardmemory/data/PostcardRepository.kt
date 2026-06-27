package com.postcardmemory.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostcardRepository @Inject constructor(
    private val dao: PostcardDao
) {
    fun getAllPostcards(): Flow<List<Postcard>> = dao.getAllPostcards()

    suspend fun getPostcardById(id: Long): Postcard? = dao.getPostcardById(id)

    suspend fun insertPostcard(postcard: Postcard): Long = dao.insertPostcard(postcard)

    suspend fun deletePostcard(postcard: Postcard) = dao.deletePostcard(postcard)

    suspend fun deletePostcardById(id: Long) = dao.deletePostcardById(id)
}
