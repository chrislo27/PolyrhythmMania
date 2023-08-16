package polyrhythmmania.screen.play.regular

import paintbox.binding.Var


/**
 * Results behaviour for normal non-story gameplay. This is only used with [EnginePlayScreenBase].
 */
sealed class ResultsBehaviour {
    
    data object NoResults : ResultsBehaviour()
    
    data class ShowResults(
            val onRankingRevealed: OnRankingRevealed?, // Used to update GlobalScoreCache
            val previousHighScore: PreviousHighScore
    ) : ResultsBehaviour()

    sealed class PreviousHighScore {
        data object None : PreviousHighScore()
        class NumberOnly(val previousHigh: Int) : PreviousHighScore()
        class Persisted(val scoreVar: Var<Int>) : PreviousHighScore()
    }
}