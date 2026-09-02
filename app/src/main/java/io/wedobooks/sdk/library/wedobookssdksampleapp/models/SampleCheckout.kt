package io.wedobooks.sdk.library.wedobookssdksampleapp.models

import io.wedobooks.sdk.models.Checkout
import io.wedobooks.sdk.models.enums.MaterialType
import java.time.Instant

/** [Checkout] carried across the media-session boundary, where the SDK's own model can't travel. */
data class SampleCheckout(
    override val id: String,
    override val userId: String,
    override val active: Boolean,
    override val author: List<String>,
    override val materialId: String,
    override val title: String,
    override val type: MaterialType,
    override val publisher: String,
    override val start: Instant,
    override val end: Instant,
    override val lastOpenedAt: Instant?,
) : Checkout
