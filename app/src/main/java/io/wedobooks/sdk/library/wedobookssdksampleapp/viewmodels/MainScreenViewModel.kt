package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.models.Checkout
import io.wedobooks.sdk.models.WdbDownloadStatus
import io.wedobooks.sdk.models.enums.MaterialType
import io.wedobooks.sdk.models.enums.WdbDownloadState
import io.wedobooks.sdk.library.wedobookssdksampleapp.services.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "MainScreenViewModel"

class MainScreenViewModel : ViewModel() {
    val authService = AuthService.instance
    val isEbookCheckoutLoading = mutableStateOf(false)
    val isAudioCheckoutLoading = mutableStateOf(false)
    val didCheckoutFail = MutableStateFlow(false)

    // ask WeDoBooks for isbns for different books
    suspend fun getCheckout(isbn: String, materialType: MaterialType): Checkout? {
        val loader = when (materialType) {
            MaterialType.Audiobook -> isAudioCheckoutLoading
            MaterialType.Ebook -> isEbookCheckoutLoading
        }
        loader.value = true

        return try {
            WeDoBooksSdk.bookOperations.checkoutBook(isbn)
        } catch (e: Exception) {
            Log.d(TAG, "err: ${e.message}")
            didCheckoutFail.update { true }
            null
        } finally {
            loader.value = false
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
