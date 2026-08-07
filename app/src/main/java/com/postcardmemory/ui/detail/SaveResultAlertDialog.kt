package com.postcardmemory.ui.detail

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.postcardmemory.ui.theme.BrutalBlack

/**
 * 저장/내보내기 결과 안내 다이얼로그 7종의 공통 UI. 어떤 상태가 언제 표시될지,
 * 어떤 reset 함수를 호출할지는 호출부(DetailScreen)의 책임이며 이 Composable은
 * 그 판단을 하지 않는다.
 */
@Composable
internal fun SaveResultAlertDialog(
    title: String,
    titleColor: Color,
    body: String,
    onAcknowledge: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onAcknowledge,
        title = {
            Text(
                text = title,
                color = titleColor,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(text = body)
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) {
                Text(
                    text = "확인",
                    color = BrutalBlack,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
