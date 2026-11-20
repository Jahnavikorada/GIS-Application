package com.fieldassets.domain.model

enum class AssetType { Pole, Transformer, Substation }

enum class AssetStatus { Active, Inactive }

data class Asset(
    val id: String,
    val name: String,
    val type: AssetType,
    val latitude: Double,
    val longitude: Double,
    val status: AssetStatus,
)

data class NewAsset(
    val name: String,
    val type: AssetType,
    val latitude: Double,
    val longitude: Double,
    val status: AssetStatus,
)

/** Failure with a message that is safe to show to the user, plus per-field API validation errors. */
class ApiException(
    message: String,
    val fieldErrors: Map<String, String> = emptyMap(),
) : Exception(message)
