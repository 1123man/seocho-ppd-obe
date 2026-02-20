package com.seocho.ppd.obe.data.api

import com.seocho.ppd.obe.data.model.RouteInfo
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ObeApiService {

    @GET("route-info")
    suspend fun getRouteInfo(): List<RouteInfo>

    @GET("get-vehinfo")
    suspend fun getVehInfo(@Query("androidId") androidId: String): Response<ResponseBody>
}
