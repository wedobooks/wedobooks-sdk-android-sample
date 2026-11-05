package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.models.WdbBookStorageUsage
import io.wedobooks.sdk.models.WdbStorageUsage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "DownloadedBooksViewModel"

class DownloadedBooksViewModel : ViewModel() {
    private val _downloadedListFlow = MutableStateFlow<List<String>>(emptyList())
    val downloadedListFlow = _downloadedListFlow.asStateFlow()

    private val _bookStorageFlow = MutableStateFlow<Map<String, WdbBookStorageUsage>>(emptyMap())
    val bookStorageFlow = _bookStorageFlow.asStateFlow()

    private val _usageFlow = MutableStateFlow<WdbStorageUsage?>(null)
    val usageFlow = _usageFlow.asStateFlow()

    private val _errorFlow = MutableStateFlow<String?>(null)
    val errorFlow = _errorFlow.asStateFlow()

    fun getDownloadedBooks() {
        val books = WeDoBooksSdk.storageOperations.getDownloadedBooks()
        _downloadedListFlow.update { books }
        viewModelScope.launch {
            try {
                _usageFlow.update { WeDoBooksSdk.storageOperations.getStorageUsage() }
                _bookStorageFlow.update {
                    books.associateWith { WeDoBooksSdk.storageOperations.getBookStorageUsage(it) }
                }
            } catch (t: Throwable) {
                Log.d(TAG, "storage sizes err: ${t.message}")
                _errorFlow.update { "Could not read storage sizes: ${t.message}" }
            }
        }
    }

    fun clearError() = _errorFlow.update { null }

    fun removeDownload(isbn: String): Boolean {
        return WeDoBooksSdk.storageOperations.removeBook(isbn)
    }
}
