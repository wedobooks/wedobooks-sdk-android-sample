package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.WDBAudioPlayerScreenViewModel
import io.wedobooks.sdk.models.Checkout
import io.wedobooks.sdk.models.enums.MaterialType

@Composable
fun WDBAudioPlayerScreen(
    checkout: Checkout?,
    goBack: () -> Unit,
) {
    val vm: WDBAudioPlayerScreenViewModel = viewModel()
    val uiState by vm.state.collectAsState()

    LaunchedEffect(checkout?.id) {
        vm.setCheckout(checkout)
        vm.loadPlayer()
    }

    DisposableEffect(Unit) {
        onDispose {
            vm.killPlayer()
        }
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
                Text(text = uiState.statusMessage, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    text = if (uiState.isPlaying) "Playing" else "Paused",
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${formatMinSec(uiState.currentPositionMs)} / ${
                        formatMinSec(
                            uiState.totalDurationMs
                        )
                    }",
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Download: ${
                        uiState.downloadStatus?.let { "${it.state} (${(it.progress * 100).toInt()}%)" } ?: "NotStarted"
                    }",
                    color = MaterialTheme.colorScheme.onBackground
                )

                CustomButton(
                    title = "Play / Pause",
                    enabled = uiState.checkout?.type == MaterialType.Audiobook &&
                        uiState.didLoad &&
                        uiState.isPlayerReady &&
                        !uiState.isLoading,
                    onClick = vm::togglePlayPause
                )

                CustomButton(
                    title = "+15 sec",
                    enabled = uiState.checkout?.type == MaterialType.Audiobook &&
                        uiState.didLoad &&
                        uiState.isPlayerReady &&
                        !uiState.isLoading,
                    onClick = vm::seekForward15
                )

                CustomButton(
                    title = "Kill",
                    enabled = uiState.checkout?.type == MaterialType.Audiobook,
                    onClick = {
                        vm.killPlayer()
                        goBack()
                    }
                )

                CustomButton(
                    title = "Download",
                    enabled = uiState.canDownload,
                    onClick = vm::download
                )

                CustomButton(
                    title = "Delete Download",
                    enabled = uiState.canDelete,
                    onClick = vm::deleteDownload
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
    val seconds = (positionMs / 1000L).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}
