package com.seocho.ppd.obe

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seocho.ppd.obe.ui.theme.ObeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        setContent {
            ObeTheme {
                MainScreen(androidId = androidId)
            }
        }
    }
}

@Composable
fun MainScreen(androidId: String) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        var isOperating by rememberSaveable { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 상단 헤더: 로고(좌) + 앱명(우), 흰색 배경
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.seocho_top_logo),
                    contentDescription = "서초구 로고",
                    modifier = Modifier.height(56.dp),
                    contentScale = ContentScale.FillHeight,
                )
                Text(
                    text = "서초구 운전자앱",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // 상단/하단 영역 구분선
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 기기 ID 표시
            Text(
                text = "기기 ID: $androidId",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )

            // 메인 영역: 운행시작/종료 버튼
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                OperationButton(
                    isOperating = isOperating,
                    onClick = {
                        isOperating = !isOperating
                        // TODO: 서버 API 호출 (운행시작/종료 알림)
                    },
                )
            }
        }
    }
}

@Composable
private fun OperationButton(
    isOperating: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 누를 때 살짝 축소되는 애니메이션
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = tween(100),
        label = "scale",
    )

    // 상태 전환 시 색상 부드럽게 변경
    val startColor by animateColorAsState(
        targetValue = if (isOperating) Color(0xFFE53935) else Color(0xFF1B5E20),
        animationSpec = tween(300),
        label = "startColor",
    )
    val endColor by animateColorAsState(
        targetValue = if (isOperating) Color(0xFFC62828) else Color(0xFF2E7D32),
        animationSpec = tween(300),
        label = "endColor",
    )

    // 상태 전환 시 라벨 색상도 애니메이션
    val labelColor by animateColorAsState(
        targetValue = if (isOperating) Color(0xFFE53935) else Color(0xFF1B5E20),
        animationSpec = tween(300),
        label = "labelColor",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 상태 라벨 - 크고 굵게
        Text(
            text = if (isOperating) "운행 중" else "운행 대기",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 원형 그라데이션 버튼
        Box(
            modifier = Modifier
                .size(320.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = if (isPressed) 4.dp else 12.dp,
                    shape = CircleShape,
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(startColor, endColor),
                    ),
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isOperating) "운행종료" else "운행시작",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 안내 텍스트 - 크기 키우고 색상 강조
        Text(
            text = if (isOperating) "버튼을 누르면 운행이 종료됩니다" else "버튼을 누르면 운행이 시작됩니다",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    ObeTheme {
        MainScreen(androidId = "a1b2c3d4e5f6")
    }
}
