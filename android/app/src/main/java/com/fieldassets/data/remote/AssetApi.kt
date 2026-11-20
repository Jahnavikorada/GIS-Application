package com.fieldassets.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface AssetApi {

    @GET("api/assets")
    suspend fun getAssets(): AssetListResponse

    @POST("api/assets")
    suspend fun createAsset(
        @Body body: CreateAssetRequest
    ): AssetResponse

    @PATCH("api/assets/{id}")
    suspend fun updateStatus(
        @Path("id") id: String,
        @Body body: UpdateStatusRequest
    ): AssetResponse
}