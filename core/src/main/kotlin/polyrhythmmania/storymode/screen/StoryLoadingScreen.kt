package polyrhythmmania.storymode.screen

import paintbox.Paintbox
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import polyrhythmmania.PRManiaGame
import polyrhythmmania.screen.SimpleLoadingScreen
import polyrhythmmania.solitaire.SolitaireAssets
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.ui.LoadingIconRod


class StoryLoadingScreen(main: PRManiaGame, val unload: Boolean, val doAfterLoad: () -> Unit)
    : SimpleLoadingScreen(main) {

    private var isReadyToContinue: Boolean = false
    var delayBeforeContinuing: Float = 0.25f
    private var alreadyRanAfterLoad: Boolean = false
    
    init {
        this.textLabelLoading.text.set("")
        
        val pane = Pane().apply {
            this.margin.set(Insets(32f))

            this += LoadingIconRod().apply {
                this.bounds.height.set(64f)
                Anchor.BottomRight.configure(this)
                this.bindWidthToSelfHeight(multiplier = 192f / 128f)
            }
        }
        this.sceneRoot += pane
    }
    
    override fun render(delta: Float) {
        super.render(delta)
        
        if (main.screen === this) { // Don't load while in a transition
            delayBeforeContinuing -= delta
            if (isReadyToContinue) {
                if (delayBeforeContinuing <= 0f) {
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
                        SolitaireAssets.loadBlocking()
                        
                        isReadyToContinue = true
                    }
                }
            }
        }
    }
    
}
