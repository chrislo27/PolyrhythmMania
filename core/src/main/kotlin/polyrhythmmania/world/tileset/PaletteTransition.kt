package polyrhythmmania.world.tileset


/**
 * Defines temporal behaviour for palette changes and spotlight changes.
 */
data class PaletteTransition(
        val duration: Float,

        val pulseMode: Boolean,
        val reverse: Boolean,
) {

    companion object {
        val DEFAULT: PaletteTransition = PaletteTransition(0.5f, pulseMode = false, reverse = false)
    }

    fun translatePercentage(timePercentage: Float): Float {
        var per = timePercentage.coerceIn(0f, 1f)

        if (this.reverse) {
            per = 1f - per
        }
        if (this.pulseMode) {
            per = if (per <= 0.5f) {
                (per / 0.5f).coerceIn(0f, 1f)
            } else {
                1f - (((per - 0.5f) / 0.5f).coerceIn(0f, 1f))
            }
        }

        return per
    }

}
