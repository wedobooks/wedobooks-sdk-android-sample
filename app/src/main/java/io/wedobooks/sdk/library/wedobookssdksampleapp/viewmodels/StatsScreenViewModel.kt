package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.wedobooks.sdk.WeDoBooksSDK
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

class StatsScreenViewModel(
    checkoutId: String?
): ViewModel() {
    val statsForCurrentYear  = WeDoBooksSDK.userOperations.userStatsForYear(LocalDate.now().year.toString())
    val statsForCheckout =  checkoutId?.let {
        WeDoBooksSDK.userOperations.userStatsForCheckout(it)
    } ?: flowOf(emptyMap())

    companion object {
        fun factory(
            checkoutId: String?
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                StatsScreenViewModel(checkoutId = checkoutId)
            }
        }
    }
}