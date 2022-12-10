package polyrhythmmania.storymode.screen.title

import paintbox.binding.BooleanVar
import polyrhythmmania.PRManiaGame
import polyrhythmmania.storymode.StorySavefile
import polyrhythmmania.storymode.StorySession


class TitleLogic(val main: PRManiaGame, val storySession: StorySession) {

    val savefiles: List<StorySavefile.LoadedState> = (1..StorySavefile.NUM_FILES).map(StorySavefile.Companion::attemptLoad)

    val fullTitle: BooleanVar = BooleanVar(true)
    
}
