package polyrhythmmania.engine.modifiers

import paintbox.binding.BooleanVar
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.engine.input.InputResult
import polyrhythmmania.engine.input.InputterListener


/**
 * Represents an engine modifier.
 */
abstract class ModifierModule(val parent: EngineModifiers) : InputterListener {
    
    protected val engine: Engine = parent.engine
    
    val enabled: BooleanVar = BooleanVar(false)
    

    /**
     * Resets this modifier's internal state to a blank state.
     * 
     * Does NOT change any settings.
     */
    abstract fun resetState()
    
    abstract fun engineUpdate(beat: Float, seconds: Float, deltaSec: Float)


    // InputterListener overrides

    override fun onMissed(inputter: EngineInputter, firstMiss: Boolean) {
    }

    override fun onInputResultHit(inputter: EngineInputter, result: InputResult, countsAsMiss: Boolean) {
    }
}
