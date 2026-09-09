package io.wedobooks.sdk.library.wedobookssdksampleapp.books

/**
 * Merges the committed seed list with the books remembered for an environment.
 *
 * Keyed by ISBN, seed entries first. A seed ISBN starts titleless and picks up
 * its title from the remembered copy once it has been loaned successfully, so
 * the same book never appears twice and the title always wins over a bare ISBN.
 */
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
