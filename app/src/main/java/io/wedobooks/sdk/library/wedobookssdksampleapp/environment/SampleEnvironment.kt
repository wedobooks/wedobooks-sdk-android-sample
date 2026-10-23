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
    // Internal SDK progress tracking, per material: player = audiobook, reader = ebook.
    val internalProgressPlayer: Boolean = true,
    val internalProgressReader: Boolean = true,
    // Added to the saved user IDs on the login screen once; forgetting one there sticks.
    val seedUserIds: List<String> = emptyList(),
    // Added to the saved books once, like seedUserIds.
    val seedIsbns: List<String> = emptyList(),
)
