package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.library.wedobookssdksampleapp.service.AuthService
import io.wedobooks.sdk.models.Checkout
import io.wedobooks.sdk.models.enums.MaterialType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "MainScreenViewModel"

class MainScreenViewModel: ViewModel() {
    val authService = AuthService.instance
    val isEbookCheckoutLoading = mutableStateOf(false)
    val isAudioCheckoutLoading = mutableStateOf(false)
    val didCheckoutFail = MutableStateFlow(false)

    // ask WeDoBooks for isbns for different books
    suspend fun getCheckout(bookType: MaterialType): Checkout? {
        val isbn = when(bookType) {
            MaterialType.Audiobook -> "TODO" // Fill in isbn from your catalog
            MaterialType.Ebook -> "TODO" // Fill in isbn from your catalog
            else -> null
        }
        val loader = when (bookType) {
            MaterialType.Audiobook ->  isAudioCheckoutLoading
            MaterialType.Ebook ->  isEbookCheckoutLoading
            else -> mutableStateOf(false)
        }
        loader.value = true


        return isbn?.let {
            try {
                WeDoBooksSdk.bookOperations.checkoutBook(it)
            } catch (e: Exception) {
                Log.d(TAG, "err: ${e.message}")
                didCheckoutFail.update { true }
                null
            } finally {
                loader.value = false
            }
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
}