package polyrhythmmania.engine.modifiers


/**
 * Represents an engine modifier.
 */
interface ModifierModule {

    /**
     * Resets this modifier's internal state to a blank state.
     * 
     * Does NOT change any settings.
     */
    fun resetState()
    
}