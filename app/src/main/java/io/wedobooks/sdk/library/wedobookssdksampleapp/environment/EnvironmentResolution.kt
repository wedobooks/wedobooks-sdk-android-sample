package io.wedobooks.sdk.library.wedobookssdksampleapp.environment

fun resolveEnvironment(
    all: List<SampleEnvironment>,
    savedId: String?,
): SampleEnvironment {
    require(all.isNotEmpty()) { "Environments.all must not be empty" }

    val duplicates = all.groupBy { it.id }.filterValues { it.size > 1 }.keys
    require(duplicates.isEmpty()) {
        "Environments.all has duplicate ids: ${duplicates.joinToString()}. " +
            "Each entry needs its own id."
    }

    return all.firstOrNull { it.id == savedId } ?: all.first()
}
