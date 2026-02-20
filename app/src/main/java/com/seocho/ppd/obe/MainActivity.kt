package com.seocho.ppd.obe

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.seocho.ppd.obe.ui.main.MainViewModel
import com.seocho.ppd.obe.ui.main.RouteUiState
import com.seocho.ppd.obe.ui.main.VehUiState
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
fun MainScreen(
    androidId: String,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val routeState by viewModel.routeState.collectAsState()
    val vehState by viewModel.vehState.collectAsState()

    // 최초 진입 시 데이터 로드 (노선 + 차량정보)
    LaunchedEffect(Unit) {
        viewModel.loadInitialData(androidId)
    }

    // --- 인터넷 연결 상태 실시간 모니터링 ---
    var isNetworkAvailable by remember { mutableStateOf(checkNetwork(context)) }
    DisposableEffect(Unit) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isNetworkAvailable = true }
            override fun onLost(network: Network) { isNetworkAvailable = false }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        onDispose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    // --- 위치 권한 상태 ---
    var hasLocationPermission by remember {
        mutableStateOf(checkLocationPermission(context))
    }

    // 앱이 설정에서 돌아왔을 때 권한 상태 재확인
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasLocationPermission = checkLocationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 권한 요청 런처
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        hasLocationPermission = permissions.values.any { it }
        if (!hasLocationPermission) {
            showPermissionDeniedDialog = true
        }
    }

    // 최초 진입 시 권한 요청
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission && !hasRequestedPermission) {
            hasRequestedPermission = true
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    // 권한 거부 후 설정 안내 다이얼로그
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("위치 권한 필요") },
            text = { Text("운행 관리를 위해 위치 권한이 필요합니다.\n설정에서 권한을 허용해주세요.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDeniedDialog = false
                    // 앱 설정 페이지로 이동
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        },
                    )
                }) {
                    Text("설정으로 이동")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("닫기")
                }
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        var isOperating by rememberSaveable { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
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
                    modifier = Modifier.height(72.dp),
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

            // 기기 ID + 상태 표시
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // 좌측: 기기 ID
                Text(
                    text = "기기 ID: $androidId",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // 우측: 상태 인디케이터
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusIndicator(
                        label = "네트워크 상태",
                        isOk = isNetworkAvailable,
                        onClick = null,
                    )
                    StatusIndicator(
                        label = "위치 권한",
                        isOk = hasLocationPermission,
                        onClick = {
                            if (!hasLocationPermission) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            }
                        },
                    )
                }
            }

            // 차량 정보 인사 메시지
            if (vehState is VehUiState.Success) {
                val vehId = (vehState as VehUiState.Success).vehInfo.vehId
                Text(
                    text = "반갑습니다 $vehId 차량 기사님",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }

            // 노선 선택
            var selectedRoute by rememberSaveable { mutableStateOf<String?>(null) }
            var showRouteDialog by remember { mutableStateOf(false) }

            // API에서 가져온 노선 목록
            val routeNames = when (val state = routeState) {
                is RouteUiState.Success -> state.routes.map { it.routeName }
                else -> emptyList()
            }

            RouteSelectButton(
                selectedRoute = selectedRoute,
                enabled = routeState is RouteUiState.Success,
                onClick = { showRouteDialog = true },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            if (showRouteDialog && routeNames.isNotEmpty()) {
                RouteSelectDialog(
                    routeList = routeNames,
                    selectedRoute = selectedRoute,
                    onSelect = { route ->
                        selectedRoute = route
                        showRouteDialog = false
                    },
                    onDismiss = { showRouteDialog = false },
                )
            }

            // 노선 미선택 경고 다이얼로그
            var showRouteWarning by remember { mutableStateOf(false) }
            if (showRouteWarning) {
                AlertDialog(
                    onDismissRequest = { showRouteWarning = false },
                    title = {
                        Text(
                            text = "노선 미선택",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                        )
                    },
                    text = {
                        Text(
                            text = "운행할 노선을 선택하세요.",
                            fontSize = 18.sp,
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showRouteWarning = false
                            showRouteDialog = true
                        }) {
                            Text("노선 선택하기")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRouteWarning = false }) {
                            Text("닫기")
                        }
                    },
                )
            }

            // 메인 영역: 운행시작/종료 버튼
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                OperationButton(
                    isOperating = isOperating,
                    selectedRoute = selectedRoute,
                    onClick = {
                        if (selectedRoute == null) {
                            showRouteWarning = true
                            return@OperationButton
                        }
                        isOperating = !isOperating
                        // TODO: 서버 API 호출 (운행시작/종료 알림)
                    },
                )
            }
        }

        // 로딩 오버레이 (API 요청 중 화면 차단)
        val isLoading = routeState is RouteUiState.Loading || vehState is VehUiState.Loading
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(enabled = false, onClick = {}),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 6.dp,
                        color = Color.White,
                    )
                    Text(
                        text = "정보를 불러오는 중...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                }
            }
        }

        // 에러 다이얼로그 (노선 정보 로드 실패)
        if (routeState is RouteUiState.Error) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        text = "노선 정보 로드 실패",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F),
                    )
                },
                text = {
                    Text(
                        text = (routeState as RouteUiState.Error).message,
                        fontSize = 16.sp,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.loadInitialData(androidId) }) {
                        Text("다시 시도")
                    }
                },
            )
        }

        // 미등록 기기 팝업
        if (vehState is VehUiState.NotRegistered) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        text = "미등록 기기",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F),
                    )
                },
                text = {
                    Text(
                        text = "현재 기기가 관제 시스템에 등록되지 않았습니다.\n관리자에게 문의하세요.",
                        fontSize = 16.sp,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.loadVehInfo(androidId) }) {
                        Text("다시 확인")
                    }
                },
            )
        }

        // 에러 다이얼로그 (차량 정보 로드 실패)
        if (vehState is VehUiState.Error) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        text = "차량 정보 로드 실패",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F),
                    )
                },
                text = {
                    Text(
                        text = (vehState as VehUiState.Error).message,
                        fontSize = 16.sp,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.loadVehInfo(androidId) }) {
                        Text("다시 시도")
                    }
                },
            )
        }
        } // Box
    }
}

/** 인터넷/위치 상태 인디케이터 */
@Composable
private fun StatusIndicator(
    label: String,
    isOk: Boolean,
    onClick: (() -> Unit)?,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isOk) Color(0xFF2E7D32) else Color(0xFFD32F2F),
        animationSpec = tween(300),
        label = "statusBg",
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (isOk) "✔" else "✕",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

private fun checkNetwork(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private fun checkLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
}

/** 노선 선택 버튼 */
@Composable
private fun RouteSelectButton(
    selectedRoute: String?,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            .then(
                if (enabled) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "노선",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (!enabled) "로딩 중..." else (selectedRoute ?: "선택해주세요"),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (selectedRoute != null)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.outline,
        )
        Text(
            text = "▼",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 노선 선택 팝업 다이얼로그 */
@Composable
private fun RouteSelectDialog(
    routeList: List<String>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "노선 선택",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            LazyColumn {
                items(routeList) { route ->
                    val isSelected = route == selectedRoute
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                            )
                            .clickable { onSelect(route) }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = route,
                            fontSize = 20.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                        )
                        if (isSelected) {
                            Text(
                                text = "✔",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        },
    )
}

@Composable
private fun OperationButton(
    isOperating: Boolean,
    selectedRoute: String?,
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
        // 선택 운행노선 표시
        if (selectedRoute != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "선택 운행노선",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "[ $selectedRoute ]",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

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
