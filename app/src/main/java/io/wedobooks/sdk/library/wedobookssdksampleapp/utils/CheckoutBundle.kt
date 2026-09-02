package io.wedobooks.sdk.library.wedobookssdksampleapp.utils

import android.os.Bundle
import io.wedobooks.sdk.library.wedobookssdksampleapp.models.SampleCheckout
import io.wedobooks.sdk.models.Checkout
import io.wedobooks.sdk.models.enums.MaterialType
import java.time.Instant

private const val ARG_ID = "arg_checkout_id"
private const val ARG_USER_ID = "arg_user_id"
private const val ARG_ACTIVE = "arg_active"
private const val ARG_AUTHORS = "arg_authors"
private const val ARG_MATERIAL_ID = "arg_material_id"
private const val ARG_TITLE = "arg_title"
private const val ARG_TYPE = "arg_book_type"
private const val ARG_PUBLISHER = "arg_publisher"
private const val ARG_START_MS = "arg_start_ms"
private const val ARG_END_MS = "arg_end_ms"
private const val ARG_LAST_OPENED_MS = "arg_last_opened_ms"

/**
 * Serializes every [Checkout] field, so the copy rebuilt by [toCheckout] behaves like the
 * original: the SDK refuses to load a checkout whose `end` has passed, so a copy that drops
 * `end` is silently unplayable.
 */
fun Checkout.toBundle(): Bundle = Bundle().apply {
    putString(ARG_ID, id)
    putString(ARG_USER_ID, userId)
    putBoolean(ARG_ACTIVE, active)
    putStringArrayList(ARG_AUTHORS, ArrayList(author))
    putString(ARG_MATERIAL_ID, materialId)
    putString(ARG_TITLE, title)
    putString(ARG_TYPE, type.name)
    putString(ARG_PUBLISHER, publisher)
    putLong(ARG_START_MS, start.toEpochMilli())
    putLong(ARG_END_MS, end.toEpochMilli())
    lastOpenedAt?.let { putLong(ARG_LAST_OPENED_MS, it.toEpochMilli()) }
}

/** Rebuilds a [Checkout] written by [toBundle]. Returns `null` when a required field is missing. */
fun Bundle.toCheckout(): Checkout? {
    val id = getString(ARG_ID) ?: return null
    val materialId = getString(ARG_MATERIAL_ID) ?: return null
    val title = getString(ARG_TITLE) ?: return null
    val type = getString(ARG_TYPE)
        ?.let { name -> runCatching { MaterialType.valueOf(name) }.getOrNull() }
        ?: return null
    if (!containsKey(ARG_START_MS) || !containsKey(ARG_END_MS)) return null
    return SampleCheckout(
        id = id,
        userId = getString(ARG_USER_ID).orEmpty(),
        active = getBoolean(ARG_ACTIVE, true),
        author = getStringArrayList(ARG_AUTHORS)?.toList() ?: emptyList(),
        materialId = materialId,
        title = title,
        type = type,
        publisher = getString(ARG_PUBLISHER).orEmpty(),
        start = Instant.ofEpochMilli(getLong(ARG_START_MS)),
        end = Instant.ofEpochMilli(getLong(ARG_END_MS)),
        lastOpenedAt = if (containsKey(ARG_LAST_OPENED_MS)) {
            Instant.ofEpochMilli(getLong(ARG_LAST_OPENED_MS))
        } else {
            null
        },
    )
}
