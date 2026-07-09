package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.models.HistoryCursor
import io.wedobooks.sdk.models.HistoryItem
import io.wedobooks.sdk.models.HistoryPage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the history pager: loads the first page on init, then prefetches the next
 * page as the user swipes toward the end of the loaded items. Also exposes per-item
 * write actions ([markCompleted], [remove]) used by the cards.
 *
 * The SDK operations are injectable so pagination and the write actions can be exercised
 * with fakes in tests; by default they forward to [WeDoBooksSdk.historyOperations].
 */
class HistoryPagerViewModel(
    private val loadPage: suspend (after: HistoryCursor?) -> HistoryPage = { after ->
        WeDoBooksSdk.historyOperations.list(after = after, limit = HISTORY_PAGE_LIMIT)
    },
    private val markCompletedOp: suspend (materialId: String) -> Unit = { id ->
        WeDoBooksSdk.historyOperations.markCompleted(id)
    },
    private val removeOp: suspend (materialId: String) -> Unit = { id ->
        WeDoBooksSdk.historyOperations.remove(id)
    },
    private val entryOp: suspend (materialId: String) -> HistoryItem? = { id ->
        WeDoBooksSdk.historyOperations.entry(id)
    },
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    /** One-shot, user-facing messages for failed write actions (shown as a toast). */
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    /** Forward-only cursor for the next page; `null` once exhausted. */
    private var nextCursor: HistoryCursor? = null

    init {
        loadInitial()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            try {
                val page = loadPage(null)
                _uiState.value = if (page.items.isEmpty()) {
                    nextCursor = null
                    HistoryUiState.Empty
                } else {
                    nextCursor = page.nextCursor
                    HistoryUiState.Loaded(
                        items = page.items,
                        isAppending = false,
                        endReached = page.nextCursor == null,
                    )
                }
            } catch (t: Throwable) {
                _uiState.value = HistoryUiState.Error(historyErrorMessage(t))
            }
        }
    }

    /** Called as the pager settles on [currentPage]; appends the next page when near the end. */
    fun prefetchAround(currentPage: Int) {
        val state = _uiState.value as? HistoryUiState.Loaded ?: return
        if (!shouldPrefetchHistory(currentPage, state.items.size, state.isAppending, state.endReached)) return
        appendNextPage(state)
    }

    private fun appendNextPage(current: HistoryUiState.Loaded) {
        val cursor = nextCursor ?: return
        _uiState.value = current.copy(isAppending = true)
        viewModelScope.launch {
            try {
                val page = loadPage(cursor)
                val latest = _uiState.value as? HistoryUiState.Loaded ?: return@launch
                nextCursor = page.nextCursor
                _uiState.value = latest.copy(
                    items = latest.items + page.items,
                    isAppending = false,
                    endReached = page.nextCursor == null,
                )
            } catch (t: Throwable) {
                // Keep already-loaded items; just stop the spinner. The next swipe retries.
                val latest = _uiState.value as? HistoryUiState.Loaded ?: return@launch
                _uiState.value = latest.copy(isAppending = false)
            }
        }
    }

    /** Marks [item] completed, then swaps in the refreshed entry. No-op while already pending. */
    fun markCompleted(item: HistoryItem) {
        if (!beginPending(item.materialId)) return
        viewModelScope.launch {
            try {
                markCompletedOp(item.materialId)
                val refreshed = entryOp(item.materialId)
                val latest = _uiState.value as? HistoryUiState.Loaded ?: return@launch
                _uiState.value = applyHistoryCompleted(latest, item.materialId, refreshed)
            } catch (t: Throwable) {
                clearPending(item.materialId)
                _events.tryEmit(historyWriteErrorMessage(t))
            }
        }
    }

    /** Removes [item] from history, then drops it from the list. No-op while already pending. */
    fun remove(item: HistoryItem) {
        if (!beginPending(item.materialId)) return
        viewModelScope.launch {
            try {
                removeOp(item.materialId)
                val latest = _uiState.value as? HistoryUiState.Loaded ?: return@launch
                _uiState.value = applyHistoryRemoved(latest, item.materialId)
            } catch (t: Throwable) {
                clearPending(item.materialId)
                _events.tryEmit(historyWriteErrorMessage(t))
            }
        }
    }

    /** Adds [materialId] to pending; returns false if not Loaded or an op is already in flight for it. */
    private fun beginPending(materialId: String): Boolean {
        val state = _uiState.value as? HistoryUiState.Loaded ?: return false
        if (materialId in state.pendingIds) return false
        _uiState.value = state.copy(pendingIds = state.pendingIds + materialId)
        return true
    }

    private fun clearPending(materialId: String) {
        val latest = _uiState.value as? HistoryUiState.Loaded ?: return
        _uiState.value = latest.copy(pendingIds = latest.pendingIds - materialId)
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer { HistoryPagerViewModel() }
        }
    }
}
