package polyrhythmmania.editor


class TrackView {
    var beat: Float = 0f
        set(value) {
            field = value.coerceAtLeast(0f)
        }
    var renderScale: Float = 1f
    val pxPerBeat: Float 
        get() = 128f * renderScale
}
