package polyrhythmmania.screen.play.regular

import polyrhythmmania.library.score.LevelScoreAttempt

fun interface OnRankingRevealed {
    fun onRankingRevealed(lsa: LevelScoreAttempt, score: ScoreWithResults)
}