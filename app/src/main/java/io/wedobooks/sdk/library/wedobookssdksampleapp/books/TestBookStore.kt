package io.wedobooks.sdk.library.wedobookssdksampleapp.books

import io.wedobooks.sdk.library.wedobookssdksampleapp.storage.KeyValueStore
import org.json.JSONArray
import org.json.JSONObject

/**
 * Books that have been checked out successfully, newest first, per environment.
 *
 * Nothing is written until a checkout succeeds — the same rule the login screen
 * applies to UIDs. That keeps unreachable ISBNs out of the list, and means the
 * stored title always came from a real `Checkout`.
 */
class TestBookStore(private val store: KeyValueStore) {

    fun all(envId: String): List<TestBook> = decode(store.getString(key(envId)))

    /** Records a successful checkout of [isbn], moving it to the front. */
    fun remember(envId: String, isbn: String, title: String?) {
        val trimmedIsbn = isbn.trim()
        if (trimmedIsbn.isEmpty()) return

        val book = TestBook(
            isbn = trimmedIsbn,
            title = title?.trim()?.takeIf { it.isNotEmpty() },
        )
        val updated = listOf(book) + all(envId).filterNot { it.isbn == trimmedIsbn }
        store.putString(key(envId), encode(updated))
    }

    fun forget(envId: String, isbn: String) {
        store.putString(key(envId), encode(all(envId).filterNot { it.isbn == isbn }))
    }

    private fun key(envId: String) = "test_books_$envId"

    private fun encode(books: List<TestBook>): String =
        JSONArray().apply {
            books.forEach { book ->
                put(
                    JSONObject().apply {
                        put("isbn", book.isbn)
                        book.title?.let { put("title", it) }
                    }
                )
            }
        }.toString()

    private fun decode(raw: String?): List<TestBook> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            TestBook(
                isbn = obj.getString("isbn"),
                title = if (obj.has("title")) obj.getString("title") else null,
            )
        }
    }
}
