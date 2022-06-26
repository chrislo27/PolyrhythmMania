package polyrhythmmania.engine.modifiers

import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.engine.input.InputResult
import polyrhythmmania.engine.input.InputterListener


class EngineModifiers(val engine: Engine) : InputterListener {
    
    private val inputter: EngineInputter = engine.inputter

    val perfectChallenge: PerfectChallengeData = PerfectChallengeData()
    val endlessScore: EndlessScore = EndlessScore()
    
    private val allModules: List<ModifierModule> = listOf(perfectChallenge, endlessScore)
 
    init {
        inputter.inputterListeners += this
    }
    
    fun resetState() {
        allModules.forEach(ModifierModule::resetState)
    }

    
    // InputterListener override functions
    
    override fun onMissed(inputter: EngineInputter, firstMiss: Boolean) {
        allModules.forEach { it.onMissed(inputter, firstMiss) }
    }

    override fun onInputResultHit(inputter: EngineInputter, result: InputResult, countsAsMiss: Boolean) {
        allModules.forEach { it.onInputResultHit(inputter, result, countsAsMiss) }
    }
}
