package dev.monolith.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DockFocusPolicyTest {
    @Test fun initialLaunchSelectsFirstVisibleApp() {
        assertEquals("BrowseHere", resolveDockFocusKey(listOf("BrowseHere", "Cx"), null))
    }

    @Test fun reorderingAfterFavoriteKeepsSelectedComponent() {
        assertEquals(
            "Cx",
            resolveDockFocusKey(listOf("Cx", "BrowseHere", "DUNE"), "Cx")
        )
    }

    @Test fun removingFavoriteFallsBackWhenOldComponentNotVisible() {
        assertEquals("BrowseHere", resolveDockFocusKey(listOf("BrowseHere", "DUNE"), "Cx"))
    }

    @Test fun emptyFavoritesLeavesNoDockTarget() {
        assertNull(resolveDockFocusKey(emptyList(), "Cx"))
        assertNull(resolveDockFocusKey(emptyList(), null))
    }
}
