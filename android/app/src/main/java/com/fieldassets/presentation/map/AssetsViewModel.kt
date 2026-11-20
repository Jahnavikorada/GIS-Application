package com.fieldassets.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fieldassets.FieldAssetsApp
import com.fieldassets.data.repository.AssetRepository
import com.fieldassets.domain.model.ApiException
import com.fieldassets.domain.model.Asset
import com.fieldassets.domain.model.AssetStatus
import com.fieldassets.domain.model.AssetType
import com.fieldassets.domain.model.NewAsset
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AssetsViewModel(private val repository: AssetRepository) : ViewModel() {

    private val _state = MutableStateFlow(AssetsUiState())
    val state: StateFlow<AssetsUiState> = _state.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var initialFitDone = false

    init {
        loadAssets()
    }

    fun loadAssets() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, loadError = null) }
            fetchAssets()
        }
    }

    private suspend fun fetchAssets() {
        repository.getAssets().fold(
            onSuccess = { assets ->
                _state.update { it.copy(assets = assets, loading = false, loadError = null) }
                if (!initialFitDone && assets.isNotEmpty()) {
                    initialFitDone = true
                    _events.send(UiEvent.FitAll)
                }
            },
            onFailure = { error ->
                val hadData = _state.value.assets.isNotEmpty()
                _state.update { it.copy(loading = false, loadError = if (hadData) null else error.message) }
                if (hadData) _events.send(UiEvent.Message(error.message ?: "Couldn't refresh assets"))
            },
        )
    }

    fun select(asset: Asset) {
        _state.update { it.copy(selectedId = asset.id, pendingStatus = null, savingStatus = false) }
        _events.trySend(UiEvent.FocusOn(asset.latitude, asset.longitude))
    }

    fun clearSelection() {
        _state.update { it.copy(selectedId = null, pendingStatus = null, savingStatus = false) }
    }

    fun chooseStatus(status: AssetStatus) {
        _state.update { current ->
            val persisted = current.selected?.status
            current.copy(pendingStatus = status.takeIf { it != persisted })
        }
    }

    fun saveStatus() {
        val current = _state.value
        val asset = current.selected ?: return
        val target = current.pendingStatus ?: return
        if (current.savingStatus) return

        viewModelScope.launch {
            _state.update { it.copy(savingStatus = true) }
            repository.updateStatus(asset.id, target).fold(
                onSuccess = { updated ->
                    _state.update { s ->
                        s.copy(
                            assets = s.assets.map { if (it.id == updated.id) updated else it },
                            pendingStatus = null,
                            savingStatus = false,
                        )
                    }
                    _events.send(UiEvent.Message("${updated.name} is now ${updated.status.name}"))
                },
                onFailure = { error ->
                    // Persisted status is untouched; drop the unsaved selection so the UI shows the truth.
                    _state.update { it.copy(pendingStatus = null, savingStatus = false) }
                    _events.send(UiEvent.Message(error.message ?: "Couldn't update status"))
                },
            )
        }
    }

    fun startAdd(latitude: Double, longitude: Double) {
        _state.update { it.copy(draft = AddAssetDraft(latitude, longitude), selectedId = null, pendingStatus = null) }
    }

    fun cancelAdd() {
        _state.update { it.copy(draft = null) }
    }

    fun updateDraftName(name: String) = updateDraft { it.copy(name = name, fieldErrors = it.fieldErrors - "name") }

    fun updateDraftType(type: AssetType) = updateDraft { it.copy(type = type, fieldErrors = it.fieldErrors - "asset_type") }

    fun updateDraftStatus(status: AssetStatus) = updateDraft { it.copy(status = status, fieldErrors = it.fieldErrors - "status") }

    private fun updateDraft(transform: (AddAssetDraft) -> AddAssetDraft) {
        _state.update { s -> s.draft?.let { s.copy(draft = transform(it)) } ?: s }
    }

    fun submitAdd() {
        val draft = _state.value.draft ?: return
        if (draft.submitting) return
        if (draft.name.isBlank()) {
            updateDraft { it.copy(fieldErrors = mapOf("name" to "Name is required"), error = null) }
            return
        }

        viewModelScope.launch {
            updateDraft { it.copy(submitting = true, fieldErrors = emptyMap(), error = null) }
            val request = NewAsset(draft.name.trim(), draft.type, draft.latitude, draft.longitude, draft.status)
            repository.createAsset(request).fold(
                onSuccess = { created ->
                    _state.update { it.copy(draft = null, assets = it.assets + created) }
                    fetchAssets()
                    val shown = _state.value.assets.firstOrNull { it.id == created.id } ?: created
                    select(shown)
                    _events.send(UiEvent.Message("${created.name} added"))
                },
                onFailure = { error ->
                    val api = error as? ApiException
                    updateDraft {
                        it.copy(
                            submitting = false,
                            fieldErrors = api?.fieldErrors.orEmpty(),
                            error = error.message,
                        )
                    }
                },
            )
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AssetsViewModel((this[APPLICATION_KEY] as FieldAssetsApp).repository) }
        }
    }
}
