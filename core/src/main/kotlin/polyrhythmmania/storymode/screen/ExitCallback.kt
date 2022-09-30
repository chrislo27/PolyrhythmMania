package polyrhythmmania.storymode.screen



sealed class ExitReason {
    
    /**
     * The player quit the level 
     */
    object Quit : ExitReason()

    /**
     * The player is choosing to skip this level
     */
    object Skipped : ExitReason()
    
    data class Passed(val score: Int) : ExitReason()
    
}

/**
 * Called when the player leaves the [StoryPlayScreen] for any reason.
 */
fun interface ExitCallback {
    
    fun onExit(reason: ExitReason)
    
}
