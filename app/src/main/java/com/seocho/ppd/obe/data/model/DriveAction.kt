package com.seocho.ppd.obe.data.model

import kotlinx.serialization.Serializable

/** POST /drive-action 요청 바디 */
@Serializable
data class DriveActionRequest(
    val androidId: String,
    val routeEid: Long,
    val driveAction: Boolean,
)

/** POST /drive-action 응답 바디 */
@Serializable
data class DriveActionResponse(
    val success: Boolean,
    val message: String,
)
