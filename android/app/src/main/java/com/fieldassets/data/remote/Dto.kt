
package com.fieldassets.data.remote

import com.fieldassets.domain.model.Asset
import com.fieldassets.domain.model.AssetStatus
import com.fieldassets.domain.model.AssetType
import com.fieldassets.domain.model.NewAsset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AssetDto(
    val id: String,
    val name: String,
    @SerialName("asset_type") val assetType: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
)

@Serializable
data class CreateAssetRequest(
    val name: String,
    @SerialName("asset_type") val assetType: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
)

@Serializable
data class UpdateStatusRequest(val status: String)

@Serializable
data class AssetListResponse(
    val success: Boolean,
    val count: Int,
    val data: List<AssetDto>
)

@Serializable
data class AssetResponse(
    val success: Boolean,
    val message: String? = null,
    val data: AssetDto
)

@Serializable
data class ErrorEnvelope(val error: ErrorBody? = null)

@Serializable
data class ErrorBody(
    val code: String? = null,
    val message: String? = null,
    val details: List<FieldErrorDto> = emptyList(),
)

@Serializable
data class FieldErrorDto(val field: String, val message: String)

/** Returns null for records with an asset type or status this client does not understand. */
fun AssetDto.toDomain(): Asset? {
    val type = AssetType.entries.firstOrNull { it.name == assetType }
        ?: return null

    val parsedStatus = AssetStatus.entries.firstOrNull { it.name == status }
        ?: return null

    return Asset(id, name, type, latitude, longitude, parsedStatus)
}

fun NewAsset.toRequest() =
    CreateAssetRequest(name, type.name, latitude, longitude, status.name)
