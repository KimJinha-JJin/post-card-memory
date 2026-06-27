package com.postcardmemory.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "postcards")
data class Postcard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePath: String,
    val title: String,
    val capturedAt: Long = System.currentTimeMillis(),
    val location: String? = null
)
