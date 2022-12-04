package polyrhythmmania.storymode

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.Localization
import polyrhythmmania.credits.CreditsBase
import polyrhythmmania.storymode.contract.Contracts


object StoryCredits : CreditsBase() {

    override val credits: Map<ReadOnlyVar<String>, List<ReadOnlyVar<String>>> = linkedMapOf(
            Localization.getVar("credits.story.levelCreators") to getLevelCreators().toVars(),
            Localization.getVar("credits.story.writing") to abcSorted(
                    "sp00pster", "chrislo27", "J-D Thunder", "Conn", "garbo", "Luxury",
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
                            o1.second.compareTo(o2.second)
                        }.then { o1, o2 ->
                            o1.first.compareTo(o2.first)
                        }
                )
                .map { it.first }
    }
}
