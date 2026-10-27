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
    // SQL 기본값 'STANDARD'와 Kotlin 기본값 "STAMP"는 일부러 다르다.
    // 'STANDARD'는 이미 내보낸 Room schema와 기존 DB의 컬럼 정의에 맞춘
    // 값이라, 단순 정리 목적으로 'STAMP'로 맞추면 schema 검증과 Migration에
    // 영향을 준다. 옛 값을 가진 행은 MIGRATION_14_15가 STAMP로 정규화한다.
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
    val photoEdgeBlur: Float = 0f,
    @ColumnInfo(defaultValue = "0.0")
    val stampPhotoOffsetX: Float = 0f,
    @ColumnInfo(defaultValue = "0.0")
    val stampPhotoOffsetY: Float = 0f,
    @ColumnInfo(defaultValue = "0.0")
    val polaroidPhotoOffsetX: Float = 0f,
    @ColumnInfo(defaultValue = "0.0")
    val polaroidPhotoOffsetY: Float = 0f,
    @ColumnInfo(defaultValue = "0.0")
    val tapedFilmPhotoOffsetX: Float = 0f,
    @ColumnInfo(defaultValue = "0.0")
    val tapedFilmPhotoOffsetY: Float = 0f,
    @ColumnInfo(defaultValue = "1.0")
    val stampPhotoZoom: Float = 1f,
    @ColumnInfo(defaultValue = "1.0")
    val polaroidPhotoZoom: Float = 1f,
    @ColumnInfo(defaultValue = "1.0")
    val tapedFilmPhotoZoom: Float = 1f,
    @ColumnInfo(defaultValue = "'NONE'")
    val futureMailState: String = FUTURE_MAIL_STATE_NONE,
    val futureMailDeliverAt: Long? = null,
    val envelopeStyle: String? = null,
    @ColumnInfo(defaultValue = "0")
    val envelopePostmarked: Boolean = false,
    @ColumnInfo(defaultValue = "''")
    val backRecipientModifier: String = "",
    @ColumnInfo(defaultValue = "''")
    val backMessage: String = "",
    val backPostscript: String? = null,
    val backWrittenAt: Long? = null,
    val backWrittenOffsetMinutes: Int? = null,
    // SQL default protects migrated postcards; only newly created objects opt in.
    @ColumnInfo(defaultValue = "0")
    val backWritingRecordEnabled: Boolean = true
)

/** 일반 엽서. 갤러리에 보인다. */
const val FUTURE_MAIL_STATE_NONE = "NONE"

/** 미래로 발송되어 갤러리에서 사라진 엽서. futureMailDeliverAt이 지나야 열어볼 수 있다. */
const val FUTURE_MAIL_STATE_SENT = "SENT"
