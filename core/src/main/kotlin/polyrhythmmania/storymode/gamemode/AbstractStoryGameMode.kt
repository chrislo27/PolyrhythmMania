package polyrhythmmania.storymode.gamemode

import polyrhythmmania.PRManiaGame
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.storymode.screen.StoryPlayScreen


abstract class AbstractStoryGameMode(main: PRManiaGame) : GameMode(main, null) {
    
    open fun getIntroCardTimeOverride(): Float? = null
    
    open fun prepareFirstTimeWithStoryPlayScreen(playScreen: StoryPlayScreen) {
        val introCardTimeOverride = this.getIntroCardTimeOverride()
        if (introCardTimeOverride != null){
            playScreen.introCardDuration = introCardTimeOverride
        }
    }

    open fun getSecondsToDelayAtStartOverride(): Float? = null
    open fun shouldPauseWhileInIntroCardOverride(): Boolean? = null
    
}