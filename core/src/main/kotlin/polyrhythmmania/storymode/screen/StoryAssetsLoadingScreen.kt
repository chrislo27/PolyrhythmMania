package polyrhythmmania.storymode.screen

import paintbox.Paintbox
import polyrhythmmania.PRManiaGame
import polyrhythmmania.solitaire.SolitaireAssets
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.gamemode.boss.pattern.BossPatterns


class StoryAssetsLoadingScreen(main: PRManiaGame, unloadAssets: Boolean, doAfterLoad: () -> Unit)
    : StoryLoadingScreen<Nothing?>(main, determineLoadAction(unloadAssets), { doAfterLoad() }) {
    
    companion object {
        private fun determineLoadAction(unloadAssets: Boolean): (Float) -> LoadResult<Nothing?>? {
            return { delta ->
                val done = if (unloadAssets) {
                    unloadAction(delta)
                } else {
                    loadAction(delta)
                }
                if (done) LoadResult.SIGNAL else null
            }
        }
        
        private fun loadAction(delta: Float): Boolean {
            return if (StoryAssets.load(delta) >= 1f) {
                SolitaireAssets.loadBlocking()
                BossPatterns

                true
            } else false
        }
        
        private fun unloadAction(delta: Float): Boolean {
            try {
                StoryAssets.unloadAllAssets()
            } catch (e: Exception) {
                Paintbox.LOGGER.error("Failed to unload story assets", throwable = e)
            }
            return true
        }
    }
    
    init {
        this.minimumShowTime = 0.25f
    }
    
}
