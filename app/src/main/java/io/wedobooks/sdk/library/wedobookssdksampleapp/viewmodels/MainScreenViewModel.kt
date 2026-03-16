package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.wedobooks.sdk.WeDoBooksSdk
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
            BookType.Audiobook -> "9780018134553"
            BookType.Ebook -> "9780661420706"
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
                WeDoBooksSdk.books.getCheckout(it)
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
        WeDoBooksSdk.books.stopAudioPlayer()
    }

    fun logout() {
        authService.logout()
    }

    fun removeStorage() {
        WeDoBooksSdk.storage.removeAll()
    }
}