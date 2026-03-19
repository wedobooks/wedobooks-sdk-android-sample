package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.models.Checkout
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

class StatsScreenViewModel(
    checkout: Checkout?
): ViewModel() {
    val statsForCurrentYear  = WeDoBooksSdk.userOperations.totalStats(LocalDate.now().year.toString())
    val statsForCheckout =  checkout?.let {
        WeDoBooksSdk.userOperations.totalStats(it)
    } ?: flowOf(emptyMap())

    companion object {
        fun factory(
            checkout: Checkout?
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                StatsScreenViewModel(checkout = checkout)
            }
        }
    }
}
