package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

/**
 * Formats a duration in seconds as "Hh Mm Ss", dropping leading zero units.
 * Examples: `0` → `"0s"`, `42` → `"42s"`, `125` → `"2m 5s"`, `3725` → `"1h 2m 5s"`.
 */
internal fun formatDuration(totalSeconds: Int): String {
    if (totalSeconds <= 0) return "0s"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return buildString {
        if (hours > 0) append("${hours}h ")
        if (hours > 0 || minutes > 0) append("${minutes}m ")
        append("${seconds}s")
    }.trim()
}

/** `1234` → `"1,234"`. Locale-independent thousands separator. */
internal fun formatThousands(value: Int): String {
    val s = value.toString()
    if (s.length <= 3) return s
    val negative = s.startsWith('-')
    val digits = if (negative) s.drop(1) else s
    val sb = StringBuilder()
    val mod = digits.length % 3
    digits.forEachIndexed { i, c ->
        if (i != 0 && i % 3 == mod) sb.append(',')
        sb.append(c)
    }
    return if (negative) "-$sb" else sb.toString()
}
