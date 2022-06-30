package polyrhythmmania.engine.modifiers

import paintbox.binding.IntVar
import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.engine.input.InputResult


class BarrageMode : ModifierModule() {
    
    // Settings
    val minimumNumber: IntVar = IntVar(0)
    
    
    // Data
    val currentScore: IntVar = IntVar(0)

    
    override fun resetState() {
        currentScore.set(0)
    }
    
    override fun onMissed(inputter: EngineInputter, firstMiss: Boolean) {
    }

    override fun onInputResultHit(inputter: EngineInputter, result: InputResult, countsAsMiss: Boolean) {
    }
}
