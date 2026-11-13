package dev.monolith.tv.ui

/**
 * Layout measurements in logical dp, kept independent of Compose to make
 * 1080p BOXY and short-height TV regressions testable on the JVM.
 *
 * The 0.1.4 screen is a full-width stage + horizontal app dock, not the
 * 0.1.3 sidebar + multi-row grid. The main viewport must never overflow.
 */
internal data class TvLayoutSpec(
    val narrow: Boolean,
    val short: Boolean,
    val edgeDp: Int,
    val verticalDp: Int,
    val headerDp: Int,
    val stageDp: Int,
    val sectionDp: Int,
    val shelfDp: Int,
    val cardDp: Int,
    val iconDp: Int,
    val headerGapDp: Int,
    val stageGapDp: Int,
    val shelfGapDp: Int
) {
    companion object {
        fun forViewport(widthDp: Float, heightDp: Float, compactGrid: Boolean): TvLayoutSpec {
            val narrow = widthDp < 760f
            val short = heightDp < 470f
            return TvLayoutSpec(
                narrow = narrow,
                short = short,
                edgeDp = if (narrow) 17 else 28,
                verticalDp = if (short) 12 else 18,
                headerDp = if (short) 42 else 46,
                stageDp = if (short) 126 else 238,
                sectionDp = if (short) 22 else 24,
                shelfDp = if (short) 100 else 150,
                cardDp = if (short) {
                    if (compactGrid) 106 else 124
                } else {
                    if (compactGrid) 130 else 148
                },
                iconDp = if (short) 47 else 70,
                headerGapDp = if (short) 7 else 12,
                stageGapDp = if (short) 9 else 12,
                shelfGapDp = if (short) 6 else 7
            )
        }
    }

    fun contentHeightDp(): Int = headerDp + stageDp + sectionDp + shelfDp +
        headerGapDp + stageGapDp + shelfGapDp + 2 * verticalDp

    fun fitsViewport(heightDp: Float): Boolean = contentHeightDp() <= heightDp

    fun shelfPeekWidthDp(widthDp: Float): Float = widthDp - 2 * edgeDp
}
