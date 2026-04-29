package io.wedobooks.sdk.library.wedobookssdksampleapp

import android.app.Application
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.models.WdbConfiguration
import io.wedobooks.sdk.models.WdbInternalProgressConfig
import io.wedobooks.sdk.models.WdbThemeConfiguration

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        WeDoBooksSdk.setup(
            context = applicationContext,
            config = WdbConfiguration(
                applicationId = BuildConfig.APPLICATION_ID,
                firebaseApiKey = Constants.SDK_API_KEY,
                firebaseProjectId = Constants.SDK_PROJECT_ID,
                firebaseAppId = Constants.SDK_APP_ID,
                readerApiKey = BuildConfig.READER_API_KEY,
                readerApiSecret = BuildConfig.READER_API_SECRET,
                internalProgressConfig = WdbInternalProgressConfig(),
                sdkMode = // SdkMode.Library or SdkMode.Streaming,
            ),
            themeConfig = WdbThemeConfiguration
                .builder()
                .build()
        )
    }
}
