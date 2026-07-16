package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wedobooks.sdk.models.Checkout
import io.wedobooks.sdk.models.enums.MaterialType
import io.wedobooks.sdk.models.enums.WdbDownloadState
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.AudioDownloadCard
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.HeadlessAudioPlayerCallbacks
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.HeadlessAudioPlayerLayout
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.HeadlessAudioPlayerUiState
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.WdbAudioPlayerScreenViewModel

/**
 * Media3-backed audio player demo. Drives a [androidx.media3.session.MediaController]
 * connected to [io.wedobooks.sdk.library.wedobookssdksampleapp.services.WdbAudioPlayerSessionService]
 * and renders the shared [HeadlessAudioPlayerLayout].
 */
@Composable
fun WdbAudioPlayerScreen(
    checkout: Checkout?,
    goBack: () -> Unit,
) {
    val vm: WdbAudioPlayerScreenViewModel = viewModel()
    val uiState by vm.state.collectAsState()

    LaunchedEffect(checkout?.id) {
        vm.setCheckout(checkout)
        vm.loadPlayer()
    }

    val isAudio = uiState.checkout?.type == MaterialType.Audiobook
    val controlsEnabled = isAudio && uiState.didLoad && uiState.isPlayerReady && !uiState.isLoading
    val isDownloading = uiState.downloadStatus?.state == WdbDownloadState.Downloading ||
        uiState.downloadStatus?.state == WdbDownloadState.Waiting

    val displayedTitle = uiState.checkout?.title?.takeIf { it.isNotBlank() } ?: "Audiobook"
    val isbnLabel = uiState.checkout?.materialId
        ?.let { "ISBN $it" } ?: "No audiobook selected"

    HeadlessAudioPlayerLayout(
        state = HeadlessAudioPlayerUiState(
            screenTitle = "Media3 audio player",
            isbnLabel = isbnLabel,
            statusMessage = uiState.statusMessage,
            trackTitle = displayedTitle,
            isPlaying = uiState.isPlaying,
            positionMs = uiState.currentPositionMs,
            durationMs = uiState.totalDurationMs,
            playbackSpeed = uiState.playbackSpeed,
            controlsEnabled = controlsEnabled,
            playPauseEnabled = controlsEnabled,
        ),
        callbacks = HeadlessAudioPlayerCallbacks(
            onClose = goBack,
            onSeekTo = vm::seekTo,
            onSkipBack = vm::seekBack15,
            onSkipForward = vm::seekForward15,
            onTogglePlayPause = vm::togglePlayPause,
            onSpeedSelected = vm::setPlaybackSpeed,
        ),
    ) {
        AudioDownloadCard(
            downloadState = uiState.downloadStatus?.state,
            downloadProgress = (uiState.downloadStatus?.progress ?: 0.0).toFloat(),
            isDownloading = isDownloading,
            canDownload = uiState.canDownload,
            canDelete = uiState.canDelete,
            canStop = isAudio,
            stopLabel = "Stop Media3 session",
            onDownload = vm::download,
            onDelete = vm::deleteDownload,
            onStop = {
                vm.killPlayer()
                goBack()
            },
        )
    }
}
