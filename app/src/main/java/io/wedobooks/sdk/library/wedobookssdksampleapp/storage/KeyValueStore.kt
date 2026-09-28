package io.wedobooks.sdk.library.wedobookssdksampleapp.storage

import android.content.Context
import android.content.SharedPreferences

interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

class SharedPreferencesKeyValueStore(
    private val prefs: SharedPreferences,
) : KeyValueStore {

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        // commit(), not apply(): switching environment kills the process
        // immediately after this write, which skips the async flush.
        prefs.edit().putString(key, value).commit()
    }
}

private const val PREFS_NAME = "wdb_sample_prefs"

fun Context.sampleKeyValueStore(): KeyValueStore =
    SharedPreferencesKeyValueStore(getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
