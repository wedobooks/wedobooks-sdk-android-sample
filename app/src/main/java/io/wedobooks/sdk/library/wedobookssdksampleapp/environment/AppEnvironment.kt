package io.wedobooks.sdk.library.wedobookssdksampleapp.environment

import android.content.Context
import io.wedobooks.sdk.library.wedobookssdksampleapp.storage.sampleKeyValueStore
import io.wedobooks.sdk.library.wedobookssdksampleapp.utils.restartApp

/** [init] must run before `WeDoBooksSdk.setup()`, which is one-shot per process. */
object AppEnvironment {

    private lateinit var store: EnvironmentStore

    lateinit var current: SampleEnvironment
        private set

    val all: List<SampleEnvironment> get() = Environments.all

    val hasMultiple: Boolean get() = all.size > 1

    fun init(context: Context) {
        store = EnvironmentStore(context.sampleKeyValueStore())
        current = resolveEnvironment(all, store.savedId())
    }

    fun select(context: Context, id: String) {
        if (id == current.id) return
        store.save(id)
        context.restartApp()
    }
}
