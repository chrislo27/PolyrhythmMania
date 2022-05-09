package polyrhythmmania.storymode.test.gamemode

import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.input.InputTimingRestriction


class TestStoryAcesOnlyGameMode(main: PRManiaGame) : TestStoryGameMode(main) {
    init {
        engine.inputter.inputChallenge.restriction = InputTimingRestriction.ACES_ONLY
    }
}