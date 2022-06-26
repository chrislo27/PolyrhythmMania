package polyrhythmmania.engine.input.practice

import polyrhythmmania.engine.input.InputScore
import polyrhythmmania.engine.input.InputType


data class RequiredInput(val beat: Float, val inputType: InputType) {
    
    var wasHit: Boolean = false
    var hitScore: InputScore = InputScore.MISS
    
}
