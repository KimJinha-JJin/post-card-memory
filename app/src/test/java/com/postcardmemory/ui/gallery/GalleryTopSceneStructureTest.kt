package com.postcardmemory.ui.gallery

import com.postcardmemory.testsupport.extractBalancedCall
import com.postcardmemory.testsupport.readStructureTestSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 108일차: 놀이(연못·쉼터·쫑쫑컵)를 켜면 상단 시계·양손 장면이 위로 물러나며 높이까지
 * 접히고, 끄면 같은 길로 돌아온다. 세 놀이가 같은 조건 하나(`playMode == NONE`)를 쓰고,
 * 장면 전체(GalleryRetroClock)를 한 덩어리로 감싸며, 450ms FastOutSlowIn인지 소스로 고정한다.
 * 실제 움직임·끊김은 JVM으로 볼 수 없어 실기기 QA로 확인한다.
 */
class GalleryTopSceneStructureTest {

    private val sourceText: String by lazy {
        readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt",
                "app/src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt"
            )
        )
    }

    @Test
    fun topScene_retreatsForEveryPlayModeThroughOneSharedCondition() {
        val clockCalls = Regex("""GalleryRetroClock\(""").findAll(sourceText).count()
        assertEquals("상단 장면은 한 곳에서만 그려야 함", 1, clockCalls)

        val start = sourceText.indexOf("AnimatedVisibility(\n                        visible = playMode == GalleryPlayMode.NONE")
        assertTrue("시계 장면을 playMode == NONE 조건의 AnimatedVisibility로 감싸야 함", start >= 0)
        val call = extractBalancedCall(sourceText, start)

        // extractBalancedCall은 괄호 안 인자까지만 자르므로, 내용은 바로 뒤 trailing lambda에서 찾는다.
        val lambdaStart = start + call.length
        val lambdaHead = sourceText.substring(lambdaStart, sourceText.indexOf("HorizontalDivider(", lambdaStart))
        assertTrue(
            "시계·양손 장면 전체가 같은 AnimatedVisibility의 trailing lambda 안에 있어야 함",
            lambdaHead.trimStart().startsWith("{") && lambdaHead.contains("GalleryRetroClock(")
        )
        assertTrue("위로 물러나야 함", call.contains("slideOutVertically(") && call.contains("targetOffsetY = { fullHeight -> -fullHeight }"))
        assertTrue("높이까지 접혀야 함", call.contains("shrinkVertically("))
        assertTrue("같은 길로 돌아와야 함", call.contains("slideInVertically(") && call.contains("initialOffsetY = { fullHeight -> -fullHeight }"))
        assertTrue("높이도 다시 펼쳐져야 함", call.contains("expandVertically("))
        assertEquals("네 전환 모두 공용 tween을 써야 함", 4, Regex("""galleryTopSceneTween\(\)""").findAll(call).count())
    }

    @Test
    fun topSceneTween_is450msFastOutSlowIn() {
        assertTrue(sourceText.contains("private const val GALLERY_TOP_SCENE_ANIM_MS = 450"))
        assertTrue(
            sourceText.contains(
                "tween<T>(durationMillis = GALLERY_TOP_SCENE_ANIM_MS, easing = FastOutSlowInEasing)"
            )
        )
    }
}
