package polyrhythmmania.engine.input

class InputChallengeData {

    var restriction: InputTimingRestriction = InputTimingRestriction.NORMAL

    fun isInputScoreMiss(inputScore: InputScore): Boolean {
        val restriction = this.restriction
        return when {
            inputScore == InputScore.MISS -> true
            restriction == InputTimingRestriction.ACES_ONLY && inputScore != InputScore.ACE -> true
            restriction == InputTimingRestriction.NO_BARELY && !(inputScore == InputScore.ACE || inputScore == InputScore.GOOD) -> true
            else -> false
        }
    }

    fun isInputScoreMissFromRestriction(inputScore: InputScore): Boolean {
        if (inputScore == InputScore.MISS) return false

        return restriction != InputTimingRestriction.NORMAL && isInputScoreMiss(inputScore)
    }
}