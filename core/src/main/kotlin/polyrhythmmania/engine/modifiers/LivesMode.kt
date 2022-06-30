package polyrhythmmania.engine.modifiers

import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.engine.input.InputResult


class LivesMode : ModifierModule() {

    // Settings

    // Data
    
    override fun resetState() {
        
    }
    
    
    override fun onMissed(inputter: EngineInputter, firstMiss: Boolean) {
    }

    override fun onInputResultHit(inputter: EngineInputter, result: InputResult, countsAsMiss: Boolean) {
    }
}
