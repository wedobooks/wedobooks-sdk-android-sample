package io.wedobooks.sdk.library.wedobookssdksampleapp

import io.wedobooks.sdk.library.wedobookssdksampleapp.environment.AppEnvironment
import io.wedobooks.sdk.models.SdkMode

/**
 * Flat accessors over the active environment.
 *
 * These are `get()` accessors, not `val`s, on purpose. `Constants` is an
 * `object`, so eager `val`s all initialise on first touch of any member — one
 * unfilled placeholder would then take down every unrelated read.
 */
object Constants {
    private val env get() = AppEnvironment.current

    val SDK_APP_ID: String get() = env.firebaseAppId
    val SDK_API_KEY: String get() = env.firebaseApiKey
    val SDK_PROJECT_ID: String get() = env.firebaseProjectId
    val CUSTOM_TOKEN_URL: String get() = env.customTokenUrl
    val READER_API_KEY: String get() = env.readerApiKey
    val READER_API_SECRET: String get() = env.readerApiSecret
    val SDK_MODE: SdkMode get() = env.sdkMode

    /**
     * Pre-fills the reserve field on the Reservations tab. Optional — set it to
     * an ISBN if you always reserve the same title while testing.
     */
    val RESERVATION_BOOK: String? get() = null
}
