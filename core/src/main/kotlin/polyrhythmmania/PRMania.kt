package polyrhythmmania

import paintbox.util.Version
import paintbox.util.WindowSize
import java.io.File


object PRMania {

    const val TITLE = "Polyrhythm Mania"
    const val GITHUB = "https://github.com/chrislo27/PolyrhythmMania"
    const val HOMEPAGE = "https://polyrhythmmania.rhre.dev"
    val VERSION: Version = Version(0, 6, 0, "alpha_20210718a")
    const val WIDTH: Int = 1280 //1080
    const val HEIGHT: Int = 720
    val DEFAULT_SIZE: WindowSize = WindowSize(WIDTH, HEIGHT)
    val MINIMUM_SIZE: WindowSize = WindowSize(1152, 648)
    val MAIN_FOLDER: File = File(System.getProperty("user.home") + "/.polyrhythmmania/").apply {
        mkdirs()
    }
    val RECOVERY_FOLDER: File = MAIN_FOLDER.resolve("recovery/").apply { 
        mkdirs()
    }
    val commonResolutions: List<WindowSize> = listOf(
            WindowSize(1152, 648),
            WindowSize(1280, 720),
            WindowSize(1366, 768),
            WindowSize(1600, 900),
            WindowSize(1920, 1080),
            WindowSize(2560, 1440),
            WindowSize(3840, 2160),
    ).sortedBy { it.width }
    
    val enableEarlyAccessMessage: Boolean = true
    
    // Command line arguments
    var logMissingLocalizations: Boolean = false
    var dumpPackedSheets: Boolean = false
    
}