package polyrhythmmania.storymode.screen



sealed class ExitReason {
    
    /**
     * The player quit the level 
     */
    object Quit : ExitReason() {
        override fun toString(): String = "Quit"
    }

    /**
     * The player is choosing to skip this level
     */
    object Skipped : ExitReason() {
        override fun toString(): String = "Skipped"
    }

    data class Passed(
            val score: Int, val skillStar: Boolean?, val noMiss: Boolean,
    ) : ExitReason() {
        
        fun isBetterThan(other: Passed): Boolean {
            return this.score > other.score || (this.skillStar == true && other.skillStar != true) || (this.noMiss && !other.noMiss)
        }
    }
    
}

/**
 * Called when the player leaves the [StoryPlayScreen] for any reason.
 */
fun interface ExitCallback {
    
    fun onExit(reason: ExitReason)
    
}
