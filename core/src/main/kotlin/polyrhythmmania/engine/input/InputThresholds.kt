package polyrhythmmania.engine.input

import polyrhythmmania.screen.PlayScreen


object InputThresholds {
    val MAX_OFFSET_SEC: Float = 7f / 60
    val ACE_OFFSET: Float = 1f / 60
    val GOOD_OFFSET: Float = 3.5f / 60
    val BARELY_OFFSET: Float = 5f / 60

    /**
     * Forces the classloader to initialize the input-related classes to avoid possible stutter on the first input.
     */
    fun initInputClasses() {
        InputResult(0f, InputType.A, 0f, 0f, 0)
        InputScore.ACE
        Ranking.SUPERB
        Score(0, 0f, 0, 1, false, false, Challenges.NO_CHANGES, "", "")
        PlayScreen
    }
}