package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.HeadlessAudioPlayerCallbacks
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.HeadlessAudioPlayerLayout
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.HeadlessAudioPlayerUiState
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.WdbAudioPlayerScreenViewModel

/**
 * Plays an audiobook sample on the SAME shared player as the full book: it drives a
 * [androidx.media3.session.MediaController] connected to
 * [io.wedobooks.sdk.library.wedobookssdksampleapp.services.WdbAudioPlayerSessionService] (through
 * [WdbAudioPlayerScreenViewModel]) and sends a load-sample command, so starting a sample
 * replaces whatever the service was playing instead of spinning up a second player.
 */
@Composable
fun WdbAudioPlayerSampleScreen(
    isbn: String,
    goBack: () -> Unit,
) {
    val vm: WdbAudioPlayerScreenViewModel = viewModel()
    val uiState by vm.state.collectAsState()

    LaunchedEffect(isbn) {
        vm.loadSample(isbn)
    }

    val controlsEnabled = uiState.didLoad && uiState.isPlayerReady && !uiState.isLoading

    HeadlessAudioPlayerLayout(
        state = HeadlessAudioPlayerUiState(
            screenTitle = "WdbAudioPlayer sample",
            isbnLabel = "ISBN $isbn",
            statusMessage = uiState.statusMessage,
            trackTitle = "Audiobook sample",
            isPlaying = uiState.isPlaying,
            positionMs = uiState.currentPositionMs,
            durationMs = uiState.totalDurationMs,
            playbackSpeed = uiState.playbackSpeed,
            controlsEnabled = controlsEnabled,
            playPauseEnabled = uiState.isPlayerReady,
        ),
        callbacks = HeadlessAudioPlayerCallbacks(
            onClose = goBack,
            onSeekTo = vm::seekTo,
            onSkipBack = vm::seekBack15,
            onSkipForward = vm::seekForward15,
            onTogglePlayPause = vm::togglePlayPause,
            onSpeedSelected = vm::setPlaybackSpeed,
        ),
    )
}
