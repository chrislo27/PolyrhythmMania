package polyrhythmmania.sidemodes.endlessmode

import java.time.LocalDate


data class DailyChallengeScore(val date: LocalDate, val score: Int) {
    companion object {
        val ZERO: DailyChallengeScore = DailyChallengeScore(LocalDate.MIN, 0)
    }
}

data class EndlessHighScore(val seed: UInt, val score: Int)
