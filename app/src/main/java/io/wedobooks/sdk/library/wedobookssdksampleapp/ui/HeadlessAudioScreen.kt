package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.models.CheckoutBook
import io.wedobooks.sdk.models.WdbPlayerUiState
import io.wedobooks.sdk.models.enums.MaterialType
import io.wedobooks.sdk.models.enums.WdbDownloadState
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val STATE_READY = 3
private const val TAG = "HeadlessAudioScreen"

@Composable
fun HeadlessAudioScreen(
    checkout: CheckoutBook?,
    goBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var didLoad by remember {
        mutableStateOf(false)
    }
    var isLoading by remember {
        mutableStateOf(false)
    }
    var isPlayerReady by remember {
        mutableStateOf(false)
    }
    var statusMessage by remember {
        mutableStateOf("Not loaded")
    }
    var isPlayingState by remember {
        mutableStateOf(false)
    }
    var currentPositionMsState by remember {
        mutableStateOf(0L)
    }
    var totalDurationMsState by remember {
        mutableStateOf(0L)
    }
    val playerUiState by remember { WeDoBooksSdk.bookOperations.playerUiState }
        .collectAsState(initial = WdbPlayerUiState())
    val audioDownloads by remember { WeDoBooksSdk.storageOperations.audioDownloadsFlow }
        .collectAsState(initial = emptyMap())
    val downloadStatus = checkout?.materialId?.let { audioDownloads[it] }
    val canDownload = checkout?.type == MaterialType.Audiobook && (
        downloadStatus == null ||
            downloadStatus.state == WdbDownloadState.NotStarted ||
            downloadStatus.state == WdbDownloadState.Cancelled ||
            downloadStatus.state == WdbDownloadState.Error
        )
    val canDelete = checkout?.type == MaterialType.Audiobook &&
        downloadStatus != null &&
        downloadStatus.state != WdbDownloadState.NotStarted
    var previousDownloadState by remember(checkout?.id) {
        mutableStateOf<WdbDownloadState?>(null)
    }

    LaunchedEffect(checkout?.id) {
        if (checkout?.type == MaterialType.Audiobook) {
            isLoading = true
            statusMessage = "Loading headless player..."
            isPlayerReady = false
            didLoad = try {
                WeDoBooksSdk.bookOperations.loadAudioBook(
                    checkout = checkout,
                    coverUrl = null, // you can replace this with your own coverUrl
                    initialProgressMs = null
                )
            } catch (e: Exception) {
                statusMessage = "Failed to load player: ${e.message ?: "unknown error"}"
                false
            }
            statusMessage = if (didLoad) {
                "Controller connected, waiting for STATE_READY..."
            } else {
                if (statusMessage.startsWith("Failed to load player:")) {
                    statusMessage
                } else {
                    "Failed to load player"
                }
            }
            isLoading = false
        } else {
            statusMessage = "Select an audiobook first"
            didLoad = false
            isPlayerReady = false
        }
    }

    LaunchedEffect(didLoad, checkout?.id, playerUiState) {
        if (didLoad && checkout?.type == MaterialType.Audiobook) {
            isPlayingState = (playerUiState.isPlaying == true) || (playerUiState.playWhenReady == true)
            if (playerUiState.playbackState == STATE_READY) {
                isPlayerReady = true
                statusMessage = "Player ready"
            }
        }
    }

    LaunchedEffect(didLoad, checkout?.id) {
        while (didLoad && checkout?.type == MaterialType.Audiobook) {
            val updated = try {
                WeDoBooksSdk.bookOperations.withAudioController {
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
                statusMessage = "Waiting for controller..."
            }
            delay(500)
        }
    }

    LaunchedEffect(downloadStatus?.state, checkout?.id) {
        val currentState = downloadStatus?.state
        if (
            checkout?.type == MaterialType.Audiobook &&
            previousDownloadState != WdbDownloadState.Finished &&
            currentState == WdbDownloadState.Finished
        ) {
            try {
                WeDoBooksSdk.bookOperations.restartPlayerFromCurrentPosition()
                statusMessage = "Download finished, switched to local source"
            } catch (e: Exception) {
                Log.d(TAG, "restartPlayerFromCurrentPosition failed: ${e.message}", e)
            }
        }
        previousDownloadState = currentState
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = statusMessage, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    text = if (isPlayingState) "Playing" else "Paused",
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${formatMinSec(currentPositionMsState)} / ${formatMinSec(totalDurationMsState)}",
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Download: ${
                        downloadStatus?.let { "${it.state} (${(it.progress * 100).toInt()}%)" } ?: "NotStarted"
                    }",
                    color = MaterialTheme.colorScheme.onBackground
                )

                CustomButton(
                    title = "Play / Pause",
                    enabled = checkout?.type == MaterialType.Audiobook && didLoad && isPlayerReady && !isLoading,
                    onClick = {
                        coroutineScope.launch {
                            val toggled = try {
                                WeDoBooksSdk.bookOperations.withAudioController {
                                    if (it.isPlaying) {
                                        it.pause()
                                    } else {
                                        it.play()
                                    }
                                    currentPositionMsState = it.currentPositionMs
                                    totalDurationMsState = it.totalDurationMs
                                    true
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "withAudioController failed on play/pause: ${e.message}", e)
                                false
                            }
                            if (!toggled) {
                                statusMessage = "Controller unavailable. Retrying..."
                                isPlayerReady = false
                            }
                        }
                    }
                )

                CustomButton(
                    title = "+15 sec",
                    enabled = checkout?.type == MaterialType.Audiobook && didLoad && isPlayerReady && !isLoading,
                    onClick = {
                        coroutineScope.launch {
                            val sought = try {
                                WeDoBooksSdk.bookOperations.withAudioController {
                                    val target = max(0L, it.currentPositionMs + 15_000L)
                                    it.seekTo(target)
                                    currentPositionMsState = it.currentPositionMs
                                    totalDurationMsState = it.totalDurationMs
                                    true
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "withAudioController failed on seek: ${e.message}", e)
                                false
                            }
                            if (!sought) {
                                statusMessage = "Controller unavailable. Retrying..."
                                isPlayerReady = false
                            }
                        }
                    }
                )

                CustomButton(
                    title = "Kill",
                    enabled = checkout?.type == MaterialType.Audiobook,
                    onClick = {
                        WeDoBooksSdk.bookOperations.stopAudioPlayer()
                        goBack()
                    }
                )

                CustomButton(
                    title = "Download",
                    enabled = canDownload,
                    onClick = {
                        checkout?.let { selectedCheckout ->
                            coroutineScope.launch {
                                val started = try {
                                    WeDoBooksSdk.storageOperations.downloadAudioBook(selectedCheckout)
                                } catch (e: Exception) {
                                    Log.d(TAG, "downloadAudioBook failed: ${e.message}", e)
                                    false
                                }
                                statusMessage = if (started) {
                                    "Download started"
                                } else {
                                    "Download failed"
                                }
                            }
                        }
                    }
                )

                CustomButton(
                    title = "Delete Download",
                    enabled = canDelete,
                    onClick = {
                        checkout?.let { selectedCheckout ->
                            coroutineScope.launch {
                                val removed = try {
                                    WeDoBooksSdk.storageOperations.removeAudioBookDownload(selectedCheckout)
                                } catch (e: Exception) {
                                    Log.d(TAG, "removeAudioBookDownload failed: ${e.message}", e)
                                    false
                                }
                                statusMessage = if (removed) {
                                    "Download removed"
                                } else {
                                    "Remove failed"
                                }
                            }
                        }
                    }
                )

                CustomButton(
                    title = "Go Back",
                    onClick = goBack
                )
            }
        }
    }
}

private fun formatMinSec(positionMs: Long): String {
    val totalSeconds = (positionMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
