package polyrhythmmania.engine.modifiers

import polyrhythmmania.engine.input.InputterListener


/**
 * Represents an engine modifier.
 */
interface ModifierModule : InputterListener {

    /**
     * Resets this modifier's internal state to a blank state.
     * 
     * Does NOT change any settings.
     */
    fun resetState()
    
}
