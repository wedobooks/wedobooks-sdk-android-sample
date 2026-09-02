package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import io.wedobooks.sdk.models.HistoryItem
import io.wedobooks.sdk.models.WdbException

/** Items requested per history page. Small so cursor pagination is visible while swiping. */
const val HISTORY_PAGE_LIMIT = 10

/** How many items from the end of the loaded list to prefetch the next page at. */
const val HISTORY_PREFETCH_AHEAD = 3

/**
 * Whether the next history page should be prefetched given the current pager position.
 * Pure (no coroutines / SDK) so it can be unit-tested directly.
 */
fun shouldPrefetchHistory(
    currentPage: Int,
    loadedCount: Int,
    isAppending: Boolean,
    endReached: Boolean,
    prefetchAhead: Int = HISTORY_PREFETCH_AHEAD,
): Boolean =
    loadedCount > 0 &&
        !isAppending &&
        !endReached &&
        currentPage >= loadedCount - prefetchAhead

/** Maps a history read failure to a user-facing message. */
fun historyErrorMessage(throwable: Throwable): String = when (throwable) {
    is WdbException.FeatureNotEnabled -> "History is only available in streaming mode."
    is WdbException.NoUserFound -> "Sign in to see your reading history."
    else -> "Couldn't load your reading history."
}

/** Maps a history write failure (add / markCompleted / remove) to a user-facing message. */
fun historyWriteErrorMessage(throwable: Throwable): String = when (throwable) {
    is WdbException.AlreadyInHistory -> "Already in history"
    is WdbException.MaterialNotFound -> "No material found for that ISBN"
    is WdbException.NotInHistory -> "Not in history"
    is WdbException.FeatureNotEnabled -> "History is only available in streaming mode."
    is WdbException.NoUserFound -> "Sign in to manage history."
    else -> "History action failed."
}

/**
 * Returns [state] with [materialId] dropped from the loaded items and from pending; when no
 * items remain the state collapses to [HistoryUiState.Empty]. Used after a successful remove.
 */
fun applyHistoryRemoved(state: HistoryUiState.Loaded, materialId: String): HistoryUiState {
    val items = state.items.filterNot { it.materialId == materialId }
    return if (items.isEmpty()) {
        HistoryUiState.Empty
    } else {
        state.copy(items = items, pendingIds = state.pendingIds - materialId)
    }
}

/**
 * Returns [state] with [materialId] replaced by [refreshed] (left as-is when [refreshed] is
 * null) and cleared from pending. Used after a successful mark-completed.
 */
fun applyHistoryCompleted(
    state: HistoryUiState.Loaded,
    materialId: String,
    refreshed: HistoryItem?,
): HistoryUiState.Loaded {
    val items = if (refreshed == null) {
        state.items
    } else {
        state.items.map { if (it.materialId == materialId) refreshed else it }
    }
    return state.copy(items = items, pendingIds = state.pendingIds - materialId)
}
