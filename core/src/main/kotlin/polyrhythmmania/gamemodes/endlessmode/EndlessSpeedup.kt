package polyrhythmmania.gamemodes.endlessmode


/**
 * Maps positive integers to semitones, representing speed up values.
 */
object EndlessSpeedup {
    
    const val MAX_LEVEL: Int = 10

    fun map(level: Int): Int {
        return when {
            level == 0 -> 0
            level == 1 -> 2
            level == 2 -> 4
            level >= 3 -> 2 + level // lvl 3 -> 5, lvl 4 -> 6, lvl 5 -> 7, etc
            else -> level // Negatives not yet implemented
        }
    }
}
