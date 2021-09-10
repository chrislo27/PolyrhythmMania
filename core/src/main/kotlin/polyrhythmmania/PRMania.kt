package polyrhythmmania

import com.codahale.metrics.MetricRegistry
import paintbox.util.Version
import paintbox.util.WindowSize
import java.io.File


object PRMania {

    const val TITLE = "Polyrhythm Mania"
    const val GITHUB = "https://github.com/chrislo27/PolyrhythmMania"
    const val HOMEPAGE = "https://polyrhythmmania.rhre.dev"
    const val DONATE_LINK = "https://www.paypal.com/donate/?hosted_button_id=9JLGHKZNWLLQ8"
    val VERSION: Version = Version(1, 0, 3, "dev_20210909a")
    const val WIDTH: Int = 1280 //1080
    const val HEIGHT: Int = 720
    val DEFAULT_SIZE: WindowSize = WindowSize(WIDTH, HEIGHT)
    val MINIMUM_SIZE: WindowSize = WindowSize(1152, 648)
    
    var portableMode: Boolean = false
    var possiblyNewPortableMode: Boolean = false
    
    val MAIN_FOLDER: File by lazy {
        (if (portableMode) File(".polyrhythmmania/") else File(System.getProperty("user.home") + "/.polyrhythmmania/")).apply {
            mkdirs()
        }
    }
    val RECOVERY_FOLDER: File by lazy {
        MAIN_FOLDER.resolve("recovery/").apply {
            mkdirs()
        }
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
    
    val enableEarlyAccessMessage: Boolean = (VERSION.suffix.startsWith("dev") || VERSION.suffix.startsWith("beta"))
    val metrics: MetricRegistry = MetricRegistry()
    
    // Command line arguments
    var logMissingLocalizations: Boolean = false
    var dumpPackedSheets: Boolean = false
    var enableMetrics: Boolean = false
    
}