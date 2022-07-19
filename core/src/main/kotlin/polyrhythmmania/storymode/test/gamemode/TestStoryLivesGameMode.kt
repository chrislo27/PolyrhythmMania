package polyrhythmmania.storymode.test.gamemode

import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.block.BlockDeployRod
import polyrhythmmania.editor.block.BlockDespawnPattern
import polyrhythmmania.editor.block.storymode.BlockDeployRodStoryMode
import polyrhythmmania.engine.input.InputTimingRestriction


class TestStoryLivesGameMode(main: PRManiaGame) : TestStoryGameMode(main) {
    init {
        engine.modifiers.livesMode.enabled.set(true)
        engine.modifiers.livesMode.maxLives.set(3)
        engine.modifiers.livesMode.resetState()
    }
}