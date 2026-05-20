package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.models.Checkout
import io.wedobooks.sdk.models.StatData
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class StatsScreenViewModel(
    private val checkouts: List<Checkout>,
) : ViewModel() {
    val statsForCurrentYear: Flow<Map<String, StatData>> =
        WeDoBooksSdk.userOperations.totalStats(LocalDate.now().year.toString())

    /**
     * Per-checkout stats keyed by `checkout.id`. Order matches [checkouts].
     */
    val statsByCheckoutId: Map<String, Flow<Map<String, StatData>>> = checkouts
        .associateBy({ it.id }) { WeDoBooksSdk.userOperations.totalStats(it) }

    /** Convenience: the active checkouts in display order. */
    val orderedCheckouts: List<Checkout> = checkouts

    companion object {
        fun factory(
            checkouts: List<Checkout>,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                StatsScreenViewModel(checkouts = checkouts)
            }
        }
    }
}
