package polyrhythmmania.storymode.test.gamemode

import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.input.InputTimingRestriction


class TestStoryNoBarelyGameMode(main: PRManiaGame) : TestStoryGameMode(main) {
    init {
        engine.inputter.inputChallenge.restriction = InputTimingRestriction.NO_BARELY
    }
}