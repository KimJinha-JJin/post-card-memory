package com.postcardmemory.ui.detail

import kotlin.math.roundToInt
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 사진 스티커 오림 스타일의 저장값 해석과 canonical geometry.
 *
 * PhotoStickerItem 자체는 android.net.Uri 때문에 순수 JUnit에서 만들 수 없어,
 * 직렬화 왕복·복제·누끼 차단은 androidTest의 PhotoStickerEdgeStyleInstrumentedTest가
 * 맡는다. 여기서는 deserializePhotoStickerItem이 그대로 호출하는 필드 해석 함수와
 * 미리보기·저장 렌더러가 공유하는 PhotoStickerPaperSpec만 검증한다.
 */
class PhotoStickerEdgeStyleTest {

    @Test
    fun `missing style field falls back to DEFAULT`() {
        assertEquals(
            PhotoStickerEdgeStyle.DEFAULT,
            PhotoStickerEdgeStyle.fromStored(null)
        )
    }

    @Test
    fun `unknown or malformed style falls back to DEFAULT without throwing`() {
        listOf("", "SCISSORZ", "polaroid", "~", "3").forEach { raw ->
            assertEquals(
                raw,
                PhotoStickerEdgeStyle.DEFAULT,
                PhotoStickerEdgeStyle.fromStored(raw)
            )
        }
    }

    @Test
    fun `every style round-trips through its stored name`() {
        PhotoStickerEdgeStyle.entries.forEach { style ->
            assertEquals(
                style,
                PhotoStickerEdgeStyle.fromStored(style.storedName)
            )
        }
    }

    @Test
    fun `seed parsing keeps stored value and treats missing or malformed as unassigned`() {
        assertEquals(-4242L, parsePhotoStickerEdgeSeed("-4242"))
        assertEquals(Long.MAX_VALUE, parsePhotoStickerEdgeSeed(Long.MAX_VALUE.toString()))
        assertEquals(0L, parsePhotoStickerEdgeSeed(null))
        assertEquals(0L, parsePhotoStickerEdgeSeed("abc"))
        assertEquals(0L, parsePhotoStickerEdgeSeed(""))
    }

    @Test
    fun `new seed is never the unassigned marker`() {
        val alwaysZeroThenValue =
            object : Random() {
                private var calls = 0
                override fun nextBits(bitCount: Int): Int = 0
                override fun nextLong(): Long =
                    if (calls++ < 3) 0L else 99L
            }

        assertEquals(99L, newPhotoStickerEdgeSeed(alwaysZeroThenValue))
    }

    @Test
    fun `DEFAULT has no paper spec so the legacy renderer path is kept`() {
        assertNull(
            photoStickerPaperSpec(PhotoStickerEdgeStyle.DEFAULT, edgeSeed = 123L)
        )
    }

    @Test
    fun `polaroid has thin even top and side margins and a wider bottom`() {
        val spec =
            photoStickerPaperSpec(PhotoStickerEdgeStyle.POLAROID, edgeSeed = 0L)!!
        val window = spec.photoWindow
        val top = window.top
        val left = window.left
        val right = 1f - window.right
        val bottom = 1f - window.bottom

        assertEquals(top, left, 0.0001f)
        assertEquals(left, right, 0.0001f)
        assertTrue("bottom margin should be wider", bottom > top)
        // 큰 흰 박스가 되지 않도록 사진이 종이 면적의 대부분을 차지해야 한다.
        val photoArea =
            (window.right - window.left) * (window.bottom - window.top)
        assertTrue("photo area $photoArea", photoArea > 0.7f)
    }

    @Test
    fun `polaroid geometry does not depend on seed`() {
        assertEquals(
            photoStickerPaperSpec(PhotoStickerEdgeStyle.POLAROID, edgeSeed = 1L),
            photoStickerPaperSpec(PhotoStickerEdgeStyle.POLAROID, edgeSeed = 987654321L)
        )
    }

    @Test
    fun `mirrored window keeps the wide margin at the visual bottom under vertical flip`() {
        val window =
            NormalizedRect(left = 0.05f, top = 0.05f, right = 0.95f, bottom = 0.85f)

        val flippedV = window.mirrored(flipHorizontal = false, flipVertical = true)
        // 뒤집힌 좌표계 안에서는 넓은 여백이 위로 가야 화면에서 아래로 보인다.
        assertEquals(0.15f, flippedV.top, 0.0001f)
        assertEquals(0.95f, flippedV.bottom, 0.0001f)
        assertEquals(window.left, flippedV.left, 0.0001f)

        val flippedH = window.mirrored(flipHorizontal = true, flipVertical = false)
        assertEquals(0.05f, flippedH.left, 0.0001f)
        assertEquals(0.95f, flippedH.right, 0.0001f)

        assertEquals(window, window.mirrored(flipHorizontal = false, flipVertical = false))
        assertNotEquals(window, flippedV)
    }

