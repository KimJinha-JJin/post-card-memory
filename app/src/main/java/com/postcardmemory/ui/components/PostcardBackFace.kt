package com.postcardmemory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.ui.theme.SunsetGold

/**
 * 커서가 문구 바로 뒤에 보일 여유 공간. 폭을 텍스트 실측치와 정확히
 * 같게 주면 커서가 마지막 글자에 가려 보이므로 약간의 여유를 둔다.
 */
private val RECIPIENT_FIELD_CURSOR_BUFFER = 6.dp

/** 뒷면 수식언 입력칸의 최대 길이. UI 레이어 독립 상수(DetailViewModel의 저장 쪽 제한과 별도, 기존 코드베이스의 message/120자와 동일한 컨벤션). */
const val BACK_RECIPIENT_MODIFIER_MAX_LENGTH = 20

/** 뒷면 편지 본문의 최대 길이. */
const val BACK_MESSAGE_MAX_LENGTH = 500

/**
 * "To. [수식언] 나"에서 수식언이 비어 있으면 "To.  나"처럼 이중 공백이
 * 생기지 않도록 마지막 "나" 앞에 붙는 공백을 조건부로 결정한다.
 */
internal fun backRecipientSuffix(
    recipientModifier: String
): String =
    if (recipientModifier.isBlank()) {
        "나"
    } else {
        " 나"
    }

/** "From. yyyy-MM-dd의 나" 한 줄. 날짜는 엽서 생성일(capturedAt) 기준, 사용자가 편집하지 않는다. */
internal fun formatBackFromLine(
    capturedAt: Long
): String =
    "From. " +
            PostcardDateFormat.formatIso(capturedAt) +
            "의 나"

/**
 * 갤러리 등에서 "뒷면에 편지가 있다"고 표시할지 판단하는 단일 기준.
 * 공백만 입력된 경우는 내용으로 보지 않는다.
 */
internal fun postcardHasBackContent(
    recipientModifier: String,
    message: String
): Boolean =
    recipientModifier.isNotBlank() || message.isNotBlank()

/**
 * 엽서 뒷면 — 수신자(To.)·편지 본문·발신 날짜(From.)로 구성된 편지 영역.
 * 앞면과 달리 스티커·도장·낙서·배경 패턴 등 꾸미기 기능은 제공하지 않는다.
 *
 * To.는 "To. "+수식언+" 나"를 한 줄로 유지해야 하므로 Row 3분할로 구성한다
 * (Row는 자식이 넘쳐도 다음 줄로 내려가지 않으므로 "나"가 별도 줄로
 * 떨어지는 문제가 구조적으로 발생하지 않는다). 수식언 입력칸은
 * singleLine BasicTextField라 폭이 모자라면 줄바꿈 대신 내부 스크롤된다.
 */
@Composable
fun PostcardBackFaceContent(
    recipientModifier: String,
    onRecipientModifierChanged: (String) -> Unit,
    message: String,
    onMessageChanged: (String) -> Unit,
    capturedAt: Long,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PaperSurface)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            val recipientTextStyle =
                TextStyle(
                    color = InkPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

            val textMeasurer = rememberTextMeasurer()
            val density = LocalDensity.current

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val recipientSuffix =
                    backRecipientSuffix(recipientModifier)

                // 실제 표시 폭(한글/영문/공백이 섞여도 정확)을 TextStyle
                // 그대로 재사용해 측정한다 — 글자 수 기반 어림값이 아니다.
                val prefixWidthPx = remember(recipientTextStyle) {
                    textMeasurer.measure(
                        text = "To. ",
                        style = recipientTextStyle
                    ).size.width
                }

                val suffixWidthPx =
                    remember(recipientSuffix, recipientTextStyle) {
                        textMeasurer.measure(
                            text = recipientSuffix,
                            style = recipientTextStyle
                        ).size.width
                    }

                val contentWidthPx =
                    remember(recipientModifier, recipientTextStyle) {
                        textMeasurer.measure(
                            // 빈 문자열은 폭이 0이라 커서가 보이지 않으므로
                            // 공백 한 칸 기준 폭을 최소값으로 쓴다.
                            text = recipientModifier.ifEmpty { " " },
                            style = recipientTextStyle
                        ).size.width
                    }

                // To. 와 나 사이에서 수식언이 실제로 쓸 수 있는 최대 폭.
                // 카드 가용 폭(BoxWithConstraints)을 기준으로 계산하므로
                // 20자를 꽉 채워도 화면 밖으로 넘치거나 줄바꿈되지 않는다.
                val maxFieldWidthPx =
                    (constraints.maxWidth - prefixWidthPx - suffixWidthPx)
                        .coerceAtLeast(0)

                val cursorBufferPx = with(density) {
                    RECIPIENT_FIELD_CURSOR_BUFFER.roundToPx()
                }

                val fieldWidthPx =
                    (contentWidthPx + cursorBufferPx)
                        .coerceAtMost(maxFieldWidthPx)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "To. ",
                        style = recipientTextStyle
                    )

                    BasicTextField(
                        value = recipientModifier,
                        onValueChange = { newValue ->
                            if (
                                newValue.length <=
                                BACK_RECIPIENT_MODIFIER_MAX_LENGTH &&
                                !newValue.contains('\n')
                            ) {
                                onRecipientModifierChanged(newValue)
                            }
                        },
                        enabled = enabled,
                        singleLine = true,
                        textStyle = recipientTextStyle,
                        cursorBrush = SolidColor(SunsetGold),
                        modifier = Modifier.width(
                            with(density) {
                                fieldWidthPx.toDp()
                            }
                        )
                    )

                    Text(
                        text = recipientSuffix,
                        style = recipientTextStyle
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PaperDivider)
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (message.isEmpty()) {
                    Text(
                        text = "오늘의 나에게 하고 싶은 말을 적어봐.",
                        color = InkSecondary,
                        fontSize = 15.sp
                    )
                }

                BasicTextField(
                    value = message,
                    onValueChange = { newValue ->
                        if (newValue.length <= BACK_MESSAGE_MAX_LENGTH) {
                            onMessageChanged(newValue)
                        }
                    },
                    enabled = enabled,
                    textStyle = TextStyle(
                        color = InkPrimary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    cursorBrush = SolidColor(SunsetGold),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                text = "${message.length} / $BACK_MESSAGE_MAX_LENGTH",
                color = InkSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = formatBackFromLine(capturedAt),
                color = InkPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
