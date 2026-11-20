package com.fieldassets.presentation.map

import com.fieldassets.domain.model.Asset
import com.fieldassets.domain.model.AssetStatus
import com.fieldassets.domain.model.AssetType

data class AddAssetDraft(
    val latitude: Double,
    val longitude: Double,
    val name: String = "",
    val type: AssetType = AssetType.Pole,
    val status: AssetStatus = AssetStatus.Active,
    val submitting: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val error: String? = null,
)

data class AssetsUiState(
    val assets: List<Asset> = emptyList(),
    val loading: Boolean = true,
    val loadError: String? = null,
    val selectedId: String? = null,
    /** Status chosen in the detail panel but not yet saved; null means "same as persisted". */
    val pendingStatus: AssetStatus? = null,
    val savingStatus: Boolean = false,
    val draft: AddAssetDraft? = null,
) {
    val selected: Asset? get() = assets.firstOrNull { it.id == selectedId }
    val shownStatus: AssetStatus? get() = pendingStatus ?: selected?.status
}

sealed interface UiEvent {
    data class FocusOn(val latitude: Double, val longitude: Double) : UiEvent
    data object FitAll : UiEvent
    data class Message(val text: String) : UiEvent
}
