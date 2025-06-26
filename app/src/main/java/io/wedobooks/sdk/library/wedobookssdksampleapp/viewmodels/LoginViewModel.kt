package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.wedobooks.sdk.library.wedobookssdksampleapp.BuildConfig
import io.wedobooks.sdk.library.wedobookssdksampleapp.service.AuthService
import kotlinx.coroutines.flow.map

class LoginViewModel: ViewModel() {
    private val authService = AuthService.instance
    val isLoggedIn = authService.currentUser.map { it != null }
    val isLoading = mutableStateOf(false)

    suspend fun login() {
        isLoading.value = true
        val token = authService.getToken(BuildConfig.DEMO_USER_ID)
        token?.let {
            authService.tokenLogin(it)
        }
        isLoading.value = false
    }
}