package org.cascadebot.bot.utils

// Thank you to https://www.reddit.com/r/Kotlin/comments/65pv1f/comment/dgcfrz9/
inline fun <T> tryOrNull(f: () -> T) =
    try {
        f()
    } catch (_: Exception) {
        null
    }