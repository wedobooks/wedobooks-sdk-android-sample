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
import io.wedobooks.sdk.models.WdbAudioController
import io.wedobooks.sdk.models.WdbPlayerUiState
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.HeadlessAudioPlayerCallbacks
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.HeadlessAudioPlayerLayout
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.HeadlessAudioPlayerUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

private const val STATE_READY = 3
private const val TAG = "HeadlessAudioSampleScreen"
private const val SEEK_INTERVAL_MS = 15_000L

/**
 * Demonstrates playing an audiobook sample through the SDK's headless player.
 *
 * [WeDoBooksSdk.headlessAudioPlayer.loadSample] plays a short MP3 preview on an in-process
 * player (no background service, no stats, no progress) and surfaces it through the same
 * [WdbPlayerUiState] / [WdbAudioController] contract as a full audiobook — so this screen is
 * structurally identical to [HeadlessAudioScreen] and shares its [HeadlessAudioPlayerLayout]
 * shell, just driven by an ISBN instead of a checkout.
 */
@Composable
fun HeadlessAudioSampleScreen(
    isbn: String,
    goBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    var didLoad by remember(isbn) { mutableStateOf(false) }
    var isLoading by remember(isbn) { mutableStateOf(false) }
    var isPlayerReady by remember(isbn) { mutableStateOf(false) }
    var statusMessage by remember(isbn) { mutableStateOf("Not loaded") }
    var isPlayingState by remember(isbn) { mutableStateOf(false) }
    var currentPositionMsState by remember(isbn) { mutableLongStateOf(0L) }
    var totalDurationMsState by remember(isbn) { mutableLongStateOf(0L) }

    val playerUiState by remember { WeDoBooksSdk.headlessAudioPlayer.playerUiState }
        .collectAsState(initial = WdbPlayerUiState())

    val playbackSpeed = playerUiState.playbackSpeed ?: 1f
    val controlsEnabled = didLoad && isPlayerReady && !isLoading

    LaunchedEffect(isbn) {
        isLoading = true
        statusMessage = "Loading sample…"
        isPlayerReady = false
        didLoad = try {
            WeDoBooksSdk.headlessAudioPlayer.loadSample(isbn = isbn, cover = null)
        } catch (e: Exception) {
            statusMessage = "Failed to load sample: ${e.message ?: "unknown error"}"
            false
        }
        statusMessage = when {
            didLoad -> "Sample loaded"
            statusMessage.startsWith("Failed to load sample:") -> statusMessage
            else -> "No sample available"
        }
        isLoading = false
    }

    LaunchedEffect(didLoad, playerUiState) {
        if (didLoad) {
            isPlayingState =
                (playerUiState.isPlaying == true) || (playerUiState.playWhenReady == true)
            if (playerUiState.playbackState == STATE_READY) {
                isPlayerReady = true
                statusMessage = "Ready"
            }
        }
    }

    LaunchedEffect(didLoad) {
        while (didLoad) {
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
                statusMessage = "Waiting for player…"
            }
            delay(500)
        }
    }

    // The sample keeps playing after you leave this screen (no stop-on-dispose), so
    // closing the view doesn't cut off playback.

    fun withController(name: String, block: (controller: WdbAudioController) -> Unit) {
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

    HeadlessAudioPlayerLayout(
        state = HeadlessAudioPlayerUiState(
            screenTitle = "Headless audio sample",
            isbnLabel = "ISBN $isbn",
            statusMessage = statusMessage,
            trackTitle = "Audiobook sample",
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
    )
}