    // ---- SCISSOR ----

    private val sampleSeeds =
        listOf(1L, -1L, 42L, 7_777_777L, Long.MIN_VALUE, Long.MAX_VALUE) +
            (1..200).map { it * 1_234_567_891L }

    @Test
    fun `scissor outline is identical for the same seed`() {
        sampleSeeds.forEach { seed ->
            assertEquals(
                photoStickerPaperSpec(PhotoStickerEdgeStyle.SCISSOR, seed),
                photoStickerPaperSpec(PhotoStickerEdgeStyle.SCISSOR, seed)
            )
        }
    }

    @Test
    fun `scissor outline varies between seeds`() {
        val distinct =
            sampleSeeds.map { scissorPaperOutline(it) }.toSet()

        assertTrue("distinct outlines ${distinct.size}", distinct.size > sampleSeeds.size / 2)
    }

    @Test
    fun `scissor outline stays inside the sticker box and outside the photo window`() {
        sampleSeeds.forEach { seed ->
            val spec = photoStickerPaperSpec(PhotoStickerEdgeStyle.SCISSOR, seed)!!
            val window = spec.photoWindow
            val outline = spec.paperOutline!!

            outline.forEach { point ->
                assertTrue("seed $seed x ${point.x}", point.x in 0f..1f)
                assertTrue("seed $seed y ${point.y}", point.y in 0f..1f)
                // 모든 외곽점이 사진 창 바깥 — 사진 내용은 잘리지 않는다.
                val insideWindow =
                    point.x > window.left && point.x < window.right &&
                        point.y > window.top && point.y < window.bottom
                assertTrue("seed $seed point $point cuts into photo", !insideWindow)
            }
        }
    }

    @Test
    fun `scissor keeps a thin white margin around the photo`() {
        sampleSeeds.forEach { seed ->
            val spec = photoStickerPaperSpec(PhotoStickerEdgeStyle.SCISSOR, seed)!!
            val window = spec.photoWindow
            val outline = spec.paperOutline!!

            // 각 외곽점이 사진 창 바깥으로 얼마나 떨어져 있는지(가장 먼 방향 기준).
            val minMargin =
                outline.minOf { point ->
                    maxOf(
                        window.left - point.x,
                        point.x - window.right,
                        window.top - point.y,
                        point.y - window.bottom
                    )
                }
            assertTrue("seed $seed min margin $minMargin", minMargin > 0.02f)
            assertTrue("seed $seed photo too small", window.right - window.left > 0.85f)
        }
    }

    @Test
    fun `scissor sides are a few straight cuts not a zigzag`() {
        sampleSeeds.forEach { seed ->
            val outline = scissorPaperOutline(seed)
            // 네 모서리 + 변마다 1..MAX 개의 꺾임
            assertTrue(outline.size in 8..(4 + 4 * SCISSOR_MAX_BREAKS_PER_SIDE))

            // 각 꺾임점이 이웃 두 점을 잇는 직선에서 벗어나는 폭이 아주 작다.
            outline.indices.forEach { i ->
                val prev = outline[(i - 1 + outline.size) % outline.size]
                val point = outline[i]
                val next = outline[(i + 1) % outline.size]
                val dx = next.x - prev.x
                val dy = next.y - prev.y
                val length = kotlin.math.sqrt(dx * dx + dy * dy)
                val distance =
                    kotlin.math.abs(dy * point.x - dx * point.y + next.x * prev.y - next.y * prev.x) /
                        length
                // 모서리는 직각이라 거리가 크다 — 변 중간의 꺾임만 본다.
                if (length > 0.3f && distance < 0.1f) {
                    assertTrue(
                        "seed $seed break deviation $distance",
                        distance <= SCISSOR_SIDE_DEVIATION + SCISSOR_CORNER_JITTER * 2
                    )
                }
            }
        }
    }

    @Test
    fun `unassigned seed still yields a valid stable outline`() {
        assertEquals(scissorPaperOutline(0L), scissorPaperOutline(0L))
        assertTrue(scissorPaperOutline(0L).size >= 8)
    }

    // ---- TORN ----

    @Test
    fun `torn shape is identical for the same seed`() {
        sampleSeeds.forEach { seed ->
            assertEquals(
                photoStickerPaperSpec(PhotoStickerEdgeStyle.TORN, seed),
                photoStickerPaperSpec(PhotoStickerEdgeStyle.TORN, seed)
            )
        }
    }

