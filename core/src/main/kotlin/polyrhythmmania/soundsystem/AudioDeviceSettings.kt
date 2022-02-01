package polyrhythmmania.soundsystem

import paintbox.util.SystemUtils


data class AudioDeviceSettings(val bufferSize: Int, val bufferCount: Int) {
    companion object {
        const val DEFAULT_SIZE_WINDOWS: Int = 1024
        const val DEFAULT_COUNT_WINDOWS: Int = 6
        const val DEFAULT_SIZE: Int = 1024
        const val DEFAULT_COUNT: Int = 10
        
        const val MINIMUM_SIZE: Int = 256
        const val MINIMUM_COUNT: Int = 3
        
        val DEFAULT_SETTINGS: AudioDeviceSettings = AudioDeviceSettings(getDefaultBufferSize(), getDefaultBufferCount())
        
        fun getDefaultBufferSize(): Int {
            return if (SystemUtils.isWindows()) DEFAULT_SIZE_WINDOWS else DEFAULT_SIZE
        }
        
        fun getDefaultBufferCount(): Int {
            return if (SystemUtils.isWindows()) DEFAULT_COUNT_WINDOWS else DEFAULT_COUNT
        }
    }
}