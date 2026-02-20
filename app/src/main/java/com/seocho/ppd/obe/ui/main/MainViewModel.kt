package com.seocho.ppd.obe.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seocho.ppd.obe.data.api.ObeApiService
import com.seocho.ppd.obe.data.model.DriveActionRequest
import com.seocho.ppd.obe.data.model.RouteInfo
import com.seocho.ppd.obe.data.model.VehInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val apiService: ObeApiService,
    private val json: Json,
) : ViewModel() {

    private val _routeState = MutableStateFlow<RouteUiState>(RouteUiState.Loading)
    val routeState: StateFlow<RouteUiState> = _routeState.asStateFlow()

    private val _vehState = MutableStateFlow<VehUiState>(VehUiState.Loading)
    val vehState: StateFlow<VehUiState> = _vehState.asStateFlow()

    private val _driveState = MutableStateFlow<DriveUiState>(DriveUiState.Idle)
    val driveState: StateFlow<DriveUiState> = _driveState.asStateFlow()

    fun loadInitialData(androidId: String) {
        loadRoutes()
        loadVehInfo(androidId)
    }

    fun loadRoutes() {
        viewModelScope.launch {
            _routeState.value = RouteUiState.Loading
            try {
                val routes = apiService.getRouteInfo()
                _routeState.value = RouteUiState.Success(routes)
            } catch (e: Exception) {
                _routeState.value = RouteUiState.Error(toUserMessage(e))
            }
        }
    }

    fun loadVehInfo(androidId: String) {
        viewModelScope.launch {
            _vehState.value = VehUiState.Loading
            try {
                val response = apiService.getVehInfo(androidId)
                if (!response.isSuccessful) {
                    _vehState.value = VehUiState.Error("서버 오류 (${response.code()})")
                    return@launch
                }
                val bodyString = response.body()?.string()
                if (bodyString.isNullOrBlank() || bodyString.trim() == "null") {
                    _vehState.value = VehUiState.NotRegistered
                } else {
                    val vehInfo = json.decodeFromString<VehInfo>(bodyString)
                    _vehState.value = VehUiState.Success(vehInfo)
                }
            } catch (e: Exception) {
                _vehState.value = VehUiState.Error(toUserMessage(e))
            }
        }
    }

    fun sendDriveAction(androidId: String, routeEid: Long, driveAction: Boolean) {
        viewModelScope.launch {
            _driveState.value = DriveUiState.Loading
            try {
                val response = apiService.driveAction(
                    DriveActionRequest(
                        androidId = androidId,
                        routeEid = routeEid,
                        driveAction = driveAction,
                    ),
                )
                if (response.success) {
                    _driveState.value = DriveUiState.Success(driveAction)
                } else {
                    _driveState.value = DriveUiState.Error(response.message)
                }
            } catch (e: Exception) {
                _driveState.value = DriveUiState.Error(toUserMessage(e))
            }
        }
    }

    fun resetDriveState() {
        _driveState.value = DriveUiState.Idle
    }

    private fun toUserMessage(e: Exception): String = when (e) {
        is ConnectException -> "서버에 연결할 수 없습니다.\n네트워크 상태 또는 서버 주소를 확인하세요."
        is SocketTimeoutException -> "서버 응답 시간이 초과되었습니다.\n잠시 후 다시 시도하세요."
        is UnknownHostException -> "서버 주소를 찾을 수 없습니다.\n네트워크 연결을 확인하세요."
        else -> "오류가 발생했습니다.\n(${e.javaClass.simpleName}: ${e.message})"
    }
}

sealed interface RouteUiState {
    data object Loading : RouteUiState
    data class Success(val routes: List<RouteInfo>) : RouteUiState
    data class Error(val message: String) : RouteUiState
}

sealed interface VehUiState {
    data object Loading : VehUiState
    data class Success(val vehInfo: VehInfo) : VehUiState
    data object NotRegistered : VehUiState
    data class Error(val message: String) : VehUiState
}

sealed interface DriveUiState {
    data object Idle : DriveUiState
    data object Loading : DriveUiState
    data class Success(val isStart: Boolean) : DriveUiState
    data class Error(val message: String) : DriveUiState
}
