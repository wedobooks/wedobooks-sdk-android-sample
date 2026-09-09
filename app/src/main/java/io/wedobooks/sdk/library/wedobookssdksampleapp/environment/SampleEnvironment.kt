package io.wedobooks.sdk.library.wedobookssdksampleapp.environment

import io.wedobooks.sdk.models.SdkMode

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
