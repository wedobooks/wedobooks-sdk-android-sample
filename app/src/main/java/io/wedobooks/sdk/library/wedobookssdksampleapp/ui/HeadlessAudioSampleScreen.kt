package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.HeadlessAudioPlayerCallbacks
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.HeadlessAudioPlayerLayout
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.HeadlessAudioPlayerUiState
import kotlinx.coroutines.delay

private const val TAG = "HeadlessAudioSampleScreen"
private const val SEEK_INTERVAL_MS = 15_000L

/**
 * Demonstrates a "headless" approach to playing an audiobook sample:
 * the SDK resolves the sample URL and the sample app drives a basic
 * [ExoPlayer] with its own UI.
 *
 * Shares its visual shell with [HeadlessAudioScreen] via
 * [HeadlessAudioPlayerLayout].
 */
@OptIn(UnstableApi::class)
@Composable
fun HeadlessAudioSampleScreen(
    isbn: String,
    goBack: () -> Unit,
) {
    val context = LocalContext.current

    var sampleUrl by remember(isbn) { mutableStateOf<String?>(null) }
    var statusMessage by remember(isbn) { mutableStateOf("Resolving sample URL…") }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isbn) {
        try {
            sampleUrl = WeDoBooksSdk.bookOperations.getAudiobookSampleUrl(isbn)
            statusMessage = "Sample URL resolved, preparing player…"
        } catch (e: Throwable) {
            Log.e(TAG, "getAudiobookSampleUrl failed", e)
            statusMessage = "Failed to resolve sample URL: ${e.message ?: "unknown"}"
        }
    }

    val player = remember(sampleUrl) {
        val url = sampleUrl ?: return@remember null
        ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = false
            }
    }

    DisposableEffect(player) {
        val listener = player?.let { p ->
            object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "sample playback error", error)
                    statusMessage = "Playback error: ${error.message ?: "unknown"}"
                }

                override fun onPlaybackParametersChanged(parameters: PlaybackParameters) {
                    playbackSpeed = parameters.speed
                }

                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_READY -> {
                            durationMs = p.duration.coerceAtLeast(0L)
                            statusMessage = "Ready"
                        }
                        Player.STATE_BUFFERING -> statusMessage = "Buffering…"
                        Player.STATE_ENDED -> statusMessage = "Sample ended"
                        Player.STATE_IDLE -> Unit
                    }
                }
            }.also { p.addListener(it) }
        }
        onDispose {
            listener?.let { player.removeListener(it) }
            player?.release()
        }
    }

    LaunchedEffect(player) {
        while (player != null) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            delay(500)
        }
    }

    HeadlessAudioPlayerLayout(
        state = HeadlessAudioPlayerUiState(
            screenTitle = "Headless audio sample",
            isbnLabel = "ISBN $isbn",
            statusMessage = statusMessage,
            trackTitle = "Audiobook sample",
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            playbackSpeed = playbackSpeed,
            controlsEnabled = player != null && durationMs > 0L,
            playPauseEnabled = player != null,
        ),
        callbacks = HeadlessAudioPlayerCallbacks(
            onClose = goBack,
            onSeekTo = { target -> player?.seekTo(target) },
            onSkipBack = {
                player?.let {
                    val target = (it.currentPosition - SEEK_INTERVAL_MS).coerceAtLeast(0L)
                    it.seekTo(target)
                }
            },
            onSkipForward = {
                player?.let {
                    val d = it.duration.coerceAtLeast(0L)
                    val candidate = it.currentPosition + SEEK_INTERVAL_MS
                    val target = if (d > 0L) candidate.coerceAtMost(d) else candidate
                    it.seekTo(target)
                }
            },
            onTogglePlayPause = {
                player?.let { if (it.isPlaying) it.pause() else it.play() }
            },
            onSpeedSelected = { speed -> player?.setPlaybackSpeed(speed) },
        ),
    )
}
