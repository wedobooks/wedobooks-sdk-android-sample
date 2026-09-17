package io.wedobooks.sdk.library.wedobookssdksampleapp

import android.app.Application
import android.util.Log
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.library.wedobookssdksampleapp.environment.AppEnvironment
import io.wedobooks.sdk.library.wedobookssdksampleapp.storage.SampleStores
import io.wedobooks.sdk.models.WdbConfiguration
import io.wedobooks.sdk.models.WdbInternalProgressConfig
import io.wedobooks.sdk.models.WdbThemeConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val TAG = "SampleApplication"

class SampleApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        // MUST run before WeDoBooksSdk.setup(): the SDK is one-shot per
        // process, so the environment is fixed for this process's lifetime.
        AppEnvironment.init(applicationContext)
        SampleStores.init(applicationContext)

        WeDoBooksSdk.setup(
            context = applicationContext,
            config = WdbConfiguration(
                applicationId = BuildConfig.APPLICATION_ID,
                firebaseApiKey = Constants.SDK_API_KEY,
                firebaseProjectId = Constants.SDK_PROJECT_ID,
                firebaseAppId = Constants.SDK_APP_ID,
                readerApiKey = Constants.READER_API_KEY,
                readerApiSecret = Constants.READER_API_SECRET,
                internalProgressConfig = WdbInternalProgressConfig(
                    player = false,
                    reader = false,
                ),
                sdkMode = Constants.SDK_MODE,
            ),
            themeConfig = WdbThemeConfiguration
                .builder()
                .build()
        )

        // Observe SDK background errors (Firestore permission failures, etc.).
        // The SDK already prevents them from crashing the process; this lets
        // the host log them centrally and, in a real app, escalate to crash
        // reporting or surface a snackbar.
        WeDoBooksSdk.events.errorsFlow
            .onEach { error ->
                Log.e(TAG, "SDK background error", error)
            }
            .launchIn(applicationScope)
    }
}
