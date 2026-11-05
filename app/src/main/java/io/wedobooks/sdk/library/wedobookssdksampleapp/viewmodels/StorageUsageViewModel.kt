package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.models.WdbStorageUsage
import io.wedobooks.sdk.library.wedobookssdksampleapp.environment.AppEnvironment
import io.wedobooks.sdk.library.wedobookssdksampleapp.storage.SampleStores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "StorageUsageViewModel"

class StorageUsageViewModel : ViewModel() {
    /** One book's share of the SDK's storage. [downloadedBytes] is `null` when it is cache only. */
    data class BookEntry(
        val materialId: String,
        val title: String?,
        val downloadedBytes: Long?,
        val cachedBytes: Long,
    ) {
        val totalBytes: Long get() = (downloadedBytes ?: 0L) + cachedBytes
    }

    data class Breakdown(
        val usage: WdbStorageUsage,
        val books: List<BookEntry>,
        /** Downloads on the device that belong to other signed-in users. */
        val otherUsersDownloadedBytes: Long,
        /** Stream cache the SDK cannot attribute to a current checkout. */
        val unattributedCachedBytes: Long,
    )

    private val _breakdown = MutableStateFlow<Breakdown?>(null)
    val breakdown = _breakdown.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isLoading.update { true }
            try {
                _breakdown.update { loadBreakdown() }
            } catch (t: Throwable) {
                Log.d(TAG, "storage usage err: ${t.message}")
                _error.update { "Could not read storage usage: ${t.message}" }
            } finally {
                _isLoading.update { false }
            }
        }
    }

    fun clearError() = _error.update { null }

    fun removeDownload(materialId: String) {
        runCatching { WeDoBooksSdk.storageOperations.removeBook(materialId) }
            .onFailure { t -> _error.update { "Could not remove download: ${t.message}" } }
        refresh()
    }

    /** Clears every download and the stream cache. */
    fun removeAll() {
        runCatching { WeDoBooksSdk.storageOperations.removeAll() }
            .onFailure { t -> _error.update { "Could not remove downloads: ${t.message}" } }
        refresh()
    }

    /** Clears the streamed audio of one book; its download, if any, stays. */
    fun clearCache(materialId: String) {
        viewModelScope.launch {
            try {
                WeDoBooksSdk.storageOperations.clearCache(materialId)
            } catch (t: Throwable) {
                Log.d(TAG, "clearCache err: ${t.message}")
                _error.update { "Could not clear cache: ${t.message}" }
            }
            refresh()
        }
    }

    /** Clears streamed audio that belongs to no current checkout and no download. */
    fun clearOrphanedCache() {
        viewModelScope.launch {
            try {
                WeDoBooksSdk.storageOperations.clearOrphanedCache()
            } catch (t: Throwable) {
                Log.d(TAG, "clearOrphanedCache err: ${t.message}")
                _error.update { "Could not clear orphaned cache: ${t.message}" }
            }
            refresh()
        }
    }

    /** Clears streamed audio that no download refers to; downloads stay. */
    fun clearCache() {
        viewModelScope.launch {
            try {
                WeDoBooksSdk.storageOperations.clearCache()
            } catch (t: Throwable) {
                Log.d(TAG, "clearCache err: ${t.message}")
                _error.update { "Could not clear stream cache: ${t.message}" }
            }
            refresh()
        }
    }

    // Cache is only readable per current checkout, so books that were streamed but are no longer
    // checked out can't be listed; their bytes end up in unattributedCachedBytes.
    private suspend fun loadBreakdown(): Breakdown {
        val usage = WeDoBooksSdk.storageOperations.getStorageUsage()
        val checkouts = WeDoBooksSdk.bookOperations.allCheckoutsFlow().first()
        val titles = SampleStores.books.all(AppEnvironment.current.id)
            .associate { it.isbn to it.title } +
            checkouts.associate { it.materialId to it.title.takeIf(String::isNotBlank) }

        val books = usage.books.map { (materialId, book) ->
            BookEntry(
                materialId = materialId,
                title = titles[materialId],
                downloadedBytes = book.downloadedBytes,
                cachedBytes = book.cachedBytes,
            )
        }
            .filter { it.totalBytes > 0 }
            .sortedByDescending { it.totalBytes }

        return Breakdown(
            usage = usage,
            books = books,
            otherUsersDownloadedBytes =
                (usage.downloadedBytes - usage.books.values.sumOf { it.downloadedBytes ?: 0L })
                    .coerceAtLeast(0),
            unattributedCachedBytes =
                (usage.cachedBytes - books.sumOf { it.cachedBytes }).coerceAtLeast(0),
        )
    }
}
