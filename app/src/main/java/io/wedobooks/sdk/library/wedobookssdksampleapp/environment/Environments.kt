package io.wedobooks.sdk.library.wedobookssdksampleapp.environment

import io.wedobooks.sdk.models.SdkMode

/**
 * Environments this build can run against. Fill in your own values.
 *
 * With a single entry the app behaves exactly as it always has. Add a second
 * entry and an environment picker appears on the login screen.
 *
 * Once filled in this file holds real credentials. Keep your local edits out
 * of git:
 *
 *     git update-index --skip-worktree app/src/main/java/io/wedobooks/sdk/library/wedobookssdksampleapp/environment/Environments.kt
 */
object Environments {
    val all = listOf(
        SampleEnvironment(
            id = "default",
            label = "Default",
            sdkMode = SdkMode.Streaming,
            firebaseApiKey = "<firebase-api-key>",
            firebaseAppId = "<firebase-app-id>",
            firebaseProjectId = "<firebase-project-id>",
            customTokenUrl = "<custom-token-url>",
            readerApiKey = "<reader-api-key>",
            readerApiSecret = "<reader-api-secret>",
        ),
        // Add more entries here to get an in-app environment picker.
        // Give each one its own `id`: ids namespace the remembered UIDs
        // and loaned books, so a duplicate would share them.
    )
}
