package io.wedobooks.sdk.library.wedobookssdksampleapp.services

import io.wedobooks.sdk.library.wedobookssdksampleapp.storage.KeyValueStore
import org.json.JSONArray

class UserIdStore(private val store: KeyValueStore) {

    fun all(envId: String): List<String> = decode(store.getString(key(envId)))

    fun mostRecent(envId: String): String? = all(envId).firstOrNull()

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
