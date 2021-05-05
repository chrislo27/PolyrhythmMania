package polyrhythmmania.editor

import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.binding.ReadOnlyVar


class TrackView {
    val beat: FloatVar = FloatVar(0f)
    val renderScale: FloatVar = FloatVar(1f)
    val pxPerBeat: ReadOnlyVar<Float> = FloatVar {
        128f * renderScale.use()
    }
    
    fun translateBeatToX(beat: Float): Float {
        return (beat - this.beat.getOrCompute()) * this.pxPerBeat.getOrCompute()
    }
    
    fun translateXToBeat(x: Float): Float {
        return (x / this.pxPerBeat.getOrCompute()) + this.beat.getOrCompute()
    }
}
