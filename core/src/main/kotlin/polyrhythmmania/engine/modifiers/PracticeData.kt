package polyrhythmmania.engine.modifiers

import paintbox.binding.IntVar
import polyrhythmmania.engine.input.RequiredInput

class PracticeData : ModifierModule {
    
    // Settings
    /**
     * Note: This is reset to `false` when [resetState] is called.
     */
    var practiceModeEnabled: Boolean = false
    
    // Data
    val moreTimes: IntVar = IntVar(0)
    var requiredInputs: List<RequiredInput> = emptyList()
    var clearText: Float = 0f
    
    override fun resetState() {
        practiceModeEnabled = false
        requiredInputs = emptyList()
        moreTimes.set(0)
        clearText = 0f
    }
}
