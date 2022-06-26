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
    
    fun reset() {
        practiceModeEnabled = false
        requiredInputs = emptyList()
        moreTimes.set(0)
        clearText = 0f
    }
}
