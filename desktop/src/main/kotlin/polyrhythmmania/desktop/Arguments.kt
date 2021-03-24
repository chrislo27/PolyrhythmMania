package polyrhythmmania.desktop

import com.beust.jcommander.Parameter

class Arguments {
    
    @Parameter(names = ["--help", "-h", "-?"], description = "Prints this usage menu.", help = true)
    var printHelp: Boolean = false
    
    // -----------------------------------------------------------
    
    
}