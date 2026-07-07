package com.postcardmemory.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.components.SealPreviewContent
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.NeutralLight
import com.postcardmemory.ui.theme.SealInkWhite
import com.postcardmemory.ui.theme.SoftGray
import com.postcardmemory.ui.theme.sealInkColors

@Composable
fun SealPickerPanel(
    photoSeals: List<PostcardSealItem>,
    selectedSealId: String?,
    onSelectSeal: (String) -> Unit,
    onAddSeal: (SealType) -> Unit,
    onDeleteSeal: (String) -> Unit,
    onScaleChanged: (String, Float) -> Unit,
    onRotateBy: (String, Float) -> Unit,
    onColorSelected: (String, Long) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedSeal =
        photoSeals.find { it.id == selectedSealId }

    Column(
        modifier = modifier
            .background(
                color = NeutralLight,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "도장 꾸미기",
                color = BrutalBlack,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "${photoSeals.size}개",
                color = BrutalBlack,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .background(
                        color = BrutalWhite,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "우편 소인 느낌의 도장을 골라서 포스트카드 위에 찍어봐.",
            color = BrutalBlack,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "도장 종류",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SealType.entries.forEach { type ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = enabled) {
                            onAddSeal(type)
                        }
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = BrutalWhite,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(6.dp)
                    ) {
                        SealPreviewContent(
                            type = type,
                            color = BrutalBlack,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = type.label,
                        color = BrutalBlack,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        if (photoSeals.isEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "위에서 마음에 드는 도장을 눌러서 추가해봐.",
                color = BrutalBlack,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = BrutalWhite,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )

            return@Column
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            photoSeals.forEach { seal ->
                val isSelected = seal.id == selectedSealId
                val isWhiteInk =
                    (seal.colorArgb == (SealInkWhite.toArgb().toLong() and 0xFFFFFFFFL))

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = when {
                                isSelected -> GraphiteAccent
                                isWhiteInk -> NeutralLight
                                else -> BrutalWhite
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = enabled) {
                            onSelectSeal(seal.id)
                        }
                        .padding(if (isSelected) 10.dp else 6.dp)
                ) {
                    SealPreviewContent(
                        type = seal.type,
                        color = if (isSelected) BrutalWhite else Color(seal.colorArgb),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (selectedSeal == null) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "추가한 도장을 눌러서 선택하면 여기서 꾸밀 수 있어.",
                color = BrutalBlack,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = BrutalWhite,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )

            return@Column
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BrutalBlack.copy(alpha = 0.15f))
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "선택된 도장",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "이동은 미리보기에서 드래그하고, 크기·회전은 아래에서도 세밀하게 조절할 수 있어.",
            color = BrutalBlack,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "크기",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = selectedSeal.scale,
            onValueChange = { newValue ->
                onScaleChanged(selectedSeal.id, newValue)
            },
            valueRange = 0.5f..3f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = BrutalBlack,
                activeTrackColor = BrutalBlack,
                inactiveTrackColor = SoftGray
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "색상",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sealInkColors.forEach { inkColor ->
                val inkArgb =
                    inkColor.toArgb().toLong() and 0xFFFFFFFFL
                val isColorSelected =
                    selectedSeal.colorArgb == inkArgb

                Box(
                    modifier = Modifier
                        .size(if (isColorSelected) 34.dp else 28.dp)
                        .background(
                            color = inkColor,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.5.dp,
                            color = BrutalBlack.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                        .clickable(enabled = enabled) {
                            onColorSelected(selectedSeal.id, inkArgb)
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "회전",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    onRotateBy(selectedSeal.id, -15f)
                },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftGray,
                    contentColor = BrutalBlack
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "왼쪽으로 15°",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Button(
                onClick = {
                    onRotateBy(selectedSeal.id, 15f)
                },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftGray,
                    contentColor = BrutalBlack
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "오른쪽으로 15°",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { onDeleteSeal(selectedSeal.id) },
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = BrutalCoral.copy(alpha = 0.16f),
                contentColor = BrutalCoral
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = BrutalCoral
            )
            Text(
                text = "  선택한 도장 삭제",
                color = BrutalCoral,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
