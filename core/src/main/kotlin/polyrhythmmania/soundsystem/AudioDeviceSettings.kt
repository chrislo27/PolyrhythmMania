package polyrhythmmania.soundsystem


data class AudioDeviceSettings(val bufferSize: Int = DEFAULT_SIZE, val bufferCount: Int = DEFAULT_COUNT) {
    companion object {
        const val DEFAULT_SIZE = 1024
        const val DEFAULT_COUNT = 5
    }
}