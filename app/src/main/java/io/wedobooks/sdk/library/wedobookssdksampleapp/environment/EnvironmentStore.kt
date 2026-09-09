package io.wedobooks.sdk.library.wedobookssdksampleapp.environment

import io.wedobooks.sdk.library.wedobookssdksampleapp.storage.KeyValueStore

private const val KEY_SELECTED_ENVIRONMENT = "selected_environment_id"

class EnvironmentStore(private val store: KeyValueStore) {

    fun savedId(): String? = store.getString(KEY_SELECTED_ENVIRONMENT)

    fun save(id: String) = store.putString(KEY_SELECTED_ENVIRONMENT, id)
}
