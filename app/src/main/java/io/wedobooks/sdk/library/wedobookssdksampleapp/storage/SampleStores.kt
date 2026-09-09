package io.wedobooks.sdk.library.wedobookssdksampleapp.storage

import android.content.Context
import io.wedobooks.sdk.library.wedobookssdksampleapp.books.TestBookStore
import io.wedobooks.sdk.library.wedobookssdksampleapp.services.UserIdStore

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
