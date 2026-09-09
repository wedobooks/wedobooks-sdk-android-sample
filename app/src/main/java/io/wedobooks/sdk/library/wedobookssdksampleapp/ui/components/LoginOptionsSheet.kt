package io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.wedobooks.sdk.library.wedobookssdksampleapp.environment.SampleEnvironment
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.CustomButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginOptionsSheet(
    environments: List<SampleEnvironment>,
    currentEnvironmentId: String,
    showEnvironments: Boolean,
    rememberedUids: List<String>,
    onSelectEnvironment: (String) -> Unit,
    onSelectUid: (String) -> Unit,
    onForgetUid: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var newUid by remember { mutableStateOf("") }
    var pendingEnvironmentId by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showEnvironments) {
                SheetSectionHeader("Environment")
                Text(
                    text = "Switching environment signs you out and restarts the app.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
                environments.forEach { environment ->
                    val isCurrent = environment.id == currentEnvironmentId
                    CustomButton(
                        title = environment.label,
                        isSelected = isCurrent,
                        selectedTitle = "${environment.label} (current)",
                        onClick = {
                            if (!isCurrent) pendingEnvironmentId = environment.id
                        },
                    )
                }
            }

            SheetSectionHeader("Account")

            if (rememberedUids.isEmpty()) {
                Text(
                    text = "No accounts used in this environment yet.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            rememberedUids.forEach { uid ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectUid(uid) }
                            .padding(vertical = 12.dp),
                        text = uid,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = { onForgetUid(uid) }) {
                        Text("✕")
                    }
                }
            }

            OutlinedTextField(
                value = newUid,
                onValueChange = { newUid = it },
                label = { Text("New user ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            CustomButton(
                title = "Sign in with this ID",
                enabled = newUid.isNotBlank(),
                onClick = { onSelectUid(newUid.trim()) },
            )
        }
    }

    pendingEnvironmentId?.let { targetId ->
        val target = environments.first { it.id == targetId }
        AlertDialog(
            onDismissRequest = { pendingEnvironmentId = null },
            title = { Text("Switch to ${target.label}?") },
            text = {
                Text(
                    "The app will sign out and restart. The SDK can only be " +
                        "configured once per process, so this cannot be done live."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingEnvironmentId = null
                    onSelectEnvironment(targetId)
                }) { Text("Restart") }
            },
            dismissButton = {
                TextButton(onClick = { pendingEnvironmentId = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SheetSectionHeader(title: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        text = title.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
    )
}
