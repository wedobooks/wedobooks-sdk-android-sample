package io.wedobooks.sdk.library.wedobookssdksampleapp.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * Minimal key/value abstraction over [SharedPreferences].
 *
 * Keeps every store in the app pointed at one preferences file, and keeps the
 * stores themselves free of Android types so they stay plain Kotlin. Stores
 * depend on this, never on [SharedPreferences] directly.
 */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

class SharedPreferencesKeyValueStore(
    private val prefs: SharedPreferences,
) : KeyValueStore {

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        // commit(), not apply(). Switching environment writes the selection
        // and then immediately kills the process via Runtime.exit(), which
        // skips the lifecycle points where Android flushes pending async
        // apply() writes - so an apply() here is silently lost and the app
        // restarts on the old environment. Writes are tiny and rare.
        prefs.edit().putString(key, value).commit()
    }
}

private const val PREFS_NAME = "wdb_sample_prefs"

/**
 * The single preferences file the sample app uses. Read synchronously, which
 * matters because the selected environment must be known before
 * `WeDoBooksSdk.setup()` runs in `Application.onCreate`.
 */
fun Context.sampleKeyValueStore(): KeyValueStore =
    SharedPreferencesKeyValueStore(getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
