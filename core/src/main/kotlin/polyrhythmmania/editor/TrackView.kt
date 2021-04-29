package polyrhythmmania.editor


class TrackView {
    var beat: Float = 0f
        set(value) {
            field = value.coerceAtLeast(0f)
        }
    var renderScale: Float = 1f
    val pxPerBeat: Float 
        get() = 128f * renderScale
    
    fun translateBeatToX(beat: Float): Float {
        return (beat - this.beat) * this.pxPerBeat
    }
    
    fun translateXToBeat(x: Float): Float {
        return (x / this.pxPerBeat) + this.beat
    }
}
