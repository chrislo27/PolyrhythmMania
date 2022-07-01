package polyrhythmmania.engine.modifiers

import paintbox.binding.BooleanVar
import polyrhythmmania.engine.input.InputterListener


/**
 * Represents an engine modifier.
 */
abstract class ModifierModule : InputterListener {
    
    val enabled: BooleanVar = BooleanVar(false)
    

    /**
     * Resets this modifier's internal state to a blank state.
     * 
     * Does NOT change any settings.
     */
    abstract fun resetState()
    
    abstract fun engineUpdate(beat: Float, seconds: Float, deltaSec: Float)
    
}
