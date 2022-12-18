package polyrhythmmania.storymode.gamemode.boss

import polyrhythmmania.PRManiaGame
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.storymode.gamemode.AbstractStoryGameMode
import polyrhythmmania.storymode.music.StoryMusicAssets


class StoryBossGameMode(main: PRManiaGame)
    : AbstractStoryGameMode(main) {
    
    companion object {
        private const val INTRO_CARD_TIME: Float = 2.5f // Duration of intro segment
        const val BPM: Float = 186f
        
        fun getFactory(): Contract.GamemodeFactory = object : Contract.GamemodeFactory {
            private var firstCall = true
            
            override fun load(delta: Float, main: PRManiaGame): GameMode? {
                return if (firstCall) {
                    firstCall = false
                    StoryMusicAssets.initBossStems()
                    null
                } else {
                    val bossStems = StoryMusicAssets.bossStems
                    val keys = bossStems.keys
                    val ready = keys.all { key ->
                        val stem = bossStems[key]
                        stem?.musicFinishedLoading?.get() ?: false
                    }
                    
                    if (ready) StoryBossGameMode(main) else null
                }
            }
        }
    }
    
    override fun initialize() {
    }

    override fun getIntroCardTimeOverride(): Float {
        return INTRO_CARD_TIME
    }

    override fun getSecondsToDelayAtStartOverride(): Float {
        return 0.1f
    }

    override fun shouldPauseWhileInIntroCardOverride(): Boolean? {
        return false
    }
}
