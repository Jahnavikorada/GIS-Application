package com.fieldassets.presentation.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.fieldassets.domain.model.AssetStatus
import com.fieldassets.domain.model.AssetType
import com.fieldassets.presentation.components.EnumSegmentedRow
import com.fieldassets.presentation.map.AddAssetDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAssetSheet(
    draft: AddAssetDraft,
    onNameChange: (String) -> Unit,
    onTypeChange: (AssetType) -> Unit,
    onStatusChange: (AssetStatus) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { if (!draft.submitting) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text("Add asset", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "  ${formatCoordinate(draft.latitude)}, ${formatCoordinate(draft.longitude)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = draft.name,
                onValueChange = onNameChange,
                label = { Text("Asset name") },
                singleLine = true,
                enabled = !draft.submitting,
                isError = draft.fieldErrors.containsKey("name"),
                supportingText = draft.fieldErrors["name"]?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            LabeledGroup("Asset type", draft.fieldErrors["asset_type"]) {
                EnumSegmentedRow(AssetType.entries, draft.type, onTypeChange, enabled = !draft.submitting)
            }
            LabeledGroup("Initial status", draft.fieldErrors["status"]) {
                EnumSegmentedRow(AssetStatus.entries, draft.status, onStatusChange, enabled = !draft.submitting)
            }

            val coordinateError = draft.fieldErrors["latitude"] ?: draft.fieldErrors["longitude"]
            val generalError = coordinateError ?: draft.error.takeIf { draft.fieldErrors.isEmpty() }
            if (generalError != null) {
                Text(generalError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, enabled = !draft.submitting, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(onClick = onSubmit, enabled = !draft.submitting, modifier = Modifier.weight(1f)) {
                    if (draft.submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Add asset")
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledGroup(label: String, error: String?, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
