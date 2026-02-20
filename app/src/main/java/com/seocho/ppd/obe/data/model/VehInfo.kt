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
)
