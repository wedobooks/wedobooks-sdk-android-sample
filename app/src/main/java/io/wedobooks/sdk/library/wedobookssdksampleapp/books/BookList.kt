package io.wedobooks.sdk.library.wedobookssdksampleapp.books

fun mergeBooks(seed: List<TestBook>, remembered: List<TestBook>): List<TestBook> {
    val combined = seed + remembered

    val titlesByIsbn: Map<String, String> = combined
        .mapNotNull { book ->
            book.title?.takeIf { it.isNotBlank() }?.let { title -> book.isbn to title }
        }
        .toMap()

    return combined
        .distinctBy { it.isbn }
        .map { book -> book.copy(title = titlesByIsbn[book.isbn]) }
}
