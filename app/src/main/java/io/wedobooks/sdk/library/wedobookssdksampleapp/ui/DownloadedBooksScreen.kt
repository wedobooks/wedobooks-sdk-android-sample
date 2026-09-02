package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wedobooks.sdk.R
import io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels.DownloadedBooksViewModel

@Composable
fun DownloadedBooksScreen(
    goBack: () -> Unit,
) {
    val viewModel: DownloadedBooksViewModel = viewModel()
    val downloadedList: List<String> by viewModel.downloadedListFlow.collectAsState(emptyList())

    LaunchedEffect(Unit) {
        viewModel.getDownloadedBooks()
    }

    Column(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize(),
    ) {
        // ---- Top bar: close + title ----------------------------------------
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
                modifier = Modifier.padding(start = 4.dp),
                text = "Downloaded books",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // ---- Subheader: count + refresh ------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when (downloadedList.size) {
                    0 -> "No downloads"
                    1 -> "1 book on this device"
                    else -> "${downloadedList.size} books on this device"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            TextButton(onClick = { viewModel.getDownloadedBooks() }) {
                Text("Refresh")
            }
        }

        // ---- Content -------------------------------------------------------
        if (downloadedList.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(downloadedList)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.items(downloaded: List<String>) {
    downloaded.forEach { isbn ->
        item(key = "download-$isbn") {
            DownloadedBookCard(isbn = isbn)
        }
    }
}

@Composable
private fun DownloadedBookCard(isbn: String) {
    val viewModel: DownloadedBooksViewModel = viewModel()
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
                    text = "ISBN $isbn",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Stored locally",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
            IconButton(
                onClick = {
                    viewModel.removeDownload(isbn)
                    viewModel.getDownloadedBooks()
                },
            ) {
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

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Nothing downloaded yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = "Download a book from the Checkouts tab to see it listed here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
    }
}
