package com.postcardmemory.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.postcardmemory.data.Postcard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

/** Same back-face compositor, at export resolution, with no input chrome.
 * The immutable postcard argument is the snapshot from the action's click.
 * Record only: never paint this off-screen-sized layer onto the editor.
 */
@Composable
internal fun PostcardBackExportCapture(postcard: Postcard, onResult: (Result<Bitmap>) -> Unit) {
    val layer = rememberGraphicsLayer()
    val drawn = remember(postcard) { CompletableDeferred<Unit>() }
    LaunchedEffect(postcard) {
        try {
            withTimeout(5_000) { drawn.await() }
            val bitmap = run {
                val captured = layer.toImageBitmap().asAndroidBitmap()
                try {
                    checkNotNull(captured.copy(Bitmap.Config.ARGB_8888, false))
                } finally {
                    captured.recycle()
                }
            }
            onResult(Result.success(bitmap)) // receiver owns and recycles the bitmap
        } catch (exception: TimeoutCancellationException) {
            onResult(Result.failure(exception))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            onResult(Result.failure(exception))
        }
    }
    Box(Modifier.size(1.dp).clearAndSetSemantics {}) {
        CompositionLocalProvider(LocalDensity provides Density(2048f / BACK_CARD_SIZE_DP, 1f)) {
            PostcardBackFaceContent(
                recipientModifier = postcard.backRecipientModifier,
                onRecipientModifierChanged = {},
                message = postcard.backMessage,
                onMessageChanged = {},
                capturedAt = postcard.capturedAt,
                postscript = postcard.backPostscript,
                writtenAt = postcard.backWrittenAt,
                writtenOffsetMinutes = postcard.backWrittenOffsetMinutes,
                enabled = false,
                readOnly = true,
                modifier = Modifier.requiredSize(BACK_CARD_SIZE_DP.dp).drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawn.complete(Unit)
                }
            )
        }
    }
}
