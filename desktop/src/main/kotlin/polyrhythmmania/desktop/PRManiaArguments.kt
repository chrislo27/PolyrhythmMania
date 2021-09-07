package polyrhythmmania.desktop

import com.beust.jcommander.Parameter
import paintbox.desktop.PaintboxArguments

class PRManiaArguments : PaintboxArguments() {

    @Parameter(names = ["--log-missing-localizations"], description = "Logs any missing localizations. Other locales are checked against the default properties file.")
    var logMissingLocalizations: Boolean = false

    @Parameter(names = ["--dump-packed-sheets"], description = "Dump a copy of every packed sheet to a temp folder.")
    var dumpPackedSheets: Boolean = false
    
    @Parameter(names = ["--portable-mode"], description = "The .polyrhythmmania/ directory will be local to the game executable instead of the user home.")
    var portableMode: Boolean = false
    
    @Parameter(names = ["--enable-metrics"], description = "Enables recording of certain program metrics. Used for debugging.")
    var enableMetrics: Boolean = false
    
}