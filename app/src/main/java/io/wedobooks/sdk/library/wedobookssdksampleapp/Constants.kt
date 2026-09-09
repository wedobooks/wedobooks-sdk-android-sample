package io.wedobooks.sdk.library.wedobookssdksampleapp

import io.wedobooks.sdk.library.wedobookssdksampleapp.environment.AppEnvironment
import io.wedobooks.sdk.models.SdkMode

// get() accessors, not vals: an object's eager vals all initialise on first
// touch of any member, so one unfilled placeholder takes down every read.
object Constants {
    private val env get() = AppEnvironment.current

    val SDK_APP_ID: String get() = env.firebaseAppId
    val SDK_API_KEY: String get() = env.firebaseApiKey
    val SDK_PROJECT_ID: String get() = env.firebaseProjectId
    val CUSTOM_TOKEN_URL: String get() = env.customTokenUrl
    val READER_API_KEY: String get() = env.readerApiKey
    val READER_API_SECRET: String get() = env.readerApiSecret
    val SDK_MODE: SdkMode get() = env.sdkMode

    /** Pre-fills the reserve field on the Reservations tab. */
    val RESERVATION_BOOK: String? get() = null
}
