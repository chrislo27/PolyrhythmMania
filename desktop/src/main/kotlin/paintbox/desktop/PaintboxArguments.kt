package paintbox.desktop

import com.beust.jcommander.Parameter


open class PaintboxArguments {

    @Parameter(names = ["--help", "-h", "-?"], description = "Prints this usage menu.", help = true)
    var printHelp: Boolean = false

    // -----------------------------------------------------------

    @Parameter(names = ["--fps"], description = "Manually sets the target FPS. Must be positive. If zero, the framerate is unbounded.")
    var fps: Int? = null
    
    @Parameter(names = ["--vsync"], description = "Enables/disables VSync (vertical sync).", arity = 1)
    var vsync: Boolean? = null
    
}