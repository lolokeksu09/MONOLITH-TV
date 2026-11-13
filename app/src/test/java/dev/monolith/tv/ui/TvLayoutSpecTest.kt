package dev.monolith.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvLayoutSpecTest {
    @Test fun boxy1080StageAndHorizontalDockFitWithoutVerticalClipping() {
        val spec = TvLayoutSpec.forViewport(960f, 540f, compactGrid = true)
        assertFalse(spec.narrow)
        assertFalse(spec.short)
        assertEquals(238, spec.stageDp)
        assertEquals(150, spec.shelfDp)
        assertEquals(130, spec.cardDp)
        assertTrue(spec.fitsViewport(540f))
        assertTrue(spec.shelfPeekWidthDp(960f) > 880f)
    }

    @Test fun boxy720UsesShortStageWithSameInteractionModel() {
        val spec = TvLayoutSpec.forViewport(640f, 360f, compactGrid = true)
        assertTrue(spec.narrow)
        assertTrue(spec.short)
        assertEquals(126, spec.stageDp)
        assertEquals(100, spec.shelfDp)
        assertEquals(47, spec.iconDp)
        assertTrue(spec.fitsViewport(360f))
    }

    @Test fun comfortableSettingIncreasesDockTargetWidthOnly() {
        val compact = TvLayoutSpec.forViewport(960f, 540f, true)
        val comfortable = TvLayoutSpec.forViewport(960f, 540f, false)
        assertEquals(compact.stageDp, comfortable.stageDp)
        assertEquals(compact.shelfDp, comfortable.shelfDp)
        assertTrue(comfortable.cardDp > compact.cardDp)
    }

    @Test fun narrowBreakpointPreservesHorizontalSafeArea() {
        val narrow = TvLayoutSpec.forViewport(759f, 540f, true)
        val wide = TvLayoutSpec.forViewport(760f, 540f, true)
        assertTrue(narrow.narrow)
        assertFalse(wide.narrow)
        assertTrue(narrow.shelfPeekWidthDp(759f) > 700f)
        assertTrue(wide.fitsViewport(540f))
    }
}
