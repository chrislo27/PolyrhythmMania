package polyrhythmmania.engine.input

import kotlin.math.absoluteValue


data class InputResult(val type: InputType, val accuracySec: Float, val accuracyPercent: Float) {
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