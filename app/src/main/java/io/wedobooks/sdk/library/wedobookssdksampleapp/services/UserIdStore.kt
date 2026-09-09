package io.wedobooks.sdk.library.wedobookssdksampleapp.services

import io.wedobooks.sdk.library.wedobookssdksampleapp.storage.KeyValueStore
import org.json.JSONArray

/**
 * Every UID that has signed in successfully, newest first, per environment.
 *
 * Scoped per environment because a staging UID is meaningless in prod. Stored
 * as a JSON array string rather than a `StringSet` because order matters — the
 * newest UID is the one the login screen offers by default.
 */
class UserIdStore(private val store: KeyValueStore) {

    fun all(envId: String): List<String> = decode(store.getString(key(envId)))

    fun mostRecent(envId: String): String? = all(envId).firstOrNull()

    /** Records [uid] at the front, de-duplicating if it is already known. */
    fun remember(envId: String, uid: String) {
        val trimmed = uid.trim()
        if (trimmed.isEmpty()) return

        val updated = listOf(trimmed) + all(envId).filterNot { it == trimmed }
        store.putString(key(envId), encode(updated))
    }

    fun forget(envId: String, uid: String) {
        store.putString(key(envId), encode(all(envId).filterNot { it == uid }))
    }

    private fun key(envId: String) = "user_ids_$envId"

    private fun encode(uids: List<String>): String =
        JSONArray().apply { uids.forEach { put(it) } }.toString()

    private fun decode(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { array.getString(it) }
    }
}
