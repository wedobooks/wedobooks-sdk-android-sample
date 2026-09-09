package io.wedobooks.sdk.library.wedobookssdksampleapp.utils

import android.content.Context
import android.content.Intent
import io.wedobooks.sdk.library.wedobookssdksampleapp.services.AuthService

/**
 * Signs out and hard-restarts the process.
 *
 * `WeDoBooksSdk.setup()` is one-shot per process, so a new environment can only
 * take effect after a fresh start. Signing out first avoids leaving behind a
 * session that belongs to a different Firebase project.
 *
 * The kill also tears down `WdbAudioPlayerSessionService`, which is what we
 * want when the backend changes underneath it.
 */
fun Context.restartApp() {
    AuthService.instance.logout()

    val intent = packageManager.getLaunchIntentForPackage(packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    startActivity(intent)

    Runtime.getRuntime().exit(0)
}
