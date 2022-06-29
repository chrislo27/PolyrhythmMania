package polyrhythmmania.screen.play

import paintbox.binding.Var


/**
 * Results behaviour for normal non-story gameplay.
 */
@Deprecated("This will be replaced with a generic level end behaviour")
sealed class ResultsBehaviour {
    
    object NoResults : ResultsBehaviour()
    
    data class ShowResults(
            val onRankingRevealed: OnRankingRevealed?,

            val previousHighScore: PreviousHighScore,
    ) : ResultsBehaviour()

    sealed class PreviousHighScore {
        object None : PreviousHighScore()
        class NumberOnly(val previousHigh: Int) : PreviousHighScore()
        class Persisted(val scoreVar: Var<Int>) : PreviousHighScore()
    }
}