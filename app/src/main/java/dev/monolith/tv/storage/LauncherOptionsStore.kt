package dev.monolith.tv.storage

import android.content.Context
import dev.monolith.tv.catalog.LauncherOptions

/**
 * Preferences are versioned independently of favorites.
 * Existing favorites.v1 remains untouched when launcher options change.
 */
class LauncherOptionsStore(context: Context) {
    private val prefs = context.getSharedPreferences("monolith_tv_preferences", Context.MODE_PRIVATE)

    fun load(): LauncherOptions = LauncherOptions(
        compactGrid = prefs.getBoolean("compact_grid.v1", false),
        favoritesFirst = prefs.getBoolean("favorites_first.v1", true)
    )

    fun save(options: LauncherOptions) {
        prefs.edit()
            .putBoolean("compact_grid.v1", options.compactGrid)
            .putBoolean("favorites_first.v1", options.favoritesFirst)
            .apply()
    }
}
