package com.postcardmemory.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class BookmarkShape { CLASSIC, HAND_CUT, MINI_TAG }
enum class BookmarkPaper(val argb: Long) {
    IVORY(0xFFE8DCC6), BROWN(0xFFBBA184), MINT(0xFFBECFC1)
}
enum class BookmarkTrace { NONE, POSTMARK, LINES }

data class BookmarkDesign(
    val shape: BookmarkShape = BookmarkShape.HAND_CUT,
    val paper: BookmarkPaper = BookmarkPaper.IVORY,
    val trace: BookmarkTrace = BookmarkTrace.POSTMARK
)

/** 종이 설정만 저장해. 엽서의 사진·꾸미기 파일을 소유하거나 복제하지 않아. */
@Entity(
    tableName = "memory_bookmarks",
    foreignKeys = [ForeignKey(
        entity = Postcard::class,
        parentColumns = ["id"], childColumns = ["postcardId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MemoryBookmark(
    @PrimaryKey val postcardId: Long,
    @ColumnInfo(defaultValue = "'HAND_CUT'") val shapeId: String = "HAND_CUT",
    @ColumnInfo(defaultValue = "'IVORY'") val paperId: String = "IVORY",
    @ColumnInfo(defaultValue = "'POSTMARK'") val traceId: String = "POSTMARK",
    @ColumnInfo(defaultValue = "1") val formatVersion: Int = 1
) {
    /** 알 수 없는 새 형식은 보존하되 현재 앱이 임의로 편집하지 않아. */
    fun designOrNull(): BookmarkDesign? = if (formatVersion != 1) null else BookmarkDesign(
        shape = BookmarkShape.entries.firstOrNull { it.name == shapeId } ?: BookmarkShape.HAND_CUT,
        paper = BookmarkPaper.entries.firstOrNull { it.name == paperId } ?: BookmarkPaper.IVORY,
        trace = BookmarkTrace.entries.firstOrNull { it.name == traceId } ?: BookmarkTrace.NONE
    )

    companion object {
        fun from(postcardId: Long, design: BookmarkDesign): MemoryBookmark {
            require(postcardId > 0)
            return MemoryBookmark(postcardId, design.shape.name, design.paper.name, design.trace.name)
        }
    }
}
