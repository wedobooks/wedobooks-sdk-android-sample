package io.wedobooks.sdk.library.wedobookssdksampleapp.environment

import android.content.Context
import io.wedobooks.sdk.library.wedobookssdksampleapp.storage.sampleKeyValueStore
import io.wedobooks.sdk.library.wedobookssdksampleapp.utils.restartApp

/**
 * The environment this process is running against.
 *
 * [init] MUST run before `WeDoBooksSdk.setup()`. The SDK is one-shot per
 * process, so the environment is fixed for the lifetime of the process and
 * changing it means restarting.
 */
object AppEnvironment {

    private lateinit var store: EnvironmentStore

    lateinit var current: SampleEnvironment
        private set

    val all: List<SampleEnvironment> get() = Environments.all

    /** True when there is something to pick between, i.e. show the picker. */
    val hasMultiple: Boolean get() = all.size > 1

    fun init(context: Context) {
        store = EnvironmentStore(context.sampleKeyValueStore())
        current = resolveEnvironment(all, store.savedId())
    }

    /**
     * Saves [id] and restarts the process so the SDK initialises against it.
     * Does nothing when [id] is already current.
     */
    fun select(context: Context, id: String) {
        if (id == current.id) return
        store.save(id)
        context.restartApp()
    }
}
