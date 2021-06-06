package polyrhythmmania.editor

import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyVar


class TrackView {
    val beat: FloatVar = FloatVar(0f)
    val renderScale: FloatVar = FloatVar(1f)
    val pxPerBeat: ReadOnlyVar<Float> = FloatVar {
        72f * renderScale.use()
    }
    
    fun translateBeatToX(beat: Float): Float {
        return (beat - this.beat.getOrCompute()) * this.pxPerBeat.getOrCompute()
    }
    
    fun translateXToBeat(x: Float): Float {
        return (x / this.pxPerBeat.getOrCompute()) + this.beat.getOrCompute()
    }
}
