package polyrhythmmania.editor.music


data class Summary(val min: Float, val max: Float, val rms: Float) {
    companion object {
        val ZERO: Summary = Summary(0f, 0f, 0f)
    }
}
