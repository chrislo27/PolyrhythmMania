package polyrhythmmania.engine.input

class InputChallengeData {
    var restriction: InputTimingRestriction = InputTimingRestriction.NORMAL
    
    fun isInputScoreMiss(inputScore: InputScore): Boolean {
        if (inputScore == InputScore.MISS) return true
        if (restriction == InputTimingRestriction.ACES_ONLY && inputScore != InputScore.ACE) return true
        if (restriction == InputTimingRestriction.NO_BARELY && !(inputScore == InputScore.ACE || inputScore == InputScore.GOOD)) return true
        return false
    }
}