package com.postcardmemory.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.ui.theme.PaperTray
import com.postcardmemory.ui.theme.SunsetGold
import java.io.File

/**
 * 완성된 엽서 이미지를 공유하기 전 보여주는 미리보기 BottomSheet.
 *
 * 실제 공유 실행(플랫폼 공유 시트 호출)은 이 Composable의 책임이 아니다 —
 * [onShare]가 눌렸을 때 무엇을 할지는 호출부가 결정한다. [sheetState]도
 * 호출부에서 hoist해 전달받는다: 공유 버튼을 누른 뒤 시트를 애니메이션과
 * 함께 닫을지, 파일 준비 실패 등으로 열어둔 채 유지할지는 공유 실행 결과에
 * 달려 있고 그 판단은 호출부 로직 안에 있기 때문이다(스와이프/바깥 탭/
 * 뒤로가기로 닫는 경우는 Material3가 자체 처리하므로 [onDismissed]를
 * 그대로 쓴다).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SharePreviewBottomSheet(
    file: File,
    enabled: Boolean,
    sheetState: SheetState,
    onDismissed: () -> Unit,
    onShare: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissed,
        sheetState = sheetState,
        containerColor = PaperSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "엽서 공유하기",
                color = BrutalBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "완성된 엽서를 이미지로 보낼 수 있어요",
                color = InkSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PaperTray)
                    .border(
                        BorderStroke(1.dp, PaperDivider),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp)
                    .semantics {
                        contentDescription = "공유할 완성 엽서 미리보기"
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = file,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onShare,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SunsetGold,
                    contentColor = PaperSurface,
                    disabledContainerColor = SunsetGold.copy(alpha = 0.45f),
                    disabledContentColor = PaperSurface.copy(alpha = 0.65f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = PaperSurface
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "공유하기",
                    color = PaperSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
