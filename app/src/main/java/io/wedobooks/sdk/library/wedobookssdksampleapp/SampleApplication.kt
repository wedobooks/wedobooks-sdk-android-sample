package io.wedobooks.sdk.library.wedobookssdksampleapp

import android.app.Application
import io.wedobooks.sdk.WeDoBooksSDK
import io.wedobooks.sdk.models.WDBConfiguration
import io.wedobooks.sdk.models.WDBThemeConfiguration

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        WeDoBooksSDK.setup(
            context = applicationContext,
            config = WDBConfiguration(
                applicationId = BuildConfig.APPLICATION_ID,
                firebaseApiKey = Constants.SDK_API_KEY,
                firebaseProjectId = Constants.SDK_PROJECT_ID,
                firebaseAppId = Constants.SDK_APP_ID,
                readerApiKey = BuildConfig.READER_API_KEY,
                readerApiSecret = BuildConfig.READER_API_SECRET,
                useInternalProgressService = false
            ),
            themeConfig = WDBThemeConfiguration
                .builder()
                .build()
        )
    }
}
