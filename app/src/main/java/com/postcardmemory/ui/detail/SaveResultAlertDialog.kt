package com.postcardmemory.ui.detail

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties
import com.postcardmemory.ui.theme.BrutalBlack

/**
 * 저장/내보내기 결과 안내 다이얼로그(현재 DetailScreen 호출부 4곳)의 공통 UI. 어떤 상태가 언제 표시될지,
 * 어떤 reset 함수를 호출할지는 호출부(DetailScreen)의 책임이며 이 Composable은
 * 그 판단을 하지 않는다.
 *
 * [dismissOnClickOutside]는 125일차에 더했다. 파일 내보내기가 상단바 버튼 한 번으로
 * 실행되면서, 연타하면 완료 창이 바깥 탭에 닫히고 다음 탭이 새 내보내기가 됐다. 그래서
 * 내보내기 완료 창만 false로 넘겨 확인 버튼(또는 뒤로가기)으로만 닫히게 한다.
 */
@Composable
internal fun SaveResultAlertDialog(
    title: String,
    titleColor: Color,
    body: String,
    onAcknowledge: () -> Unit,
    dismissOnClickOutside: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onAcknowledge,
        properties = DialogProperties(dismissOnClickOutside = dismissOnClickOutside),
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
