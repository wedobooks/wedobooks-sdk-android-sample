package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.models.Checkout
import io.wedobooks.sdk.models.WdbPlayerUiState
import io.wedobooks.sdk.models.enums.MaterialType
import io.wedobooks.sdk.models.enums.WdbDownloadState
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.AudioDownloadCard
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.HeadlessAudioPlayerCallbacks
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.HeadlessAudioPlayerLayout
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.HeadlessAudioPlayerUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

private const val STATE_READY = 3
private const val TAG = "HeadlessAudioScreen"
private const val SEEK_INTERVAL_MS = 15_000L

/**
 * Drives [WeDoBooksSdk.headlessAudioPlayer] from a checked-out audiobook and renders
 * the shared [HeadlessAudioPlayerLayout]. Adds a checkout-specific download +
 * stop-audio-service card under the transport row.
 */
@Composable
fun HeadlessAudioScreen(
    checkout: Checkout?,
    goBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    var activeCheckout by remember { mutableStateOf(checkout) }
    var didLoad by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isPlayerReady by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Not loaded") }
    var isPlayingState by remember { mutableStateOf(false) }
    var currentPositionMsState by remember { mutableLongStateOf(0L) }
    var totalDurationMsState by remember { mutableLongStateOf(0L) }

    val playerUiState by remember { WeDoBooksSdk.headlessAudioPlayer.playerUiState }
        .collectAsState(initial = WdbPlayerUiState())
    val audioDownloads by remember { WeDoBooksSdk.storageOperations.bookDownloadsFlow }
        .collectAsState(initial = emptyMap())
    val downloadStatus = activeCheckout?.materialId?.let { audioDownloads[it] }

    val canDownload = activeCheckout?.type == MaterialType.Audiobook && (
        downloadStatus == null ||
            downloadStatus.state == WdbDownloadState.NotStarted ||
            downloadStatus.state == WdbDownloadState.Cancelled ||
            downloadStatus.state == WdbDownloadState.Error
        )
    val canDelete = activeCheckout?.type == MaterialType.Audiobook &&
        downloadStatus != null &&
        downloadStatus.state != WdbDownloadState.NotStarted
    val isDownloading = downloadStatus?.state == WdbDownloadState.Downloading ||
        downloadStatus?.state == WdbDownloadState.Waiting

    val playbackSpeed = playerUiState.playbackSpeed ?: 1f
    val controlsEnabled = didLoad && isPlayerReady && !isLoading &&
        activeCheckout?.type == MaterialType.Audiobook

    var previousDownloadState by remember(activeCheckout?.materialId) {
        mutableStateOf<WdbDownloadState?>(null)
    }

    // ---- Effects ---------------------------------------------------------
    LaunchedEffect(checkout?.id) {
        activeCheckout = checkout
    }

    LaunchedEffect(activeCheckout?.materialId) {
        val selectedCheckout = activeCheckout
        if (selectedCheckout?.type == MaterialType.Audiobook) {
            isLoading = true
            statusMessage = "Loading…"
            isPlayerReady = false
            didLoad = try {
                WeDoBooksSdk.headlessAudioPlayer.loadAudioBook(
                    checkout = selectedCheckout,
                    cover = null,
                )
                true
            } catch (e: Exception) {
                statusMessage = "Failed to load: ${e.message ?: "unknown error"}"
                false
            }
            statusMessage = if (didLoad) {
                "Controller connected"
            } else if (statusMessage.startsWith("Failed to load:")) {
                statusMessage
            } else {
                "Failed to load player"
            }
            isLoading = false
        } else {
            statusMessage = "Select an audiobook first"
            didLoad = false
            isPlayerReady = false
        }
    }

    LaunchedEffect(didLoad, activeCheckout?.materialId, playerUiState) {
        if (didLoad && activeCheckout?.type == MaterialType.Audiobook) {
            isPlayingState =
                (playerUiState.isPlaying == true) || (playerUiState.playWhenReady == true)
            if (playerUiState.playbackState == STATE_READY) {
                isPlayerReady = true
                statusMessage = "Ready"
            }
        }
    }

    LaunchedEffect(didLoad, activeCheckout?.materialId) {
        while (didLoad && activeCheckout?.type == MaterialType.Audiobook) {
            val updated = try {
                WeDoBooksSdk.headlessAudioPlayer.withAudioController {
                    currentPositionMsState = it.currentPositionMs
                    totalDurationMsState = it.totalDurationMs
                    true
                }
            } catch (e: Exception) {
                Log.d(TAG, "withAudioController failed in polling loop: ${e.message}", e)
                false
            }
            if (!updated) {
                isPlayerReady = false
                statusMessage = "Waiting for controller…"
            }
            delay(500)
        }
    }

    LaunchedEffect(downloadStatus?.state, activeCheckout?.materialId) {
        val currentState = downloadStatus?.state
        if (
            activeCheckout?.type == MaterialType.Audiobook &&
            previousDownloadState != WdbDownloadState.Finished &&
            currentState == WdbDownloadState.Finished
        ) {
            try {
                WeDoBooksSdk.headlessAudioPlayer.restartPlayerFromCurrentPosition()
                statusMessage = "Switched to local source"
            } catch (e: Exception) {
                Log.d(TAG, "restartPlayerFromCurrentPosition failed: ${e.message}", e)
            }
        }
        previousDownloadState = currentState
    }

    // ---- Controller-bound callbacks --------------------------------------
    fun withController(name: String, block: (controller: io.wedobooks.sdk.models.WdbAudioController) -> Unit) {
        coroutineScope.launch {
            runCatching {
                WeDoBooksSdk.headlessAudioPlayer.withAudioController {
                    block(it)
                    currentPositionMsState = it.currentPositionMs
                    totalDurationMsState = it.totalDurationMs
                    true
                }
            }.onFailure { e ->
                Log.d(TAG, "$name failed: ${e.message}", e)
            }
        }
    }

    val displayedTitle = activeCheckout?.title?.takeIf { it.isNotBlank() } ?: "Audiobook"
    val isbnLabel = activeCheckout?.materialId?.let { "ISBN $it" } ?: "No audiobook selected"

    HeadlessAudioPlayerLayout(
        state = HeadlessAudioPlayerUiState(
            screenTitle = "Headless audio player",
            isbnLabel = isbnLabel,
            statusMessage = statusMessage,
            trackTitle = displayedTitle,
            isPlaying = isPlayingState,
            positionMs = currentPositionMsState,
            durationMs = totalDurationMsState,
            playbackSpeed = playbackSpeed,
            controlsEnabled = controlsEnabled,
            playPauseEnabled = controlsEnabled,
        ),
        callbacks = HeadlessAudioPlayerCallbacks(
            onClose = goBack,
            onSeekTo = { target -> withController("seek") { it.seekTo(target) } },
            onSkipBack = {
                withController("skip-back") {
                    it.seekTo(max(0L, it.currentPositionMs - SEEK_INTERVAL_MS))
                }
            },
            onSkipForward = {
                withController("skip-forward") {
                    val d = it.totalDurationMs
                    val candidate = it.currentPositionMs + SEEK_INTERVAL_MS
                    it.seekTo(if (d > 0L) candidate.coerceAtMost(d) else candidate)
                }
            },
            onTogglePlayPause = {
                withController("play/pause") {
                    if (it.isPlaying) it.pause() else it.play()
                }
            },
            onSpeedSelected = { speed ->
                withController("setPlaybackSpeed") { it.setPlaybackSpeed(speed) }
            },
        ),
    ) {
        AudioDownloadCard(
            downloadState = downloadStatus?.state,
            downloadProgress = (downloadStatus?.progress ?: 0.0).toFloat(),
            isDownloading = isDownloading,
            canDownload = canDownload,
            canDelete = canDelete,
            canStop = activeCheckout?.type == MaterialType.Audiobook,
            stopLabel = "Stop audio service",
            onDownload = {
                activeCheckout?.let { selectedCheckout ->
                    coroutineScope.launch {
                        val started = runCatching {
                            WeDoBooksSdk.storageOperations.downloadBook(selectedCheckout)
                        }.onFailure { e ->
                            Log.d(TAG, "downloadBook failed: ${e.message}", e)
                        }.isSuccess
                        statusMessage = if (started) "Download started" else "Download failed"
                    }
                }
            },
            onDelete = {
                activeCheckout?.let { selectedCheckout ->
                    coroutineScope.launch {
                        val removed = try {
                            WeDoBooksSdk.storageOperations.removeBook(selectedCheckout.materialId)
                        } catch (e: Exception) {
                            Log.d(TAG, "removeBook failed: ${e.message}", e)
                            false
                        }
                        statusMessage = if (removed) "Download removed" else "Remove failed"
                    }
                }
            },
            onStop = {
                WeDoBooksSdk.bookOperations.stopAudioPlayer()
                goBack()
            },
        )
    }
}

