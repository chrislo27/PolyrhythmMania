package polyrhythmmania.screen.play.pause

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.Localization


data class PauseOption(val text: ReadOnlyVar<String>, val enabled: Boolean, val action: () -> Unit) {
    
    constructor(localizationKey: String, enabled: Boolean, action: () -> Unit)
        : this(Localization.getVar(localizationKey), enabled, action)
    
}
