package polyrhythmmania.engine


data class InputCalibration(val audioOffsetMs: Float) {
    companion object {
        val NONE: InputCalibration = InputCalibration(0f)
    }
}
