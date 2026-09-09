package io.wedobooks.sdk.library.wedobookssdksampleapp.books

data class TestBook(
    val isbn: String,
    val title: String? = null,
) {
    val displayName: String get() = title?.takeIf { it.isNotBlank() } ?: isbn
}
