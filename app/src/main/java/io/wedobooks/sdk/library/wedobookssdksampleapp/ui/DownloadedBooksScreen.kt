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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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

    Box(modifier = Modifier
        .systemBarsPadding()
        .fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                modifier = Modifier.padding(24.dp).size(48.dp),
                onClick = goBack
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CustomButton(
                title = "refresh",
                onClick = {
                    viewModel.getDownloadedBooks()
                }
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(items = downloadedList) {
                    CustomButton(
                        modifier = Modifier.padding(vertical = 8.dp),
                        title = it,
                        color = MaterialTheme.colorScheme.secondary,
                        textColor = MaterialTheme.colorScheme.onSecondary,
                        onClick = {
                            viewModel.removeDownload(it)
                            viewModel.getDownloadedBooks()
                        }
                    )
                }
            }
        }
    }
}