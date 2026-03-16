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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.runtime.mutableIntStateOf
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
import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.seocho.ppd.obe.ui.main.DriveUiState
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val routeState by viewModel.routeState.collectAsState()
    val vehState by viewModel.vehState.collectAsState()
    val driveState by viewModel.driveState.collectAsState()

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

    // --- "다른 앱 위에 표시" 권한 (부팅 자동시작에 필요) ---
    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }
    var showOverlayDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasOverlayPermission) {
            showOverlayDialog = true
        }
    }

    if (showOverlayDialog && !hasOverlayPermission) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "권한 설정 필요",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "부팅 시 앱 자동 시작을 위해\n\"다른 앱 위에 표시\" 권한이 필요합니다.\n\n설정 화면에서 허용해주세요.",
                    fontSize = 16.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showOverlayDialog = false
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                }) {
                    Text("설정으로 이동")
                }
            },
        )
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
                hasOverlayPermission = Settings.canDrawOverlays(context)
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
        // 토스트: Pair(노선명, 시작여부) - null이면 숨김
        var operationToast by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
        // 노선 선택 상태 (Box 스코프에서도 접근 필요)
        var selectedRoute by rememberSaveable { mutableStateOf<String?>(null) }
        var selectedRouteEid by rememberSaveable { mutableStateOf<Long?>(null) }

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
                    .padding(
                        horizontal = 20.dp,
                        vertical = if (isLandscape) 8.dp else 16.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.seocho_top_logo),
                    contentDescription = "서초구 로고",
                    modifier = Modifier.height(if (isLandscape) 44.dp else 72.dp),
                    contentScale = ContentScale.FillHeight,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 실시간 날짜·시간
                    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            currentTime = System.currentTimeMillis()
                            kotlinx.coroutines.delay(1000)
                        }
                    }
                    val dateFormat = remember {
                        java.text.SimpleDateFormat("yyyy.MM.dd (E)", java.util.Locale.KOREAN)
                    }
                    val timeFormat = remember {
                        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.KOREAN)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = dateFormat.format(java.util.Date(currentTime)),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "|",
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            Text(
                                text = timeFormat.format(java.util.Date(currentTime)),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Text(
                        text = "서초구 운전자앱",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
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
                    text = "안드로이드 ID: $androidId",
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

            // 노선 선택
            var showRouteDialog by remember { mutableStateOf(false) }

            // API에서 가져온 노선 목록
            val routes = when (val state = routeState) {
                is RouteUiState.Success -> state.routes
                else -> emptyList()
            }

            var showRouteChangeWarning by remember { mutableStateOf(false) }

            RouteSelectButton(
                selectedRoute = selectedRoute,
                enabled = routeState is RouteUiState.Success,
                onClick = {
                    if (isOperating) {
                        showRouteChangeWarning = true
                    } else {
                        showRouteDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )

            if (showRouteChangeWarning) {
                AlertDialog(
                    onDismissRequest = { showRouteChangeWarning = false },
                    title = {
                        Text(
                            text = "노선 변경 불가",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                        )
                    },
                    text = {
                        Text(
                            text = "운행 중에는 노선을 변경할 수 없습니다.\n운행을 종료한 후 변경해주세요.",
                            fontSize = 16.sp,
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showRouteChangeWarning = false }) {
                            Text("확인")
                        }
                    },
                )
            }

            if (showRouteDialog && routes.isNotEmpty()) {
                RouteSelectDialog(
                    routeList = routes.map { it.routeName },
                    selectedRoute = selectedRoute,
                    onSelect = { routeName ->
                        selectedRoute = routeName
                        selectedRouteEid = routes.first { it.routeName == routeName }.entityId
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

            // 차량 정보 인사 메시지
            if (vehState is VehUiState.Success) {
                val vehId = (vehState as VehUiState.Success).vehInfo.vehId
                if (isLandscape) {
                    // 가로: 한 줄로 콤팩트하게
                    Text(
                        text = buildAnnotatedString {
                            append("반갑습니다. ")
                            withStyle(SpanStyle(color = Color(0xFFE65100), fontWeight = FontWeight.ExtraBold)) {
                                append(vehId)
                            }
                            append(" 기사님, \uD83D\uDE8C 안전운전 하세요!")
                        },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    // 세로: 3줄 강조
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    ) {
                        Text(
                            text = "반갑습니다.",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = Color(0xFFE65100))) {
                                    append(vehId)
                                }
                                append(" 기사님")
                            },
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\uD83D\uDE8C 안전운전 하세요!",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32),
                        )
                    }
                }
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
                    isLandscape = isLandscape,
                    onClick = {
                        if (selectedRoute == null) {
                            showRouteWarning = true
                            return@OperationButton
                        }
                        viewModel.sendDriveAction(
                            androidId = androidId,
                            routeEid = selectedRouteEid!!,
                            driveAction = !isOperating,
                        )
                    },
                )
            }
        }

        // 운행 시작/종료 토스트 메시지 (5초 fade)
        LaunchedEffect(operationToast) {
            if (operationToast != null) {
                kotlinx.coroutines.delay(5000)
                operationToast = null
            }
        }
        AnimatedVisibility(
            visible = operationToast != null,
            enter = fadeIn(tween(400)) + scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(400),
            ),
            exit = fadeOut(tween(600)) + scaleOut(
                targetScale = 0.95f,
                animationSpec = tween(600),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = if (isLandscape) 80.dp else 200.dp),
        ) {
            val toastData = operationToast
            val isStart = toastData?.second ?: true
            val gradientColors = if (isStart) {
                listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF388E3C))
            } else {
                listOf(Color(0xFFB71C1C), Color(0xFFC62828), Color(0xFFD32F2F))
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .shadow(16.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.horizontalGradient(colors = gradientColors),
                        )
                        .padding(horizontal = 56.dp, vertical = 36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "${if (isStart) "\uD83D\uDE8C" else "\uD83D\uDED1"} ${toastData?.first ?: ""}",
                            fontSize = if (isLandscape) 32.sp else 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isStart) "운행을 시작합니다" else "운행을 종료합니다",
                            fontSize = if (isLandscape) 26.sp else 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isStart) Color(0xFFA5D6A7) else Color(0xFFEF9A9A),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isStart) "안전운전 하세요!" else "수고하셨습니다!",
                            fontSize = if (isLandscape) 22.sp else 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isStart) Color(0xFFE8F5E9) else Color(0xFFFFCDD2),
                        )
                    }
                }
            }
        }

        // 차량에 이미 할당된 노선이 있으면 자동 선택 + 운행 상태 복원
        LaunchedEffect(vehState) {
            if (vehState is VehUiState.Success) {
                val vehInfo = (vehState as VehUiState.Success).vehInfo
                val route = vehInfo.route
                if (route != null && route.routeName != null && route.entityId != null) {
                    selectedRoute = route.routeName
                    selectedRouteEid = route.entityId
                }
                // 서버 Redis에 운행중("Y")이면 앱 운행 상태 복원
                if (vehInfo.androidVehDriving == "Y") {
                    isOperating = true
                }
            }
        }

        // 운행 API 성공 시 상태 전환 + 토스트
        LaunchedEffect(driveState) {
            if (driveState is DriveUiState.Success) {
                val isStart = (driveState as DriveUiState.Success).isStart
                isOperating = isStart
                operationToast = Pair(selectedRoute ?: "", isStart)
                viewModel.resetDriveState()
            }
        }

        // 운행 API 에러 다이얼로그
        if (driveState is DriveUiState.Error) {
            AlertDialog(
                onDismissRequest = { viewModel.resetDriveState() },
                title = {
                    Text(
                        text = "운행 처리 실패",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F),
                    )
                },
                text = {
                    Text(
                        text = (driveState as DriveUiState.Error).message,
                        fontSize = 16.sp,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetDriveState() }) {
                        Text("확인")
                    }
                },
            )
        }

        // 로딩 오버레이 (API 요청 중 화면 차단)
        val isLoading = routeState is RouteUiState.Loading || vehState is VehUiState.Loading
                || driveState is DriveUiState.Loading
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
                        text = if (driveState is DriveUiState.Loading) "운행 처리 중..." else "정보를 불러오는 중...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                }
            }
        }

        // 에러 다이얼로그 (노선 정보 로드 실패) - 15초 자동 다시시도
        if (routeState is RouteUiState.Error) {
            AutoRetryErrorDialog(
                title = "노선 정보 로드 실패",
                message = (routeState as RouteUiState.Error).message,
                onRetry = { viewModel.loadInitialData(androidId) },
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
                        text = "현재 기기가 관제 시스템에 등록되지 않았습니다.\n관리자에게 문의하세요.\n\n현재 차량번호와 안드로이드 ID '$androidId' 정보를 관리자에게 알려주세요.",
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

        // 에러 다이얼로그 (차량 정보 로드 실패) - 15초 자동 다시시도
        if (vehState is VehUiState.Error) {
            AutoRetryErrorDialog(
                title = "차량 정보 로드 실패",
                message = (vehState as VehUiState.Error).message,
                onRetry = { viewModel.loadVehInfo(androidId) },
            )
        }
        } // Box
    }
}

