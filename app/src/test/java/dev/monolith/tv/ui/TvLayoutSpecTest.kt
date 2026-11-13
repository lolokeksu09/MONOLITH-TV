package dev.monolith.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvLayoutSpecTest {
    @Test fun boxy1080FitsThreeCompactRowsAndKeepsFourColumns() {
        val spec = TvLayoutSpec.forViewport(960f, 540f, compactGrid = true)
        assertEquals(4, spec.columns)
        assertEquals(96, spec.tileDp)
        assertTrue(spec.galleryWidthDp(960f) > 680f)
        assertTrue(spec.threeCompactRowsFit(540f))
    }

    @Test fun boxy720PreservesThreeColumnsAndScrollableGrid() {
        val spec = TvLayoutSpec.forViewport(640f, 360f, compactGrid = true)
        assertEquals(3, spec.columns)
        assertEquals(96, spec.heroDp)
        assertEquals(88, spec.tileDp)
        assertTrue(spec.galleryWidthDp(640f) > 440f)
        assertFalse(spec.threeCompactRowsFit(360f))
    }

    @Test fun comfortableModeIncreasesTargetWithoutChangingNavigationColumns() {
        val compact = TvLayoutSpec.forViewport(960f, 540f, compactGrid = true)
        val comfortable = TvLayoutSpec.forViewport(960f, 540f, compactGrid = false)
        assertEquals(compact.columns, comfortable.columns)
        assertEquals(112, comfortable.tileDp)
        assertTrue(comfortable.tileDp > compact.tileDp)
    }

    @Test fun boundaryUsesNarrowRulesWithoutExcessiveRailWidth() {
        val compact = TvLayoutSpec.forViewport(799f, 480f, compactGrid = true)
        val wide = TvLayoutSpec.forViewport(800f, 480f, compactGrid = true)
        assertTrue(compact.narrow)
        assertFalse(wide.narrow)
        assertFalse(wide.short)
        assertTrue(compact.galleryWidthDp(799f) > 580f)
    }
}
