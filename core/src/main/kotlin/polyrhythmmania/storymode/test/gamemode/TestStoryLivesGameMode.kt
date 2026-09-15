package polyrhythmmania.storymode.test.gamemode

import polyrhythmmania.PRManiaGame


class TestStoryLivesGameMode(main: PRManiaGame) : TestStoryGameMode(main) {
    init {
        engine.modifiers.livesMode.enabled.set(true)
        engine.modifiers.livesMode.maxLives.set(3)
        engine.modifiers.livesMode.resetState()
    }
}