package polyrhythmmania.storymode

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.Localization
import polyrhythmmania.credits.CreditsBase
import polyrhythmmania.storymode.contract.Contracts


object StoryCredits : CreditsBase() {

    override val credits: Map<ReadOnlyVar<String>, List<ReadOnlyVar<String>>> = linkedMapOf(
        Localization.getVar("credits.programming") to listOf("chrislo27").toVars(),
        Localization.getVar("credits.graphicDesign") to abcSorted(
            "garbo", "snow krow", "GENERIC", "Luxury", "Kievit",
        ).toVars(),
        Localization.getVar("credits.story.music") to listOf(
            ReadOnlyVar.const("GENERIC"), Localization.getVar("credits.rhSoundtrack"),
        ),
        Localization.getVar("credits.story.levelCreators") to getLevelCreators().toVars(),
        Localization.getVar("credits.story.writing") to abcSorted(
            "sp00pster", "chrislo27", "J-D Thunder", "Conn", "garbo", "Luxury", "snow krow",
        ).toVars(),
        Localization.getVar("credits.qa.closedEarlyAccess") to listOf(Localization.getVar("credits.tourneycord")),
        Localization.getVar("credits.story.inspiration") to listOf(
            "Splatoon 2:\nOcto Expansion",
        ).toVars(),
    )

    /**
     * Returns all level creators sorted by level frequency and then alphabetical
     */
    private fun getLevelCreators(): List<String> {
        return Contracts.contracts.values
            .flatMap { it.attribution?.creators ?: emptyList() }
            .groupBy { it }
            .map { it.key to it.value.size }
            .filter { it.second > 0 && it.first.isNotBlank() }
            .sortedWith(
                Comparator<Pair<String, Int>> { o1, o2 ->
                    o2.second.compareTo(o1.second)
                }.then { o1, o2 ->
                    o1.first.compareTo(o2.first)
                }
            )
            .map { it.first }
    }
}
