package polyrhythmmania.storymode

import polyrhythmmania.PRManiaGame
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.storymode.screen.StoryAssetsLoadingScreen


/**
 * Represents where story mode assets are loaded. You have to enter a [StorySession] by loading assets,
 * and leave it by unloading all of them.
 */
class StorySession {
    
    fun renderUpdate() {
        GlobalStats.updateTotalStoryModePlayTime()
        // TODO if a save file is active, update its play time also (call StorySavefile.updatePlayTime)
    }
    
    fun createEntryLoadingScreen(main: PRManiaGame, doAfterLoad: () -> Unit): StoryAssetsLoadingScreen {
        return StoryAssetsLoadingScreen(main, false, doAfterLoad)
    }

    fun createExitLoadingScreen(main: PRManiaGame, doAfterUnload: () -> Unit): StoryAssetsLoadingScreen {
        return StoryAssetsLoadingScreen(main, true, doAfterUnload)
    }
}
