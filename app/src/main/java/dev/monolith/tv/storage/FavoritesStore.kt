package dev.monolith.tv.storage

import android.content.Context

/**
 * Preview settings key is stable across app upgrades with the same signing key.
 */
class FavoritesStore(context: Context) {
    private val preferences = context.getSharedPreferences("monolith_tv_preferences", Context.MODE_PRIVATE)

    fun load(): Set<String> =
        preferences.getStringSet("favorites.v1", emptySet())?.toSet().orEmpty()

    fun toggle(packageName: String): Set<String> {
        val next = load().toMutableSet()
        if (!next.add(packageName)) next.remove(packageName)
        preferences.edit().putStringSet("favorites.v1", next).apply()
        return next
    }
}

