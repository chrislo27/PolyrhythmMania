package polyrhythmmania.engine.music


data class MusicVolume(val beat: Float, val width: Float, val newVolume: Int) {
    companion object {
        val MIN_VOLUME: Int = 0
        val MAX_VOLUME: Int = 300
    }
}