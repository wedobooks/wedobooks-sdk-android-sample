package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wedobooks.sdk.library.wedobookssdksampleapp.R
import io.wedobooks.sdk.library.wedobookssdksampleapp.ui.components.PagerDots
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.HistoryPagerViewModel
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.HistoryUiState
import io.wedobooks.sdk.models.HistoryItem
import io.wedobooks.sdk.models.enums.HistoryStatus
import io.wedobooks.sdk.models.enums.MaterialType

/**
 * A horizontal pager of the signed-in user's reading/listening history — one
 * [HistoryItem] per page — intended to sit beneath the stats pager in the Stats tab.
 *
 * Loads the first page on first composition, then prefetches the next page as the
 * user swipes toward the end of the loaded items.
 */
@Composable
fun HistoryPager(
    modifier: Modifier = Modifier,
) {
    val vm: HistoryPagerViewModel = viewModel(factory = HistoryPagerViewModel.factory())
    val uiState by vm.uiState.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(vm) {
        vm.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            text = "History",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        when (val state = uiState) {
            HistoryUiState.Loading -> HistoryMessageBox {
                Spinner(modifier = Modifier)
            }

            HistoryUiState.Empty -> HistoryMessageBox {
                Text(
                    text = "No history yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            is HistoryUiState.Error -> HistoryMessageBox {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = state.message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            is HistoryUiState.Loaded -> HistoryLoaded(
                state = state,
                onPageChanged = vm::prefetchAround,
                onMarkCompleted = vm::markCompleted,
                onRemove = vm::remove,
            )
        }
    }
}

@Composable
private fun HistoryLoaded(
    state: HistoryUiState.Loaded,
    onPageChanged: (Int) -> Unit,
    onMarkCompleted: (HistoryItem) -> Unit,
    onRemove: (HistoryItem) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { state.items.size })

    // Drive prefetch from the settled page; snapshotFlow dedups repeated values.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect(onPageChanged)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(state = pagerState) { page ->
            val item = state.items[page]
            HistoryItemCard(
                item = item,
                isPending = item.materialId in state.pendingIds,
                onMarkCompleted = { onMarkCompleted(item) },
                onRemove = { onRemove(item) },
            )
        }
        PagerDots(pagerState = pagerState)
    }
}

@Composable
private fun HistoryMessageBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun HistoryItemCard(
    item: HistoryItem,
    isPending: Boolean,
    onMarkCompleted: () -> Unit,
    onRemove: () -> Unit,
) {
    val isCompleted = item.status == HistoryStatus.Completed || item.progress >= 1.0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Title + remove action.
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = item.title.ifBlank { "Untitled" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(
                            onClick = onRemove,
                            enabled = !isPending,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_trashcan),
                                contentDescription = "Remove from history",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                    if (item.author.isNotEmpty()) {
                        Text(
                            text = item.author.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HistoryRow(
                        label = { Text("ISBN", style = MaterialTheme.typography.bodyLarge) },
                        value = item.materialId,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        HistoryRow(
                            label = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        modifier = Modifier.size(44.dp),
                                        painter = when (item.type) {
                                            MaterialType.Ebook -> painterResource(R.drawable.ic_book)
                                            MaterialType.Audiobook -> painterResource(R.drawable.ic_audiobook)
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text("Progress", style = MaterialTheme.typography.bodyLarge)
                                }
                            },
                            value = "${(item.progress * 100).toInt()}%",
                        )
                        LinearProgressIndicator(
                            progress = { item.progress.toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        )
                    }
                }

                CustomButton(
                    title = if (isCompleted) "Completed" else "Mark as complete",
                    enabled = !isCompleted && !isPending,
                    onClick = onMarkCompleted,
                )
            }
        }
    }
}

/**
 * A label-on-the-left, value-on-the-right row, matching the layout used by the
 * stats cards. [label] is a slot so callers can prefix it with an icon.
 */
@Composable
private fun HistoryRow(
    label: @Composable () -> Unit,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        label()
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
