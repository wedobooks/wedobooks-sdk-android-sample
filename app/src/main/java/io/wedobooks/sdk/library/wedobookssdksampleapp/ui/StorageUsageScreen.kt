package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wedobooks.sdk.R
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.StorageUsageViewModel
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.StorageUsageViewModel.BookEntry
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.StorageUsageViewModel.Breakdown

@Composable
fun StorageUsageScreen(
    goBack: () -> Unit,
) {
    val viewModel: StorageUsageViewModel = viewModel()
    val breakdown: Breakdown? by viewModel.breakdown.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error: String? by viewModel.error.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = goBack) {
                Icon(
                    painter = painterResource(R.drawable.wdb_ic_close),
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = "Close",
                )
            }
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                text = "Storage usage",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            TextButton(onClick = { viewModel.refresh() }, enabled = !isLoading) {
                Text(if (isLoading) "Loading…" else "Refresh")
            }
        }

        val current = breakdown
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (isLoading) "Reading storage…" else "No storage data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "totals") { TotalsCard(current) }
            item(key = "actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.clearCache() },
                        enabled = !isLoading && current.usage.cachedBytes > 0,
                    ) {
                        Text("Clear stream cache")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.removeAll() },
                        enabled = !isLoading && current.usage.totalBytes > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text("Delete everything")
                    }
                }
            }

            if (current.books.isEmpty() &&
                current.otherUsersDownloadedBytes == 0L &&
                current.unattributedCachedBytes == 0L
            ) {
                item(key = "empty") {
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = "The SDK holds nothing on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    )
                }
            }

            items(count = current.books.size, key = { "book-" + current.books[it].materialId }) {
                BookCard(
                    entry = current.books[it],
                    onRemoveDownload = viewModel::removeDownload,
                    onClearCache = viewModel::clearCache,
                )
            }

            if (current.unattributedCachedBytes > 0) {
                item(key = "cache-other") {
                    OtherCard(
                        title = "Stream cache",
                        subtitle = "Audio not tied to a current checkout",
                        bytes = current.unattributedCachedBytes,
                        onRemove = viewModel::clearOrphanedCache,
                    )
                }
            }
            if (current.otherUsersDownloadedBytes > 0) {
                item(key = "downloads-other-users") {
                    OtherCard(
                        title = "Other users' downloads",
                        subtitle = "Downloaded by other accounts on this device",
                        bytes = current.otherUsersDownloadedBytes,
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalsCard(breakdown: Breakdown) {
    val usage = breakdown.usage
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Total ${formatBytes(usage.totalBytes)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "Downloads ${formatBytes(usage.downloadedBytes)} · " +
                    "Stream cache ${formatBytes(usage.cachedBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun BookCard(
    entry: BookEntry,
    onRemoveDownload: (String) -> Unit,
    onClearCache: (String) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title ?: "ISBN ${entry.materialId}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                if (entry.title != null) {
                    Text(
                        text = "ISBN ${entry.materialId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = buildString {
                        append(formatBytes(entry.totalBytes))
                        append(" · ")
                        if (entry.downloadedBytes == null) {
                            append("cache only")
                        } else {
                            append("downloaded ${formatBytes(entry.downloadedBytes)}")
                            if (entry.cachedBytes > 0) {
                                append(", cache ${formatBytes(entry.cachedBytes)}")
                            }
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
            if (entry.cachedBytes > 0) {
                TextButton(onClick = { onClearCache(entry.materialId) }) {
                    Text("Clear cache")
                }
            }
            if (entry.downloadedBytes != null) {
                IconButton(onClick = { onRemoveDownload(entry.materialId) }) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.wdb_ic_trashcan),
                        contentDescription = "Remove download",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun OtherCard(
    title: String,
    subtitle: String,
    bytes: Long,
    onRemove: (() -> Unit)? = null,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$title · ${formatBytes(bytes)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
            if (onRemove != null) {
                TextButton(onClick = onRemove) {
                    Text("Clear")
                }
            }
        }
    }
}
