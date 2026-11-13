package dev.monolith.tv.catalog

import java.util.Locale

enum class CatalogFilter { ALL, FAVORITES }

data class LauncherOptions(
    val compactGrid: Boolean = false,
    val favoritesFirst: Boolean = true
)

/** Deterministic ordering and filtering independent of UI or Android APIs. */
object AppOrder {
    fun <T> sort(
        items: List<T>,
        favorites: Set<String>,
        packageName: (T) -> String,
        title: (T) -> String,
        favoritesFirst: Boolean = true
    ): List<T> {
        val alphabetical = compareBy<T> { title(it).lowercase(Locale.ROOT) }
            .thenBy { packageName(it) }
        return if (favoritesFirst) {
            items.sortedWith(compareByDescending<T> { packageName(it) in favorites }
                .then(alphabetical))
        } else {
            items.sortedWith(alphabetical)
        }
    }

    fun <T> visible(
        items: List<T>,
        favorites: Set<String>,
        packageName: (T) -> String,
        title: (T) -> String,
        filter: CatalogFilter,
        favoritesFirst: Boolean
    ): List<T> {
        val source = if (filter == CatalogFilter.FAVORITES) {
            items.filter { packageName(it) in favorites }
        } else {
            items
        }
        return sort(source, favorites, packageName, title, favoritesFirst)
    }
}
