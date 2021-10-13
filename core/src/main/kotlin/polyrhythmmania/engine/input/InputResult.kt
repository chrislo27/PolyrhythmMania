package polyrhythmmania.engine.input

import kotlin.math.absoluteValue


interface InputResultLike {
    val perfectBeat: Float
    val expectedIndex: Int
    val inputType: InputType
}

/**
 * Represents one input and how accurate it was.
 *
 * Both [accuracyPercent] and [accuracySec] can be negative. [accuracyPercent] is in [-1, 1]. A value of 0
 * for both fields means it is a perfect input.
 * A negative value means the input was early, a positive value means it was late.
 */
data class InputResult(override val perfectBeat: Float, override val inputType: InputType,
                       val accuracyPercent: Float, val accuracySec: Float,
                       override val expectedIndex: Int)
    : InputResultLike {
    
    val inputScore: InputScore = run {
        val p = accuracySec.absoluteValue
        when {
            p <= InputThresholds.ACE_OFFSET -> InputScore.ACE
            p <= InputThresholds.GOOD_OFFSET -> InputScore.GOOD
            p <= InputThresholds.BARELY_OFFSET -> InputScore.BARELY
            else -> InputScore.MISS
        }
    }
}