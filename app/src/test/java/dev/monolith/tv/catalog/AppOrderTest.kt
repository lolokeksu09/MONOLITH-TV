package dev.monolith.tv.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOrderTest {
    private data class Item(val pkg: String, val title: String)
    private val items = listOf(
        Item("pkg.z", "Zeta"),
        Item("pkg.a", "Alpha"),
        Item("pkg.m", "Middle"),
        Item("pkg.d", "Delta")
    )

    @Test
    fun favoriteFirstThenAlphabetic() {
        val actual = AppOrder.sort(items, setOf("pkg.z"), { it.pkg }, { it.title })
        assertEquals(listOf("pkg.z", "pkg.a", "pkg.d", "pkg.m"), actual.map { it.pkg })
    }

    @Test
    fun noFavoritesDoesNotMutateInput() {
        val actual = AppOrder.sort(items, emptySet(), { it.pkg }, { it.title })
        assertEquals(listOf("pkg.a", "pkg.d", "pkg.m", "pkg.z"), actual.map { it.pkg })
        assertEquals("pkg.z", items.first().pkg)
    }

    @Test
    fun alphabeticalModeDoesNotMoveFavorite() {
        val actual = AppOrder.sort(
            items, setOf("pkg.z"), { it.pkg }, { it.title }, favoritesFirst = false
        )
        assertEquals(listOf("pkg.a", "pkg.d", "pkg.m", "pkg.z"), actual.map { it.pkg })
    }

    @Test
    fun favoritesOnlyKeepsEveryFavoriteAndSorts() {
        val actual = AppOrder.visible(
            items, setOf("pkg.z", "pkg.d"), { it.pkg }, { it.title },
            filter = CatalogFilter.FAVORITES, favoritesFirst = true
        )
        assertEquals(listOf("pkg.d", "pkg.z"), actual.map { it.pkg })
    }

    @Test
    fun removingLastFavoriteShowsEmptyFavoritesWithoutChangingSource() {
        val actual = AppOrder.visible(
            items, emptySet(), { it.pkg }, { it.title },
            filter = CatalogFilter.FAVORITES, favoritesFirst = true
        )
        assertTrue(actual.isEmpty())
        assertEquals(4, items.size)
    }

    @Test
    fun equalTitlesHaveStablePackageOrder() {
        val duplicates = listOf(Item("pkg.b", "Same"), Item("pkg.a", "same"))
        val actual = AppOrder.sort(duplicates, emptySet(), { it.pkg }, { it.title })
        assertEquals(listOf("pkg.a", "pkg.b"), actual.map { it.pkg })
    }
}
