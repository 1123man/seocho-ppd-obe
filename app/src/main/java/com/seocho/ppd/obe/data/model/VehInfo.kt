package com.seocho.ppd.obe.data.model

import kotlinx.serialization.Serializable

/**
 * GET /get-vehinfo 응답 모델
 * 백엔드 Veh 엔티티에서 앱에 필요한 필드만 파싱
 */
@Serializable
data class VehInfo(
    val entityId: Long? = null,
    val vehId: String,
    val vehName: String,
    val androidId: String? = null,
    val route: VehRoute? = null,
    val androidVehDriving: String? = null,
)

/**
 * 차량에 할당된 노선 정보 (Veh.route).
 * 백엔드 Route 엔티티에서 앱에 필요한 필드만 파싱.
 */
@Serializable
data class VehRoute(
    val entityId: Long? = null,
    val routeId: String? = null,
    val routeName: String? = null,
)
