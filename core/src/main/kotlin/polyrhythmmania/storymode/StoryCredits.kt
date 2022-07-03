package polyrhythmmania.storymode

import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import polyrhythmmania.Localization
import polyrhythmmania.credits.CreditsBase
import java.util.*


object StoryCredits : CreditsBase() {
    
    override val credits: Map<ReadOnlyVar<String>, List<ReadOnlyVar<String>>> = linkedMapOf(
            Localization.getVar("credits.story.levelCreators") to abcSorted(
                    "Dream Top", "Kievit", "J-D Thunder"
            ).toVars(),
    )
    
}
