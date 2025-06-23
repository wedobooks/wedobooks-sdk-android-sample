package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.wedobooks.sdk.library.wedobookssdksampleapp.service.AuthService
import kotlinx.coroutines.flow.map

class LoginViewModel: ViewModel() {
    private val authService = AuthService.instance
    val isLoggedIn = authService.currentUser.map { it != null }
    val isLoading = mutableStateOf(false)

    suspend fun login(uid: String) {
        isLoading.value = true
        val token = authService.getToken(uid)
        token?.let {
            authService.tokenLogin(it)
        }
        isLoading.value = false
    }
}