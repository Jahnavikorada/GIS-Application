package com.fieldassets.presentation.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fieldassets.domain.model.Asset
import com.fieldassets.domain.model.AssetStatus
import com.fieldassets.presentation.components.AssetTypeBadge
import com.fieldassets.presentation.components.EnumSegmentedRow
import com.fieldassets.presentation.components.LabeledValue
import com.fieldassets.presentation.components.StatusChip
import java.util.Locale

@Composable
fun AssetDetailPanel(
    asset: Asset,
    pendingStatus: AssetStatus?,
    saving: Boolean,
    onChooseStatus: (AssetStatus) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetTypeBadge(asset.type, size = 48)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(asset.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "ID ${asset.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(asset.status)
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "Close details") }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledValue("Type", asset.type.name)
            LabeledValue("Latitude", formatCoordinate(asset.latitude))
            LabeledValue("Longitude", formatCoordinate(asset.longitude))
        }

        Spacer(Modifier.height(16.dp))
        Text("Update status", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        EnumSegmentedRow(
            options = AssetStatus.entries,
            selected = pendingStatus ?: asset.status,
            onSelect = onChooseStatus,
            enabled = !saving,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onSave,
            enabled = pendingStatus != null && !saving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.size(8.dp))
                Text("Saving…")
            } else {
                Text("Save status")
            }
        }
    }
}

fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.5f", value)
