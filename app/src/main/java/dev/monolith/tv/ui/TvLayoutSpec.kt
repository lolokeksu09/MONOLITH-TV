package dev.monolith.tv.ui

/**
 * The TV viewport is measured in logical dp, not physical output pixels.
 * BOXY at 1920×1080 / 320dpi reports approximately 960×540dp.
 *
 * This function deliberately stays free of Android and Compose dependencies,
 * allowing the D-pad geometry to be regression-tested on the JVM.
 */
internal data class TvLayoutSpec(
    val narrow: Boolean,
    val short: Boolean,
    val columns: Int,
    val railDp: Int,
    val edgeDp: Int,
    val gapDp: Int,
    val heroDp: Int,
    val tileDp: Int,
    val tileGapDp: Int
) {
    companion object {
        fun forViewport(widthDp: Float, heightDp: Float, compactGrid: Boolean): TvLayoutSpec {
            val narrow = widthDp < 800f
            val short = heightDp < 480f
            return TvLayoutSpec(
                narrow = narrow,
                short = short,
                columns = if (narrow) 3 else 4,
                railDp = if (narrow) 138 else 166,
                edgeDp = if (narrow) 14 else 26,
                gapDp = if (narrow) 12 else 22,
                heroDp = if (short) 96 else 122,
                tileDp = if (short) {
                    if (compactGrid) 88 else 100
                } else {
                    if (compactGrid) 96 else 112
                },
                tileGapDp = if (short) 8 else 10
            )
        }
    }

    fun galleryWidthDp(widthDp: Float): Float =
        widthDp - 2 * edgeDp - railDp - gapDp

    fun threeCompactRowsFit(heightDp: Float): Boolean {
        val usable = heightDp - if (short) 28 else 44
        // Hero, two vertical gaps, apps heading and three compact rows.
        val content = heroDp + 12 + 28 + 8 + 3 * tileDp + 2 * tileGapDp + 14
        return compactRowHeightFits(usable, content)
    }

    private fun compactRowHeightFits(availableDp: Float, requestedDp: Int): Boolean =
        availableDp >= requestedDp
}
