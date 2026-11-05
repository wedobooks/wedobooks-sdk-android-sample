package io.wedobooks.sdk.library.wedobookssdksampleapp.models

import io.wedobooks.sdk.models.Checkout
import io.wedobooks.sdk.models.WdbException
import io.wedobooks.sdk.models.enums.CanLoanResponseType

/** Outcome of loaning a book from the sample app. */
sealed class LoanResult {
    data class Loaned(val checkout: Checkout) : LoanResult()

    /** The backend refused the loan; [reason] says why (Reservable, Wishable, a limit, ...). */
    data class Rejected(val reason: CanLoanResponseType, val quota: Int?) : LoanResult()

    data class Failed(val message: String) : LoanResult()

    /** One line for the result text under the ISBN field. */
    fun describe(isbn: String): String = when (this) {
        is Loaned -> "Loaned ${checkout.title.ifBlank { isbn }}"
        is Rejected -> buildString {
            append("Not loanable · ")
            append(reason.name)
            quota?.let { append(" (quota $it)") }
        }
        is Failed -> "Could not loan $isbn · $message"
    }

    companion object {
        fun from(t: Throwable): LoanResult = when (t) {
            is WdbException.CheckoutRejected -> Rejected(t.rejectReason, t.quota)
            else -> Failed(t.cause?.message ?: t.message ?: t::class.simpleName.orEmpty())
        }
    }
}
