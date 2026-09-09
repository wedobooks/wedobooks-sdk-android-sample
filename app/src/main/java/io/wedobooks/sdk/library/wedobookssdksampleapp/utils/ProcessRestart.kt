package io.wedobooks.sdk.library.wedobookssdksampleapp.utils

import android.content.Context
import android.content.Intent
import io.wedobooks.sdk.library.wedobookssdksampleapp.services.AuthService

/** `WeDoBooksSdk.setup()` is one-shot per process, so a new environment needs a fresh one. */
fun Context.restartApp() {
    AuthService.instance.logout()

    val intent = packageManager.getLaunchIntentForPackage(packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    startActivity(intent)

    Runtime.getRuntime().exit(0)
}
