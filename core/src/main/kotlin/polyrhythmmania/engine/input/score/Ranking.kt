package polyrhythmmania.engine.input.score

import com.badlogic.gdx.graphics.Color
import polyrhythmmania.engine.input.score.Ranking.Companion.getRanking


/**
 * The three-tier ranking based on the score ([getRanking]).
 */
enum class Ranking(val sfxFile: String, val color: Color, val localization: String, val rankingIconID: String) {

    TRY_AGAIN(
            "sounds/results/results_try_again.ogg", 
            Color.valueOf("01BDFD"),
            "play.results.ranking.tryAgain",
            "try_again",
    ),
    OK(
            "sounds/results/results_ok.ogg",
            Color.valueOf("00CD00"),
            "play.results.ranking.ok",
            "ok",
    ),
    SUPERB(
            "sounds/results/results_superb.ogg",
            Color.valueOf("FD0304"),
            "play.results.ranking.superb",
            "superb",
    );

    companion object {
        fun getRanking(score: Int): Ranking = when {
            score in 60..<80 -> OK
            score >= 80 -> SUPERB
            else -> TRY_AGAIN
        }
    }
}
