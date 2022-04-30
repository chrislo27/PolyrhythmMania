package polyrhythmmania.screen.play

import polyrhythmmania.gamemodes.AbstractEndlessMode


sealed class ResultsBehaviour {
    
    object NoResults : ResultsBehaviour()
    
    data class ShowResults(
            val onRankingRevealed: OnRankingRevealed?,
            /**
             * Note: No effect when sideMode is active and it is an [AbstractEndlessMode]
             */
            val previousHighScore: Int?
    ) : ResultsBehaviour()
}