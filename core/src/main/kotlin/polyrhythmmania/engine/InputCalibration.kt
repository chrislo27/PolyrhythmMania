package polyrhythmmania.engine


data class InputCalibration(val audioOffsetMs: Float, val disableInputSounds: Boolean) {
    
    companion object {
        val NONE: InputCalibration = InputCalibration(0f, false)
    }
}
