package polyrhythmmania.desktop

import com.beust.jcommander.Parameter
import paintbox.desktop.PaintboxArguments
import polyrhythmmania.soundsystem.AudioDeviceSettings

class PRManiaArguments : PaintboxArguments() {

    @Parameter(names = ["--log-missing-localizations"], description = "Logs any missing localizations. Other locales are checked against the default properties file.")
    var logMissingLocalizations: Boolean = false

    @Parameter(names = ["--dump-packed-sheets"], description = "Dump a copy of every packed sheet to a temp folder.")
    var dumpPackedSheets: Boolean = false
    
    @Parameter(names = ["--portable-mode"], description = "The .polyrhythmmania/ directory will be local to the game executable instead of the user home.")
    var portableMode: Boolean = false
    
    @Parameter(names = ["--enable-metrics"], description = "Enables recording of certain program metrics. Used for debugging.")
    var enableMetrics: Boolean = false
    
    @Parameter(names = ["--audio-device-buffer-size"], description = "Overrides the AudioDevice buffer size when using the OpenAL sound system. Should be a power of two and at least ${AudioDeviceSettings.MINIMUM_SIZE}. On Windows, defaults to ${AudioDeviceSettings.DEFAULT_SIZE_WINDOWS}; ${AudioDeviceSettings.DEFAULT_SIZE} on other platforms.")
    var audioDeviceBufferSize: Int = AudioDeviceSettings.getDefaultBufferSize()
    
    @Parameter(names = ["--audio-device-buffer-count"], description = "Overrides the AudioDevice buffer count when using the OpenAL sound system. Should be at least ${AudioDeviceSettings.MINIMUM_COUNT}. On Windows, defaults to ${AudioDeviceSettings.DEFAULT_COUNT_WINDOWS}; ${AudioDeviceSettings.DEFAULT_COUNT} on other platforms.")
    var audioDeviceBufferCount: Int = AudioDeviceSettings.getDefaultBufferCount()
    
}