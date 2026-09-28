package io.wedobooks.sdk.library.wedobookssdksampleapp

object Routes {
    val main = "main"
    val reader = "reader"
    val headlessAudio = "headless_audio"
    val wdbAudioPlayer = "audio_player"
    val login = "login"
    val stats = "stats"
    val downloadedBooks = "downloaded_books"
    val devices = "devices"

    const val ISBN_ARG = "isbn"

    val sampleEbook = "sample_ebook/{$ISBN_ARG}"
    val sampleAudiobook = "sample_audiobook/{$ISBN_ARG}"
    val headlessSampleAudio = "headless_sample_audio/{$ISBN_ARG}"
    val wdbSampleAudio = "wdb_sample_audio/{$ISBN_ARG}"

    fun sampleEbookRoute(isbn: String) = "sample_ebook/$isbn"
    fun sampleAudiobookRoute(isbn: String) = "sample_audiobook/$isbn"
    fun headlessSampleAudioRoute(isbn: String) = "headless_sample_audio/$isbn"
    fun wdbSampleAudioRoute(isbn: String) = "wdb_sample_audio/$isbn"
}
