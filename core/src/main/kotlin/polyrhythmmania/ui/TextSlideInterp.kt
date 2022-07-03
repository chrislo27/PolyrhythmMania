package polyrhythmmania.ui

import com.badlogic.gdx.math.Interpolation
import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyFloatVar


/**
 * Represents a slide interpolation using [time]. It is assumed that [time] counts down from 1.0 to 0.0.
 */
class TextSlideInterp(val time: ReadOnlyFloatVar) {

    val interpolationFrontHalf: Interpolation = Interpolation.ExpOut(2f, 5f)
    val interpolationBackHalf: Interpolation = Interpolation.ExpIn(2f, 5f)
    
    val textSlideAmount: ReadOnlyFloatVar = FloatVar {
        val timeIncreasing = 1f - time.use()
        if (timeIncreasing < 0.5f) {
            interpolationFrontHalf.apply(timeIncreasing / 0.5f) * 0.5f
        } else {
            0.5f + interpolationBackHalf.apply((timeIncreasing - 0.5f) / 0.5f) * 0.5f
        }
    }
    
}
