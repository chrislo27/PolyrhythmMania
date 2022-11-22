package polyrhythmmania.screen.play.pause

import paintbox.binding.BooleanVar
import paintbox.binding.ReadOnlyVar
import polyrhythmmania.Localization


class PauseOption(val text: ReadOnlyVar<String>, enabled: Boolean, val action: () -> Unit) {
    
    val enabled: BooleanVar = BooleanVar(enabled)
    
    constructor(localizationKey: String, enabled: Boolean, action: () -> Unit)
        : this(Localization.getVar(localizationKey), enabled, action)
    
}
