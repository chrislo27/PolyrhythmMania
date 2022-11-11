package polyrhythmmania.screen.play.regular

import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.engine.input.score.IScore
import polyrhythmmania.engine.input.score.Ranking
import polyrhythmmania.engine.input.score.ScoreBase


/**
 * A score data structure for the results.
 */
data class ScoreWithResults(
        val scoreBase: ScoreBase,
        
        val challenges: Challenges,
        val title: String, val line1: String, val line2: String = "",
        override val ranking: Ranking = scoreBase.ranking,
        
        val newHighScore: Boolean = false
) : IScore by scoreBase {
    
    val butStillJustOk: Boolean = ranking == Ranking.OK && scoreInt >= 75
    
}
