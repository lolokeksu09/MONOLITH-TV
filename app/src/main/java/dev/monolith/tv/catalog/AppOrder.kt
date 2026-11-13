package dev.monolith.tv.catalog

import java.util.Locale

/**
 * Keeps favorites in front and sorts labels deterministically, without mutating
 * the original application records.
 */
object AppOrder {
    fun <T> sort(
        items: List<T>,
        favorites: Set<String>,
        packageName: (T) -> String,
        title: (T) -> String
    ): List<T> = items.sortedWith(
        compareByDescending<T> { packageName(it) in favorites }
            .thenBy { title(it).lowercase(Locale.ROOT) }
            .thenBy { packageName(it) }
    )
}

