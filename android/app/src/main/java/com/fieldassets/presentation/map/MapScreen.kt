package com.fieldassets.presentation.map

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fieldassets.domain.model.Asset
import com.fieldassets.domain.model.AssetStatus
import com.fieldassets.presentation.assets.AddAssetSheet
import com.fieldassets.presentation.assets.AssetDetailPanel
import com.fieldassets.presentation.assets.AssetListPanel
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.launch

private val DefaultCenter = LatLng(26.16, 91.76)
private const val DefaultZoom = 12f
private const val FocusZoom = 15f
private const val CameraAnimationMs = 600
private val ListPeekHeight = 200.dp
private val DetailPeekHeight = 400.dp
private val ListSheetHeight = 380.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: AssetsViewModel = viewModel(factory = AssetsViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(DefaultCenter, DefaultZoom) }
    val snackbarHost = remember { SnackbarHostState() }
    val scaffoldState = rememberBottomSheetScaffoldState(snackbarHostState = snackbarHost)
    val scope = rememberCoroutineScope()
    val fitPaddingPx = with(LocalDensity.current) { 72.dp.roundToPx() }

    var mapLoaded by remember { mutableStateOf(false) }
    var fitRequested by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.FocusOn -> scope.launch {
                    val target = LatLng(event.latitude, event.longitude)
                    camera.animate(
                        CameraUpdateFactory.newLatLngZoom(target, maxOf(camera.position.zoom, FocusZoom)),
                        CameraAnimationMs,
                    )
                }
                UiEvent.FitAll -> fitRequested = true
                is UiEvent.Message -> scope.launch {
                    snackbarHost.currentSnackbarData?.dismiss()
                    snackbarHost.showSnackbar(event.text)
                }
            }
        }
    }

    LaunchedEffect(fitRequested, mapLoaded, state.assets) {
        if (fitRequested && mapLoaded && state.assets.isNotEmpty()) {
            camera.animate(fitUpdate(state.assets, fitPaddingPx), CameraAnimationMs)
            fitRequested = false
        }
    }

    LaunchedEffect(state.selectedId) {
        if (state.selectedId != null) scaffoldState.bottomSheetState.partialExpand()
    }

    BackHandler(enabled = state.selected != null) { viewModel.clearSelection() }

    val selected = state.selected
    val peekHeight by animateDpAsState(if (selected != null) DetailPeekHeight else ListPeekHeight, label = "peek")

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = peekHeight,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShadowElevation = 8.dp,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { TitleWithCount(state.assets.size) },
                actions = {
                    IconButton(onClick = viewModel::loadAssets, enabled = !state.loading) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh assets")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        sheetContent = {
            if (selected != null) {
                AssetDetailPanel(
                    asset = selected,
                    pendingStatus = state.pendingStatus,
                    saving = state.savingStatus,
                    onChooseStatus = viewModel::chooseStatus,
                    onSave = viewModel::saveStatus,
                    onClose = viewModel::clearSelection,
                )
            } else {
                AssetListPanel(
                    state = state,
                    onSelect = viewModel::select,
                    onRetry = viewModel::loadAssets,
                    modifier = Modifier.height(ListSheetHeight),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camera,
                contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                properties = MapProperties(),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
                onMapLoaded = { mapLoaded = true },
                onMapClick = { viewModel.clearSelection() },
                onMapLongClick = { viewModel.startAdd(it.latitude, it.longitude) },
            ) {
                state.assets.forEach { asset ->
                    AssetMarker(asset, isSelected = asset.id == state.selectedId, onClick = { viewModel.select(asset) })
                }
                state.draft?.let { draft ->
                    Marker(
                        state = rememberUpdatedMarkerState(LatLng(draft.latitude, draft.longitude)),
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        title = "New asset",
                        zIndex = 2f,
                    )
                }
            }

            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }

            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapControl(Icons.Outlined.Add, "Zoom in") { scope.launch { camera.animate(CameraUpdateFactory.zoomIn()) } }
                MapControl(Icons.Outlined.Remove, "Zoom out") { scope.launch { camera.animate(CameraUpdateFactory.zoomOut()) } }
                MapControl(Icons.Outlined.CenterFocusStrong, "Show all assets") { fitRequested = true }
            }

            ExtendedFloatingActionButton(
                onClick = { viewModel.startAdd(camera.position.target.latitude, camera.position.target.longitude) },
                icon = { Icon(Icons.Outlined.AddLocationAlt, contentDescription = null) },
                text = { Text("Add asset") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = padding.calculateBottomPadding()),
            )
        }
    }

    state.draft?.let { draft ->
        AddAssetSheet(
            draft = draft,
            onNameChange = viewModel::updateDraftName,
            onTypeChange = viewModel::updateDraftType,
            onStatusChange = viewModel::updateDraftStatus,
            onSubmit = viewModel::submitAdd,
            onDismiss = viewModel::cancelAdd,
        )
    }
}

@Composable
@GoogleMapComposable
private fun AssetMarker(asset: Asset, isSelected: Boolean, onClick: () -> Unit) {
    Marker(
        state = rememberUpdatedMarkerState(LatLng(asset.latitude, asset.longitude)),
        title = asset.name,
        snippet = "${asset.type.name} · ${asset.status.name}",
        icon = markerIcon(asset.status),
        zIndex = if (isSelected) 1f else 0f,
        onClick = {
            onClick()
            true
        },
    )
}

private fun markerIcon(status: AssetStatus): BitmapDescriptor = BitmapDescriptorFactory.defaultMarker(
    if (status == AssetStatus.Active) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_ORANGE,
)

private fun fitUpdate(assets: List<Asset>, paddingPx: Int): CameraUpdate {
    if (assets.size == 1) {
        return CameraUpdateFactory.newLatLngZoom(LatLng(assets[0].latitude, assets[0].longitude), FocusZoom)
    }
    val bounds = LatLngBounds.builder().apply { assets.forEach { include(LatLng(it.latitude, it.longitude)) } }.build()
    return CameraUpdateFactory.newLatLngBounds(bounds, paddingPx)
}

@Composable
private fun TitleWithCount(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Field Assets", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(10.dp))
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(
                "$count",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun MapControl(icon: ImageVector, description: String, onClick: () -> Unit) {
    SmallFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { Icon(icon, contentDescription = description) }
}
