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
    val location: String? = null,
    val message: String = "",
    val backgroundColorArgb: Long = 0xFFFFFBF7L,
    val backgroundImagePath: String? = null,
    val backgroundPattern: String = "NONE",
    val messageFont: String = "SERIF",
    val layoutStyle: String = "STANDARD",
    val dateFormat: String = "DOT",
    val messageTextScale: Float = 1f,
    val dateTextScale: Float = 1f
)