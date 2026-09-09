package io.wedobooks.sdk.library.wedobookssdksampleapp.storage

import android.content.Context
import io.wedobooks.sdk.library.wedobookssdksampleapp.books.TestBookStore
import io.wedobooks.sdk.library.wedobookssdksampleapp.services.UserIdStore

/**
 * Process-wide stores, initialised in `SampleApplication.onCreate` alongside
 * `AppEnvironment` and before anything reads them.
 *
 * The stores themselves take a [KeyValueStore] so they stay unit-testable; this
 * object only holds the wired-up instances the app uses.
 */
object SampleStores {

    lateinit var userIds: UserIdStore
        private set

    lateinit var books: TestBookStore
        private set

    fun init(context: Context) {
        val keyValueStore = context.sampleKeyValueStore()
        userIds = UserIdStore(keyValueStore)
        books = TestBookStore(keyValueStore)
    }
}
