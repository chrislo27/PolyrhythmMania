package polyrhythmmania

import io.github.chrislo27.paintbox.util.Version
import io.github.chrislo27.paintbox.util.WindowSize
import java.io.File


object PRMania {

    const val TITLE = "Polyrhythm Mania"
    val VERSION: Version = Version(0, 1, 0, "alpha")
    const val WIDTH: Int = 1280 //1080
    const val HEIGHT: Int = 720
    val DEFAULT_SIZE: WindowSize = WindowSize(WIDTH, HEIGHT)
    val MINIMUM_SIZE: WindowSize = WindowSize(1152, 648)
    val MAIN_FOLDER: File = File(System.getProperty("user.home") + "/.polyrhythmmania/").apply {
        mkdirs()
    }
    
}