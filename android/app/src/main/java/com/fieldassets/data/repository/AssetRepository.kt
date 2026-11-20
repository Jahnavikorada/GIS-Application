
package com.fieldassets.data.repository

import com.fieldassets.data.remote.AppJson
import com.fieldassets.data.remote.AssetApi
import com.fieldassets.data.remote.AssetDto
import com.fieldassets.data.remote.AssetResponse
import com.fieldassets.data.remote.ErrorEnvelope
import com.fieldassets.data.remote.UpdateStatusRequest
import com.fieldassets.data.remote.toDomain
import com.fieldassets.data.remote.toRequest
import com.fieldassets.domain.model.ApiException
import com.fieldassets.domain.model.Asset
import com.fieldassets.domain.model.AssetStatus
import com.fieldassets.domain.model.NewAsset
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

class AssetRepository(private val api: AssetApi) {

    suspend fun getAssets(): Result<List<Asset>> = call {
        api.getAssets().data.mapNotNull { it.toDomain() }
    }

    suspend fun createAsset(asset: NewAsset): Result<Asset> = call {
        requireDomain(api.createAsset(asset.toRequest()).data)
    }

    suspend fun updateStatus(
        id: String,
        status: AssetStatus
    ): Result<Asset> = call {
        requireDomain(
            api.updateStatus(id, UpdateStatusRequest(status.name)).data
        )
    }

    private fun requireDomain(dto: AssetDto): Asset =
        dto.toDomain()
            ?: throw SerializationException(
                "Unsupported asset returned by server"
            )

    private suspend fun <T> call(
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Result.failure(e.toApiException())
        } catch (e: IOException) {
            Result.failure(
                ApiException("Network error. Please check your connection.")
            )
        } catch (e: SerializationException) {
            Result.failure(
                ApiException("The server returned an unexpected response.")
            )
        } catch (e: Exception) {
            Result.failure(
                ApiException(e.message ?: "Something went wrong.")
            )
        }
    }

    private fun HttpException.toApiException(): ApiException {
        val body = runCatching {
            response()?.errorBody()?.string()?.let {
                AppJson.decodeFromString<ErrorEnvelope>(it).error
            }
        }.getOrNull()

        val fields = body?.details.orEmpty()
            .associate { it.field to it.message }

        val message = body?.message ?: "Request failed (HTTP ${code()})"

        return ApiException(message, fields)
    }
}
