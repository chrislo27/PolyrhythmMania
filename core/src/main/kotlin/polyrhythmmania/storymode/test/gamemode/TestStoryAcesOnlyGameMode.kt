package polyrhythmmania.storymode.test.gamemode

import polyrhythmmania.PRManiaGame


class TestStoryAcesOnlyGameMode(main: PRManiaGame) : TestStoryGameMode(main) {
    init {
        engine.inputter.inputChallenge.acesOnly = true
    }
}