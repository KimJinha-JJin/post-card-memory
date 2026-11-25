package com.postcardmemory.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryBookmarkRepository @Inject constructor(private val dao: MemoryBookmarkDao) {
    fun observeAll() = dao.observeAll()
    fun observe(postcardId: Long) = dao.observe(postcardId)
    suspend fun get(postcardId: Long) = dao.get(postcardId)
    suspend fun save(postcardId: Long, design: BookmarkDesign) =
        dao.saveCurrentFormat(MemoryBookmark.from(postcardId, design))
    suspend fun remove(postcardId: Long) = dao.remove(postcardId)
}
