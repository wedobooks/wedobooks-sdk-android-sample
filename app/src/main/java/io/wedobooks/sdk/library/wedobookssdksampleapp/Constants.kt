package io.wedobooks.sdk.library.wedobookssdksampleapp

import io.wedobooks.sdk.models.SdkMode

object Constants {
    val SDK_APP_ID = BuildConfig.FIREBASE_APP_ID
    val SDK_API_KEY = BuildConfig.FIREBASE_API_KEY
    val SDK_PROJECT_ID = BuildConfig.FIREBASE_PROJECT_ID
    val E_BOOK: String = TODO("insert ebook isbn")
    val AUDIO_BOOK: String = TODO("insert audiobook isbn")
    // Choose the SDK mode: SdkMode.Library or SdkMode.Streaming
    val SDK_MODE: SdkMode = TODO("insert SDK mode, e.g. SdkMode.Streaming")
}