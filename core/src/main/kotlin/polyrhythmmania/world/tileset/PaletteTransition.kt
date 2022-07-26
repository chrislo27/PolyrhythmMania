package polyrhythmmania.world.tileset

import com.badlogic.gdx.math.Interpolation
import polyrhythmmania.editor.block.CubeTypeLike


/**
 * Defines temporal behaviour for palette changes and spotlight changes.
 */
data class PaletteTransition(
        val duration: Float,
        val transitionCurve: TransitionCurve,

        val pulseMode: Boolean,
        val reverse: Boolean,
) {

    companion object {
        val DEFAULT: PaletteTransition = PaletteTransition(duration = 0.5f, transitionCurve = TransitionCurve.LINEAR, pulseMode = false, reverse = false)
        val INSTANT: PaletteTransition = PaletteTransition(duration = 0f, transitionCurve = TransitionCurve.LINEAR, pulseMode = false, reverse = false)
    }

    /**
     * Translates the percentage of time into a percentage based on this transition's settings.
     * Also handles special interpolation curves if set.
     */
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

        return transitionCurve.interpolation.apply(0f, 1f, per)
    }

}

enum class TransitionCurve(
        override val jsonId: Int, override val localizationNameKey: String, val interpolation: Interpolation,
        val imageID: String
) : CubeTypeLike {

    LINEAR(0x0, "blockContextMenu.transitionCurve.linear", Interpolation.linear, "linear"),
    
    SMOOTH(0x1, "blockContextMenu.transitionCurve.smooth", Interpolation.smooth2, "smooth"),
    SMOOTHER(0x2, "blockContextMenu.transitionCurve.smoother", Interpolation.smoother, "smoother"),
    
    FAST_SLOW(0x3, "blockContextMenu.transitionCurve.fastSlow", Interpolation.fastSlow, "fast_slow"),
    SLOW_FAST(0x4, "blockContextMenu.transitionCurve.slowFast", Interpolation.slowFast, "slow_fast"),
    
    CIRCLE(0x10, "blockContextMenu.transitionCurve.circle", Interpolation.circle, "circle"),
    CIRCLE_IN(0x11, "blockContextMenu.transitionCurve.circleIn", Interpolation.circleIn, "circle_in"),
    CIRCLE_OUT(0x12, "blockContextMenu.transitionCurve.circleOut", Interpolation.circleOut, "circle_out"),
    POW5(0x20, "blockContextMenu.transitionCurve.pow5", Interpolation.pow5, "pow5"),
    POW5_IN(0x21, "blockContextMenu.transitionCurve.pow5In", Interpolation.pow5In, "pow5_in"),
    POW5_OUT(0x22, "blockContextMenu.transitionCurve.pow5Out", Interpolation.pow5Out, "pow5_out"),
    SINE(0x30, "blockContextMenu.transitionCurve.sine", Interpolation.sine, "sine"),
    SINE_IN(0x31, "blockContextMenu.transitionCurve.sineIn", Interpolation.sineIn, "sine_in"),
    SINE_OUT(0x32, "blockContextMenu.transitionCurve.sineOut", Interpolation.sineOut, "sine_out"),
    BOUNCE(0x40, "blockContextMenu.transitionCurve.bounce", Interpolation.bounce, "bounce"),
    BOUNCE_IN(0x41, "blockContextMenu.transitionCurve.bounceIn", Interpolation.bounceIn, "bounce_in"),
    BOUNCE_OUT(0x42, "blockContextMenu.transitionCurve.bounceOut", Interpolation.bounceOut, "bounce_out"),
    
    ;

    companion object {
        val VALUES: List<TransitionCurve> = TransitionCurve.values().toList()
        val INDEX_MAP: Map<Int, TransitionCurve> = VALUES.associateBy { it.jsonId }
    }
}