    @Test
    fun `torn tears all four sides and each side wobbles differently`() {
        sampleSeeds.forEach { seed ->
            val profile = tornEdgeProfile(seed)
            val sides = TornSide.entries.map { profile.paperInsets.getValue(it).toList() }

            assertEquals(TornSide.entries.size, profile.paperInsets.size)
            sides.forEach { paper ->
                assertEquals(TORN_POINTS_PER_SIDE, paper.size)
                // 반듯한 변이 아니라 실제로 일렁인다.
                assertTrue("seed $seed flat side", paper.max() - paper.min() > 0.004f)
            }
            assertEquals("seed $seed identical sides", sides.size, sides.toSet().size)
        }
    }

    @Test
    fun `torn outline varies between seeds`() {
        val distinct =
            sampleSeeds.map { photoStickerPaperSpec(PhotoStickerEdgeStyle.TORN, it) }.toSet()

        assertEquals(sampleSeeds.size, distinct.size)
    }

    @Test
    fun `torn sides always leave a white paper band between photo and paper edge`() {
        sampleSeeds.forEach { seed ->
            val profile = tornEdgeProfile(seed)
            TornSide.entries.forEach { side ->
                val paper = profile.paperInsets.getValue(side)
                val photo = profile.photoInsets.getValue(side)
                paper.indices.forEach { i ->
                    assertTrue(
                        "seed $seed $side[$i] band ${photo[i] - paper[i]}",
                        photo[i] - paper[i] >= TORN_MIN_WHITE_BAND - 0.0001f
                    )
                }
            }
        }
    }

    @Test
    fun `torn edge is a gentle wobble not a burst of spikes`() {
        sampleSeeds.forEach { seed ->
            val profile = tornEdgeProfile(seed)
            TornSide.entries.forEach { side ->
                val paper = profile.paperInsets.getValue(side)
                // 전체 높낮이는 작고(한 변의 4% 이하), 이웃 점 사이 급변도 없다.
                assertTrue("seed $seed $side range", paper.max() - paper.min() <= 0.035f)
                for (i in 1 until paper.size) {
                    assertTrue(
                        "seed $seed $side step ${paper[i] - paper[i - 1]}",
                        kotlin.math.abs(paper[i] - paper[i - 1]) <= 0.012f
                    )
                }
            }
        }
    }

    @Test
    fun `torn paper and photo stay inside the sticker box with photo inside its window`() {
        sampleSeeds.forEach { seed ->
            val spec = photoStickerPaperSpec(PhotoStickerEdgeStyle.TORN, seed)!!
            val window = spec.photoWindow
            (spec.paperOutline!! + spec.photoClipOutline!!).forEach { point ->
                assertTrue("seed $seed $point", point.x in 0f..1f && point.y in 0f..1f)
            }
            spec.photoClipOutline.forEach { point ->
                assertTrue(point.x in window.left..window.right)
                assertTrue(point.y in window.top..window.bottom)
            }
            // 사진이 주인공 — 사진 면적이 종이 대부분을 차지한다.
            assertTrue((window.right - window.left) * (window.bottom - window.top) > 0.75f)
        }
    }

    @Test
    fun `mirrored polygon matches mirrored rect semantics`() {
        val points = listOf(NormalizedPoint(0.1f, 0.2f), NormalizedPoint(0.9f, 0.7f))

        assertEquals(points, points.mirrored(flipHorizontal = false, flipVertical = false))
        assertEquals(
            listOf(NormalizedPoint(0.9f, 0.8f), NormalizedPoint(0.1f, 0.3f)),
            points.mirrored(flipHorizontal = true, flipVertical = true)
                .map { NormalizedPoint((it.x * 1000).roundToInt() / 1000f, (it.y * 1000).roundToInt() / 1000f) }
        )
    }

    // ---- 저장된 스티커 모양 고정(golden) ----
    // 이미 저장된 SCISSOR/TORN 스티커는 seed만 저장하고 모양은 매번 다시 계산한다.
    // 생성 알고리즘이 바뀌면 기존 스티커 모양이 바뀌므로, 실제 출력(float raw bits)을
    // 고정해 둔다. 이 테스트가 깨지면 "기존 엽서 모양 변경"이다.

    private fun rawBits(points: List<NormalizedPoint>): String =
        points.joinToString("") { "${it.x.toRawBits()},${it.y.toRawBits()};" }

    @Test
    fun `scissor outline matches the recorded shapes`() {
        assertEquals(
            "1017816678,1008120853;1057349148,1002346755;1065010492,1012078242;1064975959,1057044229;1064929770,1065208338;1060176278,1065081957;1051341256,1065070449;1009377020,1065185656;1004477562,1058697152;1017176870,1051028254;",
            rawBits(scissorPaperOutline(42L))
        )
        assertEquals(
            "1013756362,1014965455;1056425316,1013996311;1065221348,1008109982;1065065648,1057991397;1065106897,1065188092;1060496244,1065018092;1053607238,1065105510;1015766488,1064919245;1013578152,1058144738;",
            rawBits(scissorPaperOutline(-7L))
        )
    }

