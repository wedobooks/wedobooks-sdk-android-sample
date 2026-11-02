package io.wedobooks.sdk.library.wedobookssdksampleapp

import android.app.Application
import android.util.Log
import android.widget.Toast
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.library.wedobookssdksampleapp.environment.AppEnvironment
import io.wedobooks.sdk.library.wedobookssdksampleapp.settings.applyToSdk
import io.wedobooks.sdk.library.wedobookssdksampleapp.storage.SampleStores
import io.wedobooks.sdk.models.Checkout
import io.wedobooks.sdk.models.WdbConfiguration
import io.wedobooks.sdk.models.WdbInternalProgressConfig
import io.wedobooks.sdk.models.WdbPlayerConfiguration
import io.wedobooks.sdk.models.WdbReaderConfiguration
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
        SampleStores.userIds.seed(AppEnvironment.current.id, AppEnvironment.current.seedUserIds)
        SampleStores.books.seed(AppEnvironment.current.id, AppEnvironment.current.seedIsbns)

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
                    player = Constants.INTERNAL_PROGRESS_PLAYER,
                    reader = Constants.INTERNAL_PROGRESS_READER,
                ),
                sdkMode = Constants.SDK_MODE,
            ),
            themeConfig = WdbThemeConfiguration
                .builder()
                .build()
        )
        WeDoBooksSdk.configuration.setPlayerConfiguration(
            WdbPlayerConfiguration(
                onTappedMinimizeButton = { checkout -> reportBookScreenTap("Player minimized", checkout) },
                onTappedFinishBookButton = { checkout -> reportBookScreenTap("Player finish tapped", checkout) },
            )
        )
        WeDoBooksSdk.configuration.setReaderConfiguration(
            WdbReaderConfiguration(
                onTappedFinishBookButton = { checkout -> reportBookScreenTap("Reader finish tapped", checkout) },
            )
        )
        // Also installs the local highlight copy when that setting is on.
        SampleStores.bookScreenSettings.load().applyToSdk()

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

    private fun reportBookScreenTap(action: String, checkout: Checkout) {
        Log.d(TAG, "$action for checkoutId=${checkout.id}")
        Toast.makeText(this, "$action: ${checkout.title}", Toast.LENGTH_SHORT).show()
    }
}
