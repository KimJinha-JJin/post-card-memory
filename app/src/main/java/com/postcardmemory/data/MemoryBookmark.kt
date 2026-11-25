package com.postcardmemory.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class BookmarkShape { CLASSIC, HAND_CUT, MINI_TAG }
/**
 * 종이 색. DB에는 순서(ordinal)가 아니라 이름이 저장되므로 이미 있는 이름은 바꾸거나 지우지 않고,
 * 새 색은 뒤에 더하기만 해(컬럼 변화가 없어 schema는 그대로야).
 */
enum class BookmarkPaper(val argb: Long) {
    IVORY(0xFFE8DCC6), BROWN(0xFFBBA184), MINT(0xFFBECFC1),
    LAVENDER(0xFFC8BED5), BLUE(0xFFB4C5CF), APRICOT(0xFFDFC0A3),
    ROSE(0xFFCFAFB2), OLIVE(0xFFC4C5A2), BUTTER(0xFFE5D6A5)
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
