package polyrhythmmania.engine.input.score

import kotlin.math.roundToInt


interface IScore {

    val scoreRaw: Float
    val scoreInt: Int
        get() = scoreRaw.roundToInt().coerceIn(0, 100)

    val ranking: Ranking
        get() = Ranking.getRanking(scoreInt)
    
    val inputsHit: Int
    val nInputs: Int

    /**
     * Whether or not the player got a no miss.
     * Note that this is NOT `inputsHit < nInputs`, as the miss condition can be triggered in other ways depending on the game mode.
     */
    val noMiss: Boolean

    /**
     * If null, there was no Skill Star. Otherwise, indicates whether or not the player got the Skill Star.
     */
    val skillStar: Boolean?
    
}