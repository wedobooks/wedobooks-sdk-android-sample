package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.models.Checkout
import io.wedobooks.sdk.models.WdbDownloadStatus
import io.wedobooks.sdk.models.enums.WdbDownloadState
import io.wedobooks.sdk.library.wedobookssdksampleapp.books.TestBook
import io.wedobooks.sdk.library.wedobookssdksampleapp.books.TestBooks
import io.wedobooks.sdk.library.wedobookssdksampleapp.books.mergeBooks
import io.wedobooks.sdk.library.wedobookssdksampleapp.environment.AppEnvironment
import io.wedobooks.sdk.library.wedobookssdksampleapp.services.AuthService
import io.wedobooks.sdk.library.wedobookssdksampleapp.storage.SampleStores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "MainScreenViewModel"

class MainScreenViewModel : ViewModel() {
    val authService = AuthService.instance
    val isAddingToHistory = mutableStateOf(false)
    val didCheckoutFail = MutableStateFlow(false)

    /**
     * ISBNs with a checkout in flight. Per-ISBN rather than per-type, so two
     * ebooks requested at once do not share a single spinner.
     */
    val checkoutsInFlight = mutableStateOf<Set<String>>(emptySet())

    /** The seed list merged with whatever this environment has remembered. */
    var books by mutableStateOf(emptyList<TestBook>())
        private set

    /** ISBNs backed by the store, i.e. the ones that can be removed again. */
    var rememberedIsbns by mutableStateOf(emptySet<String>())
        private set

    private val envId get() = AppEnvironment.current.id

    init {
        refreshBooks()
    }

    /**
     * Checks [isbn] out. On success the ISBN and the title from the returned
     * [Checkout] are remembered for this environment; on failure nothing is
     * stored, so unreachable ISBNs never clutter the list.
     */
    suspend fun getCheckout(isbn: String): Checkout? {
        val trimmed = isbn.trim()
        if (trimmed.isEmpty()) return null

        checkoutsInFlight.value = checkoutsInFlight.value + trimmed
        return try {
            val checkout = WeDoBooksSdk.bookOperations.checkoutBook(trimmed)
            SampleStores.books.remember(envId, trimmed, checkout.title)
            refreshBooks()
            checkout
        } catch (e: Exception) {
            Log.d(TAG, "err: ${e.message}")
            didCheckoutFail.update { true }
            null
        } finally {
            checkoutsInFlight.value = checkoutsInFlight.value - trimmed
        }
    }

    /** ISBNs with a reservation request in flight. */
    val reservationsInFlight = mutableStateOf<Set<String>>(emptySet())

    /**
     * Reserves [isbn]. Returns a user-facing result line for a toast, whether
     * the reservation succeeded or not - the SDK reports refusals in the
     * response rather than by throwing.
     */
    suspend fun reserveBook(isbn: String): String {
        val trimmed = isbn.trim()
        if (trimmed.isEmpty()) return "No ISBN"

        reservationsInFlight.value = reservationsInFlight.value + trimmed
        return try {
            val response = WeDoBooksSdk.reservationOperations.reserveBook(trimmed)
            buildString {
                append(response.canLoan.name)
                response.message?.takeIf { it.isNotBlank() }?.let { append(" \u00B7 "); append(it) }
            }
        } catch (t: Throwable) {
            Log.d(TAG, "reserve err: ${t.message}")
            "Error: ${t.cause?.message ?: t.message}"
        } finally {
            reservationsInFlight.value = reservationsInFlight.value - trimmed
        }
    }

    /**
     * Saves every active checkout as a shortcut, titles included.
     *
     * A checkout knows its title, so this is what turns a bare ISBN in the
     * saved list into a readable name - and it picks up books loaned outside
     * this app entirely.
     */
    fun rememberCheckouts(checkouts: List<Checkout>) {
        if (checkouts.isEmpty()) return
        SampleStores.books.rememberAll(
            envId,
            checkouts.map { checkout ->
                TestBook(
                    isbn = checkout.materialId,
                    title = checkout.title.takeIf { it.isNotBlank() },
                )
            },
        )
        refreshBooks()
    }

    fun forgetBook(isbn: String) {
        SampleStores.books.forget(envId, isbn)
        refreshBooks()
    }

    private fun refreshBooks() {
        val remembered = SampleStores.books.all(envId)
        books = mergeBooks(TestBooks.seed, remembered)
        rememberedIsbns = remembered.map { it.isbn }.toSet()
    }

    /**
     * Adds [isbn] to the signed-in user's history (as a completed entry, per the SDK).
     *
     * @return `null` on success, or a user-facing error message to surface (e.g. a toast).
     */
    suspend fun addToHistory(isbn: String): String? {
        isAddingToHistory.value = true
        return try {
            WeDoBooksSdk.historyOperations.add(isbn.trim())
            null
        } catch (t: Throwable) {
            Log.d(TAG, "addToHistory err: ${t.message}")
            historyWriteErrorMessage(t)
        } finally {
            isAddingToHistory.value = false
        }
    }

    fun stopAudio() {
        WeDoBooksSdk.bookOperations.stopAudioPlayer()
    }

    fun logout() {
        authService.logout()
    }

    fun removeStorage() {
        WeDoBooksSdk.storageOperations.removeAll()
    }

    fun toggleDownload(checkout: Checkout, status: WdbDownloadStatus?) {
        viewModelScope.launch {
            val shouldRemove = when (status?.state) {
                WdbDownloadState.Finished,
                WdbDownloadState.Paused,
                WdbDownloadState.Cancelled,
                WdbDownloadState.Error,
                    -> true

                else -> false
            }
            if (shouldRemove) {
                runCatching {
                    WeDoBooksSdk.storageOperations.removeBook(checkout.materialId)
                }.onFailure {
                    Log.d(TAG, "removeBook err: ${it.message}")
                }
            } else {
                runCatching {
                    WeDoBooksSdk.storageOperations.downloadBook(checkout)
                }.onFailure {
                    Log.d(TAG, "downloadBook err: ${it.message}")
                }
            }
        }
    }
}
