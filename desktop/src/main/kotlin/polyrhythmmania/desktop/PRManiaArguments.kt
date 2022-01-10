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
    
    @Parameter(names = ["--audio-device-buffer-size"], description = "Sets the AudioDevice buffer size. Should be a power of two. Defaults to ${AudioDeviceSettings.DEFAULT_SIZE}.")
    var audioDeviceBufferSize: Int = AudioDeviceSettings.DEFAULT_SIZE
    
    @Parameter(names = ["--audio-device-buffer-count"], description = "Sets the AudioDevice buffer count. Defaults to ${AudioDeviceSettings.DEFAULT_COUNT}.")
    var audioDeviceBufferCount: Int = AudioDeviceSettings.DEFAULT_COUNT
    
}