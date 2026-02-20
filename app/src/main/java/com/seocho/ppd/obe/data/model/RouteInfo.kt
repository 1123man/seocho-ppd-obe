package com.seocho.ppd.obe.data.model

import kotlinx.serialization.Serializable

/**
 * GET /route-info 응답 모델
 * 백엔드 Route 엔티티에서 앱에 필요한 필드만 파싱
 * (ignoreUnknownKeys = true 설정으로 나머지 필드 무시)
 */
@Serializable
data class RouteInfo(
    val entityId: Long? = null,
    val routeId: String,
    val routeName: String,
)
