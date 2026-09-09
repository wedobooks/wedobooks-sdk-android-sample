package io.wedobooks.sdk.library.wedobookssdksampleapp.books

/**
 * A book the sample app can loan.
 *
 * Carries no `MaterialType`: `checkoutBook(isbn)` needs only an ISBN and the
 * returned `Checkout` reports the type.
 *
 * [title] is null until the ISBN has been checked out successfully — the SDK
 * returns the title on the `Checkout`, so nothing is typed by hand.
 */
data class TestBook(
    val isbn: String,
    val title: String? = null,
) {
    /** What the card shows: the title once known, the ISBN before that. */
    val displayName: String get() = title?.takeIf { it.isNotBlank() } ?: isbn
}
