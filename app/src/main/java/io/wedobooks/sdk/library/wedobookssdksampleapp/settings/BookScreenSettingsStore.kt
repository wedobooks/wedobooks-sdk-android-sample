package io.wedobooks.sdk.library.wedobookssdksampleapp.settings

import io.wedobooks.sdk.models.enums.WdbReaderBrightnessMode
import io.wedobooks.sdk.library.wedobookssdksampleapp.storage.KeyValueStore
import org.json.JSONObject

class BookScreenSettingsStore(private val store: KeyValueStore) {

    fun load(): BookScreenSettings {
        val defaults = BookScreenSettings()
        val raw = store.getString(KEY)
        if (raw.isNullOrBlank()) return defaults

        val json = JSONObject(raw)
        return BookScreenSettings(
            showPlayerMinimizeButton = json.optBoolean(
                PLAYER_MINIMIZE,
                defaults.showPlayerMinimizeButton
            ),
            showPlayerFinishButton = json.optBoolean(
                PLAYER_FINISH,
                defaults.showPlayerFinishButton
            ),
            readerPullDownTop = json.optBoolean(READER_PULL_DOWN_TOP, defaults.readerPullDownTop),
            readerShowHeaderAnimation = json.optBoolean(READER_SHOW_HEADER_ANIMATION, defaults.readerShowHeaderAnimation),
            showReaderFinishButton = json.optBoolean(
                READER_FINISH,
                defaults.showReaderFinishButton
            ),
            readerBrightnessMode = WdbReaderBrightnessMode.entries
                .firstOrNull { it.name == json.optString(READER_BRIGHTNESS_MODE) }
                ?: defaults.readerBrightnessMode,
            rememberReaderBrightness = json.optBoolean(
                READER_REMEMBER_BRIGHTNESS,
                defaults.rememberReaderBrightness,
            ),
            useLocalHighlightStorage = json.optBoolean(
                LOCAL_HIGHLIGHT_STORAGE,
                defaults.useLocalHighlightStorage,
            ),
        )
    }

    fun save(settings: BookScreenSettings) {
        val json = JSONObject()
            .put(PLAYER_MINIMIZE, settings.showPlayerMinimizeButton)
            .put(PLAYER_FINISH, settings.showPlayerFinishButton)
            .put(READER_PULL_DOWN_TOP, settings.readerPullDownTop)
            .put(READER_SHOW_HEADER_ANIMATION, settings.readerShowHeaderAnimation)
            .put(READER_FINISH, settings.showReaderFinishButton)
            .put(READER_BRIGHTNESS_MODE, settings.readerBrightnessMode.name)
            .put(READER_REMEMBER_BRIGHTNESS, settings.rememberReaderBrightness)
            .put(LOCAL_HIGHLIGHT_STORAGE, settings.useLocalHighlightStorage)
        store.putString(KEY, json.toString())
    }

    private companion object {
        const val KEY = "book_screen_settings"
        const val PLAYER_MINIMIZE = "player_minimize"
        const val PLAYER_FINISH = "player_finish"
        const val READER_PULL_DOWN_TOP = "reader_pull_down_top"
        const val READER_SHOW_HEADER_ANIMATION = "reader_show_header_animation"
        const val READER_FINISH = "reader_finish"
        const val READER_BRIGHTNESS_MODE = "reader_brightness_mode"
        const val READER_REMEMBER_BRIGHTNESS = "reader_remember_brightness"
        const val LOCAL_HIGHLIGHT_STORAGE = "local_highlight_storage"
    }
}
