package io.wedobooks.sdk.library.wedobookssdksampleapp.books

import io.wedobooks.sdk.library.wedobookssdksampleapp.storage.KeyValueStore
import org.json.JSONArray
import org.json.JSONObject

class TestBookStore(private val store: KeyValueStore) {

    fun all(envId: String): List<TestBook> = decode(store.getString(key(envId)))

    fun remember(envId: String, isbn: String, title: String?) {
        val trimmedIsbn = isbn.trim()
        if (trimmedIsbn.isEmpty()) return

        val book = TestBook(
            isbn = trimmedIsbn,
            title = title?.trim()?.takeIf { it.isNotEmpty() },
        )
        val updated = (listOf(book) + all(envId).filterNot { it.isbn == trimmedIsbn })
            .distinctBy { it.isbn }
        store.putString(key(envId), encode(updated))
    }

    fun rememberAll(envId: String, books: List<TestBook>) {
        if (books.isEmpty()) return

        val existing = all(envId)
        val existingByIsbn = existing.associateBy { it.isbn }

        val incoming = books
            .sortedByDescending { !it.title.isNullOrBlank() }
            .distinctBy { it.isbn }

        val additions = incoming.filter { it.isbn !in existingByIsbn.keys }
        val titleFor = incoming
            .filter { incoming ->
                val current = existingByIsbn[incoming.isbn]
                current != null &&
                    current.title.isNullOrBlank() &&
                    !incoming.title.isNullOrBlank()
            }
            .associate { it.isbn to it.title }

        if (additions.isEmpty() && titleFor.isEmpty()) return

        val updated = (additions + existing.map { book ->
            titleFor[book.isbn]?.let { book.copy(title = it) } ?: book
        }).distinctBy { it.isbn }
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
