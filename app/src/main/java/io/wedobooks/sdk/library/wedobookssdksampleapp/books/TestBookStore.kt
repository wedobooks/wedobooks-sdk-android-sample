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

    /** Appends each of [isbns] not seeded before, so a forgotten seed is not re-added. */
    fun seed(envId: String, isbns: List<String>) {
        val seeded = decodeIsbns(store.getString(seededKey(envId)))
        val fresh = isbns.map { it.trim() }.filter { it.isNotEmpty() && it !in seeded }.distinct()
        if (fresh.isEmpty()) return

        val existing = all(envId)
        val additions = fresh.filter { isbn -> existing.none { it.isbn == isbn } }
        store.putString(key(envId), encode(existing + additions.map { TestBook(isbn = it) }))
        store.putString(
            seededKey(envId),
            JSONArray().apply { (seeded + fresh).forEach { put(it) } }.toString(),
        )
    }

    fun forget(envId: String, isbn: String) {
        store.putString(key(envId), encode(all(envId).filterNot { it.isbn == isbn }))
    }

    private fun key(envId: String) = "test_books_$envId"

    private fun seededKey(envId: String) = "seeded_test_books_$envId"

    private fun decodeIsbns(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { array.getString(it) }
    }

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
