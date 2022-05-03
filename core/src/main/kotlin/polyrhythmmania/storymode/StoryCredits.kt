package polyrhythmmania.storymode

import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import polyrhythmmania.Localization
import polyrhythmmania.credits.CreditsBase
import java.util.*


object StoryCredits : CreditsBase() {
    
    override val credits: Map<ReadOnlyVar<String>, List<ReadOnlyVar<String>>> = linkedMapOf(
            Localization.getVar("credits.programming") to listOf("chrislo27").toVars(),
            Localization.getVar("credits.graphicDesign") to abcSorted(
                    ""
            ).toVars(),
            Localization.getVar("credits.music") to listOf(
                    Var("GENERIC"), Localization.getVar("credits.rhSoundtrack")
            ),
            Localization.getVar("credits.qa") to abcSorted(
                    ""
            ).toVars() + listOf(
                    Localization.getVar("credits.tourneycord"),
            ),
            Localization.getVar("credits.specialThanks") to abcSorted(
                    "", // TODO possibly drop special thanks in favour of more detailed roles for SM
            ).toVars() + listOf(Localization.getVar("credits.projectDonators")),
    )
    
}
