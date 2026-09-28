package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.wedobooks.sdk.library.wedobookssdksampleapp.environment.AppEnvironment
import io.wedobooks.sdk.library.wedobookssdksampleapp.services.AuthService
import io.wedobooks.sdk.library.wedobookssdksampleapp.storage.SampleStores
import kotlinx.coroutines.flow.map

class LoginViewModel : ViewModel() {

    private val authService = AuthService.instance
    private val envId get() = AppEnvironment.current.id

    val isLoggedIn = authService.currentUser.map { it != null }
    val isLoading = mutableStateOf(false)

    /** UIDs that have signed in successfully in this environment, newest first. */
    var rememberedUids by mutableStateOf(SampleStores.userIds.all(envId))
        private set

    val mostRecentUid: String? get() = rememberedUids.firstOrNull()

    /**
     * Signs in as [uid], remembering it only if the sign-in succeeds.
     *
     * @return true when signed in.
     */
    suspend fun login(uid: String): Boolean {
        val trimmed = uid.trim()
        if (trimmed.isEmpty()) return false

        isLoading.value = true
        return try {
            val token = authService.getToken(trimmed)
            if (token == null) {
                false
            } else {
                val signedIn = authService.tokenLogin(token).getOrDefault(false)
                if (signedIn) {
                    SampleStores.userIds.remember(envId, trimmed)
                    rememberedUids = SampleStores.userIds.all(envId)
                }
                signedIn
            }
        } finally {
            isLoading.value = false
        }
    }

    fun forgetUid(uid: String) {
        SampleStores.userIds.forget(envId, uid)
        rememberedUids = SampleStores.userIds.all(envId)
    }
}
