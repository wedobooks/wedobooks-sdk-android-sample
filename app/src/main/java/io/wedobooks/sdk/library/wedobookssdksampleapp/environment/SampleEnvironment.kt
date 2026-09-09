package io.wedobooks.sdk.library.wedobookssdksampleapp.environment

import io.wedobooks.sdk.models.SdkMode

/**
 * One backend the sample app can run against.
 *
 * [sdkMode] travels with the environment on purpose: a Library-mode backend and
 * a Streaming-mode backend need different SDK modes, and keeping them together
 * removes the old footgun of switching credentials but forgetting the mode.
 */
data class SampleEnvironment(
    val id: String,
    val label: String,
    val sdkMode: SdkMode,
    val firebaseApiKey: String,
    val firebaseAppId: String,
    val firebaseProjectId: String,
    val customTokenUrl: String,
    val readerApiKey: String,
    val readerApiSecret: String,
)
