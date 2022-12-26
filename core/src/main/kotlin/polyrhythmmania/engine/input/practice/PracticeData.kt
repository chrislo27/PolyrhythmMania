package polyrhythmmania.engine.input.practice

import paintbox.binding.IntVar

class PracticeData {
    
    /**
     * Note: This is reset to `false` when [reset] is called.
     */
    var practiceModeEnabled: Boolean = false
    
    val moreTimes: IntVar = IntVar(0)
    var requiredInputs: List<RequiredInput> = emptyList()
    var clearText: Float = 0f
        private set
    
    fun reset() {
        practiceModeEnabled = false
        requiredInputs = emptyList()
        moreTimes.set(0)
        clearText = 0f
    }
    
    fun triggerClearText() {
        clearText = 1f
    }
    
    fun updateClearText(deltaSec: Float) {
        clearText = (clearText - deltaSec / 1.5f).coerceAtLeast(0f)
    }
}
