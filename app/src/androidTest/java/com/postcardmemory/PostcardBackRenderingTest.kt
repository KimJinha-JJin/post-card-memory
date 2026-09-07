package com.postcardmemory

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.components.PostcardBackExportCapture
import com.postcardmemory.ui.components.PostcardBackFaceContent
import com.postcardmemory.ui.components.fittingBackTextSize
import com.postcardmemory.utils.PostcardImageExporter
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PostcardBackRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sharedBackCompositorProducesFullResolutionSharePngWithoutPhoto() {
        val result = AtomicReference<Result<Bitmap>?>(null)
        compose.setContent {
            PostcardBackExportCapture(
                Postcard(imagePath = "missing-photo", title = "test", capturedAt = 0,
                    backRecipientModifier = "미래의", backMessage = "오늘도 고생했어.\n내일도 잘 부탁해.",
                    backPostscript = "따뜻한 차 한 잔 잊지 마.", backWrittenAt = 0, backWrittenOffsetMinutes = 540)
            ) { result.set(it) }
        }
        compose.waitUntil(15_000) { result.get() != null }
        val bitmap = result.get()!!.getOrThrow()
        try {
            assertEquals(2048, bitmap.width)
            assertEquals(2048, bitmap.height)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            assertTrue("text and divider must actually be rendered", pixels.count { it != pixels[0] } > 1000)
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            java.io.File(context.cacheDir, "day66-back-qa.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            val file = PostcardImageExporter.exportBackForSharing(context, -66, bitmap).getOrThrow()
            try {
                val decoded = BitmapFactory.decodeFile(file.path)
                try { assertTrue("PNG must preserve exactly the captured back", bitmap.sameAs(decoded)) }
                finally { decoded.recycle() }
            } finally { file.delete() }
            val uri = PostcardImageExporter.exportBackToGallery(context, bitmap).getOrThrow()
            try {
                val decoded = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
                try { assertTrue("gallery export must preserve the same back", bitmap.sameAs(decoded)) }
                finally { decoded.recycle() }
            } finally {
                context.contentResolver.delete(uri, null, null) // only the image this test just created
            }
        } finally { bitmap.recycle() }
    }

    @Test fun emptyReadOnlyBackHasNoPostscriptPlaceholderOrCounter() {
        compose.setContent {
            PostcardBackFaceContent("", {}, "", {}, 0, enabled = false, readOnly = true,
                modifier = Modifier.requiredSize(320.dp))
        }
        compose.onNodeWithText("P.S. ").assertDoesNotExist()
        compose.onNodeWithText("한마디 더 남기기").assertDoesNotExist()
        compose.onNodeWithText("오늘의 나에게 하고 싶은 말을 적어봐.").assertDoesNotExist()
        compose.onNodeWithText("0 / 500").assertDoesNotExist()
    }

    @Test fun longBodyAndPostscriptFitMeasuredBoundsAtSmallAndExportSizes() {
        val checked = AtomicReference(false)
        compose.setContent {
            for (scale in listOf(320f / 360f, 2048f / 360f)) {
                CompositionLocalProvider(LocalDensity provides Density(scale, 1f)) {
                    val measurer = rememberTextMeasurer()
                    remember(measurer) {
                        for ((text, height, maximum, ratio) in listOf(
                            FitCase("오늘의 나에게 남기는 따뜻한 편지. ".repeat(30).take(500), 140, 15f, 22f / 15f),
                            FitCase("한마디 더 남겨 봐. ".repeat(8).take(60), 44, 13f, 17f / 13f)
                        )) {
                            val widthPx = (312 * scale).toInt()
                            val heightPx = (height * scale).toInt()
                            val size = fittingBackTextSize(measurer, text, widthPx, heightPx, maximum, ratio)
                            val layout = measurer.measure(AnnotatedString(text),
                                style = TextStyle(fontSize = size.sp, lineHeight = (size * ratio).sp),
                                constraints = Constraints(maxWidth = widthPx))
                            assertTrue(layout.size.height <= heightPx)
                            assertFalse(layout.didOverflowWidth)
                        }
                        true
                    }
                }
            }
            checked.set(true)
        }
        compose.waitUntil { checked.get() }
    }

    private data class FitCase(val text: String, val height: Int, val maximum: Float, val ratio: Float)
}
