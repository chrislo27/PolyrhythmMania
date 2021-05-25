package io.github.chrislo27.paintbox.desktop

import com.beust.jcommander.Parameter


open class PaintboxArguments {

    @Parameter(names = ["--help", "-h", "-?"], description = "Prints this usage menu.", help = true)
    var printHelp: Boolean = false

    // -----------------------------------------------------------

    @Parameter(names = ["--fps"], description = "Manually sets the target FPS.")
    var fps: Int = 60
    
}