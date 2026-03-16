package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import androidx.lifecycle.ViewModel
import io.wedobooks.sdk.WeDoBooksSdk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DownloadedBooksViewModel: ViewModel() {
    private val _downloadedListFlow = MutableStateFlow<List<String>>(emptyList())
    val downloadedListFlow = _downloadedListFlow.asStateFlow()

    fun getDownloadedBooks() {
        _downloadedListFlow.update {
            WeDoBooksSdk.storage.getDownloadedBooks()
        }
    }

    fun removeDownload(isbn: String): Boolean {
        return WeDoBooksSdk.storage.removeBook(isbn)
    }
}