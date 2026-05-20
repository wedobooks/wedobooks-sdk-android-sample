package io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.wedobooks.sdk.models.enums.WdbDownloadState
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.CustomButton

/**
 * Card that wraps the download status + download / delete actions and an
 * adjacent "stop session" button. Used by both checkout-backed audio screens
 * (the SDK headless player and the Media3 player) inside the bottomContent
 * slot of [HeadlessAudioPlayerLayout].
 *
 * @param stopLabel Label for the stop-session button — caller chooses
 *   "Stop audio service" vs "Stop Media3 session" so it's clear which surface
 *   is being torn down.
 */
@Composable
internal fun AudioDownloadCard(
    downloadState: WdbDownloadState?,
    downloadProgress: Float,
    isDownloading: Boolean,
    canDownload: Boolean,
    canDelete: Boolean,
    canStop: Boolean,
    stopLabel: String,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onStop: () -> Unit,
) {
    // Optimistic toggle state for the download / delete buttons. Flipped
    // synchronously on click so the UI reacts immediately, then cleared the
    // moment `downloadState` reflects the new SDK status. Without this the
    // buttons sit on their pre-click label for the ~hundreds of ms it takes
    // bookDownloadsFlow to emit the new state.
    var pendingAction: PendingAction? by remember { mutableStateOf(null) }
    var stateAtClick: WdbDownloadState? by remember { mutableStateOf(null) }
    LaunchedEffect(downloadState) {
        if (pendingAction != null && downloadState != stateAtClick) {
            pendingAction = null
        }
    }

    val isPending = pendingAction != null
    val downloadTitle = when {
        pendingAction == PendingAction.Downloading -> "Starting download…"
        isDownloading -> "Downloading…"
        else -> "Download"
    }
    val deleteTitle = if (pendingAction == PendingAction.Deleting) "Removing…" else "Delete"

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Download",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DownloadStatusRow(state = downloadState, progress = downloadProgress)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CustomButton(
                    modifier = Modifier.weight(1f),
                    title = downloadTitle,
                    enabled = canDownload && !isDownloading && !isPending,
                    onClick = {
                        stateAtClick = downloadState
                        pendingAction = PendingAction.Downloading
                        onDownload()
                    },
                )
                CustomButton(
                    modifier = Modifier.weight(1f),
                    title = deleteTitle,
                    enabled = canDelete && !isPending,
                    color = MaterialTheme.colorScheme.errorContainer,
                    textColor = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = {
                        stateAtClick = downloadState
                        pendingAction = PendingAction.Deleting
                        onDelete()
                    },
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
            )

            CustomButton(
                title = stopLabel,
                enabled = canStop,
                color = MaterialTheme.colorScheme.secondaryContainer,
                textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onStop,
            )
        }
    }
}

private enum class PendingAction { Downloading, Deleting }

@Composable
private fun DownloadStatusRow(
    state: WdbDownloadState?,
    progress: Float,
) {
    val pct = (progress.coerceIn(0f, 1f) * 100).toInt()
    val label = when (state) {
        null, WdbDownloadState.NotStarted -> "Not downloaded"
        WdbDownloadState.Waiting -> "Waiting…"
        WdbDownloadState.Downloading -> "Downloading… $pct%"
        WdbDownloadState.Paused -> "Paused at $pct%"
        WdbDownloadState.Finished -> "Stored on device"
        WdbDownloadState.Cancelled -> "Cancelled"
        WdbDownloadState.Error -> "Failed"
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
        if (state == WdbDownloadState.Downloading || state == WdbDownloadState.Waiting) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
