package io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.wedobooks.sdk.R
import io.wedobooks.sdk.library.wedobookssdksampleapp.utils.formatMinSec

/**
 * Snapshot of the state a headless-audio screen renders. Wrap whatever
 * backing controller you have (local ExoPlayer or `WeDoBooksSdk.headlessAudioPlayer`)
 * and map it into this surface.
 */
internal data class HeadlessAudioPlayerUiState(
    /** Title shown in the screen's top app bar. */
    val screenTitle: String,
    /** Left side of the status row — typically "ISBN ..." or "No audiobook selected". */
    val isbnLabel: String,
    /** Right side of the status row — short status / error text. */
    val statusMessage: String,
    /** Centered title above the scrub bar — typically the book title. */
    val trackTitle: String,
    val isPlaying: Boolean,
    /** If null, the slider uses the live polling value; otherwise it's the user's drag value. */
    val positionMs: Long,
    val durationMs: Long,
    val playbackSpeed: Float,
    /**
     * Toggles transport, scrub and speed chips together. False when the controller
     * isn't connected yet, the duration is unknown, or the wrong material type is loaded.
     */
    val controlsEnabled: Boolean,
    /**
     * Lets the play/pause button stay tappable a moment earlier than the rest of the
     * transport. False when no player exists at all.
     */
    val playPauseEnabled: Boolean,
)

/**
 * Discrete actions the screen surfaces. Each closure is invoked on the main thread
 * and is expected to forward to the backing controller (suspend if needed).
 */
internal data class HeadlessAudioPlayerCallbacks(
    val onClose: () -> Unit,
    val onSeekTo: (Long) -> Unit,
    val onSkipBack: () -> Unit,
    val onSkipForward: () -> Unit,
    val onTogglePlayPause: () -> Unit,
    val onSpeedSelected: (Float) -> Unit,
)

internal val HEADLESS_SPEED_PRESETS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

/**
 * Shared visual shell for both the sample and checkout-backed headless audio screens.
 *
 * @param bottomContent Optional extra section rendered below the speed chips inside
 * the scrollable column. Used by the checkout-backed screen to add a download +
 * stop-audio-service card; the sample screen leaves it empty.
 */
@Composable
internal fun HeadlessAudioPlayerLayout(
    state: HeadlessAudioPlayerUiState,
    callbacks: HeadlessAudioPlayerCallbacks,
    bottomContent: @Composable ColumnScope.() -> Unit = {},
) {
    val scrollState = rememberScrollState()
    var dragValue by remember { mutableFloatStateOf(-1f) }

    val sliderMax = state.durationMs.coerceAtLeast(1L).toFloat()
    val sliderValue by remember(state.positionMs) {
        derivedStateOf {
            if (dragValue >= 0f) dragValue else state.positionMs.toFloat()
        }
    }
    val displayedPositionMs by remember(state.positionMs) {
        derivedStateOf {
            if (dragValue >= 0f) dragValue.toLong() else state.positionMs
        }
    }

    Column(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        TopBar(title = state.screenTitle, onClose = callbacks.onClose)
        StatusSubheader(left = state.isbnLabel, right = state.statusMessage)
        Spacer(modifier = Modifier.height(8.dp))
        CoverPlaceholder()
        Spacer(modifier = Modifier.height(28.dp))
        TrackHeader(title = state.trackTitle, isPlaying = state.isPlaying)
        Spacer(modifier = Modifier.height(16.dp))
        ScrubBar(
            sliderValue = sliderValue,
            sliderMax = sliderMax,
            displayedPositionMs = displayedPositionMs,
            durationMs = state.durationMs,
            enabled = state.controlsEnabled && state.durationMs > 0L,
            onValueChange = { dragValue = it },
            onValueChangeFinished = {
                if (dragValue >= 0f) {
                    callbacks.onSeekTo(dragValue.toLong())
                    dragValue = -1f
                }
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        TransportRow(
            isPlaying = state.isPlaying,
            controlsEnabled = state.controlsEnabled,
            playPauseEnabled = state.playPauseEnabled,
            onSkipBack = callbacks.onSkipBack,
            onTogglePlayPause = callbacks.onTogglePlayPause,
            onSkipForward = callbacks.onSkipForward,
        )
        Spacer(modifier = Modifier.height(20.dp))
        SpeedChipRow(
            playbackSpeed = state.playbackSpeed,
            enabled = state.controlsEnabled,
            onSpeedSelected = callbacks.onSpeedSelected,
        )

        // Optional caller-provided extras (download card, etc.)
        Spacer(modifier = Modifier.height(28.dp))
        bottomContent()
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ---- Sub-pieces -------------------------------------------------------------

@Composable
private fun TopBar(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(R.drawable.wdb_ic_close),
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = "Close",
            )
        }
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun StatusSubheader(left: String, right: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = left,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        Text(
            text = right,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun CoverPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        ElevatedCard(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(96.dp),
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun TrackHeader(title: String, isPlaying: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = if (isPlaying) "Playing" else "Paused",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun ScrubBar(
    sliderValue: Float,
    sliderMax: Float,
    displayedPositionMs: Long,
    durationMs: Long,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Slider(
            value = sliderValue,
            valueRange = 0f..sliderMax,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = enabled,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatMinSec(displayedPositionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Text(
                text = formatMinSec(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun TransportRow(
    isPlaying: Boolean,
    controlsEnabled: Boolean,
    playPauseEnabled: Boolean,
    onSkipBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipForward: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = onSkipBack,
            enabled = controlsEnabled,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                painter = painterResource(R.drawable.wdb_ic_back_15),
                contentDescription = "Back 15 seconds",
            )
        }

        FilledIconButton(
            onClick = onTogglePlayPause,
            enabled = playPauseEnabled,
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                modifier = Modifier.size(40.dp),
                painter = painterResource(
                    if (isPlaying) R.drawable.wdb_ic_pause else R.drawable.wdb_ic_play
                ),
                contentDescription = if (isPlaying) "Pause" else "Play",
            )
        }

        FilledTonalIconButton(
            onClick = onSkipForward,
            enabled = controlsEnabled,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                painter = painterResource(R.drawable.wdb_ic_forward_15),
                contentDescription = "Forward 15 seconds",
            )
        }
    }
}

@Composable
private fun SpeedChipRow(
    playbackSpeed: Float,
    enabled: Boolean,
    onSpeedSelected: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Speed",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            HEADLESS_SPEED_PRESETS.forEach { speed ->
                val isActive = kotlin.math.abs(playbackSpeed - speed) < 0.01f
                if (isActive) {
                    AssistChip(
                        onClick = { /* already active */ },
                        label = { Text(formatSpeed(speed)) },
                        enabled = enabled,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        border = null,
                    )
                } else {
                    SuggestionChip(
                        onClick = { onSpeedSelected(speed) },
                        label = { Text(formatSpeed(speed)) },
                        enabled = enabled,
                        colors = SuggestionChipDefaults.suggestionChipColors(),
                    )
                }
            }
        }
    }
}

/** `1.0f` → `"1×"`, `1.25f` → `"1.25×"`. Locale-independent. */
private fun formatSpeed(speed: Float): String {
    val rounded = (speed * 100).toInt() / 100f
    return if (rounded == rounded.toInt().toFloat()) {
        "${rounded.toInt()}×"
    } else {
        "${"%.2f".format(rounded).trimEnd('0').trimEnd('.')}×"
    }
}