/**
 * 자동 다시시도 카운트다운이 포함된 에러 다이얼로그.
 * 15초마다 자동으로 [onRetry]를 호출하며, '다시 시도' 버튼 클릭 시 카운트다운을 리셋한다.
 */
@Composable
private fun AutoRetryErrorDialog(
    title: String,
    message: String,
    retryIntervalSeconds: Int = 15,
    onRetry: () -> Unit,
) {
    var countdown by remember { mutableIntStateOf(retryIntervalSeconds) }
    // retryTrigger를 변경하면 LaunchedEffect가 재시작되어 카운트다운이 리셋됨
    var retryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryTrigger) {
        countdown = retryIntervalSeconds
        while (countdown > 0) {
            kotlinx.coroutines.delay(1000)
            countdown--
        }
        onRetry()
        // 빠른 실패 시 다이얼로그가 재생성되지 않을 수 있으므로 카운트다운 재시작
        retryTrigger++
    }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F),
            )
        },
        text = {
            Column {
                Text(
                    text = message,
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "자동 다시시도 (${countdown}초)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                retryTrigger++
                onRetry()
            }) {
                Text("다시 시도")
            }
        },
    )
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
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
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
    isLandscape: Boolean = false,
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

    val buttonSize = if (isLandscape) 260.dp else 320.dp
    val buttonTextSize = if (isLandscape) 42.sp else 48.sp
    val statusLabelSize = if (isLandscape) 26.sp else 32.sp
    val guideTextSize = if (isLandscape) 18.sp else 20.sp
    val routeLabelSize = if (isLandscape) 18.sp else 20.sp
    val routeValueSize = if (isLandscape) 22.sp else 24.sp
    val topSpacerHeight = if (isLandscape) 10.dp else 32.dp
    val bottomSpacerHeight = if (isLandscape) 8.dp else 24.dp
    val routeSpacerHeight = if (isLandscape) 6.dp else 16.dp

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
                    fontSize = routeLabelSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "[ $selectedRoute ]",
                    fontSize = routeValueSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(routeSpacerHeight))
        }

        // 상태 라벨 - 크고 굵게
        Text(
            text = if (isOperating) "운행 중" else "운행 대기",
            fontSize = statusLabelSize,
            fontWeight = FontWeight.Bold,
            color = labelColor,
        )

        Spacer(modifier = Modifier.height(topSpacerHeight))

        // 원형 그라데이션 버튼
        Box(
            modifier = Modifier
                .size(buttonSize)
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
                fontSize = buttonTextSize,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(bottomSpacerHeight))

        // 안내 텍스트 - '시작'/'종료' 강조
        Text(
            text = buildAnnotatedString {
                append("버튼을 누르면 운행이 ")
                withStyle(
                    SpanStyle(
                        color = if (isOperating) Color(0xFFD32F2F) else Color(0xFF1B5E20),
                        fontWeight = FontWeight.Bold,
                    ),
                ) {
                    append(if (isOperating) "종료" else "시작")
                }
                append("됩니다")
            },
            fontSize = guideTextSize,
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
