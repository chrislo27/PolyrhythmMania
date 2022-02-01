package polyrhythmmania.screen.play

import polyrhythmmania.engine.input.Score
import polyrhythmmania.library.score.LevelScoreAttempt

fun interface OnRankingRevealed {
    fun onRankingRevealed(lsa: LevelScoreAttempt, score: Score)
}