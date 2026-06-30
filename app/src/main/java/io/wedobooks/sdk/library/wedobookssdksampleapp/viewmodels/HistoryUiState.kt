package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import io.wedobooks.sdk.models.HistoryItem

/** State of the history pager beneath the stats. */
sealed interface HistoryUiState {
    /** First page is loading. */
    data object Loading : HistoryUiState

    /** Loading failed; [message] is user-facing. */
    data class Error(val message: String) : HistoryUiState

    /** Loaded successfully but the user has no history. */
    data object Empty : HistoryUiState

    /**
     * History loaded.
     * @property items All items loaded so far (across appended pages).
     * @property isAppending A follow-up page is currently being fetched.
     * @property endReached No more pages remain (cursor exhausted).
     * @property pendingIds Material ids with an in-flight write (mark-completed / remove),
     *   used to disable that card's actions while its operation runs.
     */
    data class Loaded(
        val items: List<HistoryItem>,
        val isAppending: Boolean,
        val endReached: Boolean,
        val pendingIds: Set<String> = emptySet(),
    ) : HistoryUiState
}
