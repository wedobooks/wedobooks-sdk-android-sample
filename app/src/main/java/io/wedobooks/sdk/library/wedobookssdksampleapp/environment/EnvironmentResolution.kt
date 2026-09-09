package io.wedobooks.sdk.library.wedobookssdksampleapp.environment

/**
 * Picks the environment to run against.
 *
 * Falls back to the first entry when [savedId] is null or names an environment
 * that no longer exists, so a stale preference left behind by an older build
 * can never brick the app.
 */
fun resolveEnvironment(
    all: List<SampleEnvironment>,
    savedId: String?,
): SampleEnvironment {
    require(all.isNotEmpty()) { "Environments.all must not be empty" }

    // Ids namespace the per-environment stores (remembered UIDs, loaned
    // books) and identify the selection. Duplicates would silently share
    // that state and make the second entry unselectable, so fail loudly
    // here rather than let it look like the app is mixing environments up.
    val duplicates = all.groupBy { it.id }.filterValues { it.size > 1 }.keys
    require(duplicates.isEmpty()) {
        "Environments.all has duplicate ids: ${duplicates.joinToString()}. " +
            "Each entry needs its own id."
    }

    return all.firstOrNull { it.id == savedId } ?: all.first()
}