    @Test
    fun `torn outline matches the recorded shapes`() {
        assertEquals(
            "1016879577,1020372999;1029712066,1018457218;1035539019,1017045451;1040255003,1015403344;1042646800,1010363087;1045038597,1009910953;",
            rawBits(photoStickerPaperSpec(PhotoStickerEdgeStyle.TORN, 42L)!!.paperOutline!!.take(6))
        )
        assertEquals(
            "1015108876,1014197838;1028915092,1017385392;1035184721,1017987646;1040012504,1017310156;1042513840,1016205123;1044927730,1018962713;",
            rawBits(photoStickerPaperSpec(PhotoStickerEdgeStyle.TORN, -7L)!!.paperOutline!!.take(6))
        )
    }

    // ---- MAGAZINE ----

    @Test
    fun `magazine shape and ink are identical for the same seed`() {
        sampleSeeds.forEach { seed ->
            assertEquals(
                photoStickerPaperSpec(PhotoStickerEdgeStyle.MAGAZINE, seed),
                photoStickerPaperSpec(PhotoStickerEdgeStyle.MAGAZINE, seed)
            )
        }
    }

    @Test
    fun `magazine halftone stays faint and never filters the photo`() {
        val used = mutableSetOf<Long>()
        sampleSeeds.forEach { seed ->
            val halftone =
                photoStickerPaperSpec(PhotoStickerEdgeStyle.MAGAZINE, seed)!!.halftone!!
            used += halftone.marginInkArgb

            assertTrue(halftone.marginInkArgb in MAGAZINE_MARGIN_INKS)
            // 여백은 인쇄색으로 읽히되 바탕은 옅게, 사진 위 망점은 거의 보이지 않게.
            assertTrue(halftone.marginTintAlpha <= 0.25f)
            assertTrue(halftone.marginAlpha <= 0.6f)
            assertTrue(halftone.photoAlpha <= 0.08f)
            assertTrue(halftone.pitch in 0.01f..0.04f)
        }
        assertEquals(MAGAZINE_MARGIN_INKS.toSet(), used)
    }

    @Test
    fun `magazine print edge is only slightly rough and keeps the photo large`() {
        sampleSeeds.forEach { seed ->
            val spec = photoStickerPaperSpec(PhotoStickerEdgeStyle.MAGAZINE, seed)!!
            val window = spec.photoWindow

            spec.photoClipOutline!!.forEach { point ->
                assertTrue(point.x in window.left..window.right)
                assertTrue(point.y in window.top..window.bottom)
                // 창 경계에서 안쪽으로 최대 거칠기만큼만 들어간다.
                val depth =
                    minOf(
                        point.x - window.left,
                        window.right - point.x,
                        point.y - window.top,
                        window.bottom - point.y
                    )
                assertTrue("seed $seed depth $depth", depth <= MAGAZINE_PRINT_EDGE_ROUGHNESS + 0.0001f)
            }
            assertTrue((window.right - window.left) * (window.bottom - window.top) > 0.75f)
        }
    }

    @Test
    fun `magazine paper edge stays inside the box and outside the photo`() {
        sampleSeeds.forEach { seed ->
            val spec = photoStickerPaperSpec(PhotoStickerEdgeStyle.MAGAZINE, seed)!!
            val window = spec.photoWindow
            spec.paperOutline!!.forEach { point ->
                assertTrue(point.x in 0f..1f && point.y in 0f..1f)
                val margin =
                    maxOf(
                        window.left - point.x,
                        point.x - window.right,
                        window.top - point.y,
                        point.y - window.bottom
                    )
                assertTrue("seed $seed margin $margin", margin > 0.02f)
            }
        }
    }

    @Test
    fun `magazine differs from scissor by printed margin thin cut core and straighter edge`() {
        val scissor = photoStickerPaperSpec(PhotoStickerEdgeStyle.SCISSOR, 42L)!!
        val magazine = photoStickerPaperSpec(PhotoStickerEdgeStyle.MAGAZINE, 42L)!!

        assertNull(scissor.halftone)
        assertEquals(0f, scissor.cutCoreWidth)
        assertTrue(magazine.halftone!!.marginTintAlpha > 0f)
        assertTrue(magazine.cutCoreWidth > 0f)
        // 흰 단면은 여백보다 훨씬 얇다.
        assertTrue(magazine.cutCoreWidth < magazine.photoWindow.left / 3f)
        assertTrue(MAGAZINE_SIDE_DEVIATION < SCISSOR_SIDE_DEVIATION)
        assertTrue(MAGAZINE_CORNER_JITTER < SCISSOR_CORNER_JITTER)
    }
}
