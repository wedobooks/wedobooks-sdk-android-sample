package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import io.wedobooks.sdk.library.wedobookssdksampleapp.R
import io.wedobooks.sdk.models.enums.MaterialType

internal fun MaterialType.iconRes(): Int = when (this) {
    MaterialType.Ebook -> R.drawable.ic_book
    MaterialType.Audiobook -> R.drawable.ic_audiobook
    MaterialType.Podcast -> R.drawable.ic_podcast
}
