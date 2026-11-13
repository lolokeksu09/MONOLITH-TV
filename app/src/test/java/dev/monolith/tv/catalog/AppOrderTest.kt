package dev.monolith.tv.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class AppOrderTest {
    private data class Item(val pkg: String, val title: String)

    @Test
    fun favoriteFirstThenAlphabetic() {
        val items = listOf(
            Item("pkg.vk", "VK Video"),
            Item("pkg.dune", "Dune HD"),
            Item("pkg.youtube", "YouTube")
        )
        val result = AppOrder.sort(items, setOf("pkg.vk"), { it.pkg }, { it.title })
        assertEquals(listOf("pkg.vk", "pkg.dune", "pkg.youtube"), result.map { it.pkg })
    }

    @Test
    fun noFavoritesDoesNotMutateInput() {
        val items = listOf(Item("pkg.z", "Zeta"), Item("pkg.a", "Alpha"))
        val result = AppOrder.sort(items, emptySet(), { it.pkg }, { it.title })
        assertEquals(listOf("pkg.a", "pkg.z"), result.map { it.pkg })
        assertEquals("pkg.z", items.first().pkg)
    }
}

