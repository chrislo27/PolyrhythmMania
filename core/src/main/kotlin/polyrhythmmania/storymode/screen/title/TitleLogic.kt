package polyrhythmmania.storymode.screen.title

import paintbox.binding.BooleanVar
import paintbox.binding.Var
import polyrhythmmania.PRManiaGame
import polyrhythmmania.storymode.StorySavefile
import polyrhythmmania.storymode.StorySession


class TitleLogic(val main: PRManiaGame, val storySession: StorySession) {

    val savefiles: List<Var<StorySavefile.LoadedState>> = (1..StorySavefile.NUM_FILES).map { idx ->
        Var(StorySavefile.attemptLoad(idx))
    }

    val fullTitle: BooleanVar = BooleanVar(true)
    
}
