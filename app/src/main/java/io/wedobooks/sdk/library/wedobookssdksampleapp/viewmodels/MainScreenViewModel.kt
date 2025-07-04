package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.wedobooks.sdk.WeDoBooksSDK
import io.wedobooks.sdk.library.wedobookssdksampleapp.service.AuthService
import io.wedobooks.sdk.models.CheckoutBook
import io.wedobooks.sdk.models.enums.BookType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "MainScreenViewModel"

class MainScreenViewModel: ViewModel() {
    val authService = AuthService.instance
    val isEbookCheckoutLoading = mutableStateOf(false)
    val isAudioCheckoutLoading = mutableStateOf(false)
    val didCheckoutFail = MutableStateFlow(false)

    // ask WeDoBooks for isbns for different books
    suspend fun getCheckout(bookType: BookType): CheckoutBook? {
        val isbn = when(bookType) {
            BookType.Audiobook -> "9780297395461"
            BookType.Ebook -> "9780788638824"
            else -> null
        }
        val loader = when (bookType) {
            BookType.Audiobook ->  isAudioCheckoutLoading
            BookType.Ebook ->  isEbookCheckoutLoading
            else -> mutableStateOf(false)
        }
        loader.value = true


        return isbn?.let {
            try {
                WeDoBooksSDK.bookOperations.getCheckout(it)
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
        WeDoBooksSDK.bookOperations.stopAudioPlayer()
    }

    fun logout() {
        authService.logout()
    }

    fun removeStorage() {
        WeDoBooksSDK.storageOperations.removeAll()
    }
}