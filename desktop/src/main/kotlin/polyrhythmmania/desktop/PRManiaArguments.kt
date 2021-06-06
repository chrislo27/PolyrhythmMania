package polyrhythmmania.desktop

import com.beust.jcommander.Parameter
import paintbox.desktop.PaintboxArguments

class PRManiaArguments : PaintboxArguments() {

    @Parameter(names = ["--log-missing-localizations"], description = "Logs any missing localizations. Other locales are checked against the default properties file.")
    var logMissingLocalizations: Boolean = false
    
}