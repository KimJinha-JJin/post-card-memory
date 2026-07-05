package com.postcardmemory.data

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "''")
    val message: String = "",
    val backgroundColorArgb: Long = 0xFFFFFBF7L,
    val backgroundImagePath: String? = null,
    @ColumnInfo(defaultValue = "'NONE'")
    val backgroundPattern: String = "NONE",
    @ColumnInfo(defaultValue = "'SERIF'")
    val messageFont: String = "SERIF",
    @ColumnInfo(defaultValue = "'STANDARD'")
    val layoutStyle: String = "STAMP",
    @ColumnInfo(defaultValue = "'DOT'")
    val dateFormat: String = "DOT",
    @ColumnInfo(defaultValue = "1.0")
    val messageTextScale: Float = 1f,
    @ColumnInfo(defaultValue = "1.0")
    val dateTextScale: Float = 1f,
    @ColumnInfo(defaultValue = "1.0")
    val backgroundPatternDensity: Float = 1f,
    @ColumnInfo(defaultValue = "1.0")
    val stampPhotoScale: Float = 1f,
    @ColumnInfo(defaultValue = "1.0")
    val polaroidPhotoScale: Float = 1f,
    @ColumnInfo(defaultValue = "0.0")
    val photoEdgeBlur: Float = 0f
)