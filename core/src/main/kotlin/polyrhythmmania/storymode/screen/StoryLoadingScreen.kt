package polyrhythmmania.storymode.screen

import paintbox.Paintbox
import polyrhythmmania.PRManiaGame
import polyrhythmmania.screen.SimpleLoadingScreen
import polyrhythmmania.storymode.StoryAssets


class StoryLoadingScreen(main: PRManiaGame, val unload: Boolean, val doAfterLoad: () -> Unit)
    : SimpleLoadingScreen(main) {

    private var isReadyToContinue: Boolean = false
    var delayBeforeContinuing: Float = 0.25f
    private var alreadyRanAfterLoad: Boolean = false
    
    override fun render(delta: Float) {
        super.render(delta)
        
        if (main.screen === this) { // Don't load while in a transition
            if (isReadyToContinue) {
                if (delayBeforeContinuing > 0f) {
                    delayBeforeContinuing -= delta
                } else {
                    if (!alreadyRanAfterLoad) {
                        alreadyRanAfterLoad = true
                        doAfterLoad()
                    }
                }
            } else {
                if (unload) {
                    try {
                        StoryAssets.unloadAllAssets()
                    } catch (e: Exception) {
                        Paintbox.LOGGER.error("Failed to unload story assets", throwable = e)
                    }
                    isReadyToContinue = true
                } else {
                    if (StoryAssets.load(delta) >= 1f) {
                        isReadyToContinue = true
                    }
                }
            }
        }
    }
    
}
