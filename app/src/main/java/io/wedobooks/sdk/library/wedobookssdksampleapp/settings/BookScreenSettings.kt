package io.wedobooks.sdk.library.wedobookssdksampleapp.settings

import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.library.wedobookssdksampleapp.storage.SampleStores
import io.wedobooks.sdk.models.enums.WdbReaderBrightnessMode

data class BookScreenSettings(
    val showPlayerMinimizeButton: Boolean = true,
    val showPlayerFinishButton: Boolean = true,
    val readerPullDownTop: Boolean = true,
    val readerShowHeaderAnimation: Boolean = true,
    val showReaderFinishButton: Boolean = true,
    val readerBrightnessMode: WdbReaderBrightnessMode = WdbReaderBrightnessMode.Window,
    val rememberReaderBrightness: Boolean = false,
    // Off: the reader uses the SDK's own highlight storage instead of the local copy.
    val useLocalHighlightStorage: Boolean = true,
)

/** The SDK does not persist these, so the app applies its saved copy on every launch. */
fun BookScreenSettings.applyToSdk() {
    val configuration = WeDoBooksSdk.configuration
    configuration.setPlayerConfiguration(
        configuration.playerConfiguration.copy(
            showMinimizeButton = showPlayerMinimizeButton,
            showFinishBookButton = showPlayerFinishButton,
        )
    )
    configuration.setReaderConfiguration(
        configuration.readerConfiguration.copy(
            pullDownTop = readerPullDownTop,
            showHeaderAnimation = readerShowHeaderAnimation,
            showFinishBookButton = showReaderFinishButton,
            brightnessMode = readerBrightnessMode,
            rememberBrightness = rememberReaderBrightness,
            highlightCallback = SampleStores.localHighlights.takeIf { useLocalHighlightStorage },
        )
    )
}
