package io.wedobooks.sdk.library.wedobookssdksampleapp.books

/**
 * ISBNs this build always shows, in every environment.
 *
 * Entirely optional — books loaned from inside the app are remembered
 * automatically, with their titles. Use this only for ISBNs you want present
 * on a fresh install.
 *
 * Seeded ISBNs have no title until the first successful loan, so their cards
 * show the raw ISBN to begin with.
 *
 * Keep your local edits out of git:
 *
 *     git update-index --skip-worktree app/src/main/java/io/wedobooks/sdk/library/wedobookssdksampleapp/books/TestBooks.kt
 */
object TestBooks {
    val seed: List<TestBook> = listOf(
        // TestBook(isbn = "9788712345678"),
    )
}
