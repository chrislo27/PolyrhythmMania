package polyrhythmmania.engine.input

import com.badlogic.gdx.graphics.Color


data class Score(val scoreInt: Int, val scoreRaw: Float, val skillStar: Boolean, val noMiss: Boolean,
                 val title: String, val line1: String, val line2: String = "",
                 val ranking: Ranking = Ranking.getRanking(scoreInt)) {
    val butStillJustOk: Boolean = ranking == Ranking.OK && scoreInt >= 75
}

enum class Ranking(val sfx: String, val color: Color, val localization: String) {

    TRY_AGAIN("sfx_results_try_again", Color.valueOf("01BDFD"), "results.ranking.tryAgain"),
    OK("sfx_results_ok", Color.valueOf("00CD00"), "results.ranking.ok"),
    SUPERB("sfx_results_superb", Color.valueOf("FD0304"), "results.ranking.superb");

    companion object {
        fun getRanking(score: Int): Ranking = when {
            score in 60 until 80 -> OK
            score >= 80 -> SUPERB
            else -> TRY_AGAIN
        }
    }

}
