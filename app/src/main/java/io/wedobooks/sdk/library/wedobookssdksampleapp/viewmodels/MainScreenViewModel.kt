package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.wedobooks.sdk.WeDoBooksSDK
import io.wedobooks.sdk.library.wedobookssdksampleapp.service.AuthService
import io.wedobooks.sdk.models.CheckoutBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "MainScreenViewModel"

enum class BookType {
    AudioBook,
    EBook
}

class MainScreenViewModel: ViewModel() {
    val authService = AuthService.instance
    val isEbookCheckoutLoading = mutableStateOf(false)
    val isAudioCheckoutLoading = mutableStateOf(false)
    val didCheckoutFail = MutableStateFlow(false)

    // ask WeDoBooks for isbns for different books
    suspend fun getCheckout(bookType: BookType): CheckoutBook? {
        val isbn = when(bookType) {
            BookType.AudioBook -> "9780297395461"
            BookType.EBook -> "9780788638824"
        }
        val loader = when (bookType) {
            BookType.AudioBook ->  isAudioCheckoutLoading
            BookType.EBook ->  isEbookCheckoutLoading
        }
        loader.value = true
        return try {
            WeDoBooksSDK.bookOperations.getCheckout(isbn)
        } catch (e: Exception) {
            Log.d(TAG, "err: ${e.message}")
            didCheckoutFail.update { true }
            null
        } finally {
            loader.value = false
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