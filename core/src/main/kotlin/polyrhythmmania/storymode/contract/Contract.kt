package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.PRManiaGame
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.storymode.StoryL10N


data class Contract(
        val id: String,

        val name: ReadOnlyVar<String>,
        val desc: ReadOnlyVar<String>,
        val tagline: ReadOnlyVar<String>,

        val conditions: List<Condition>,

        val requester: Requester,
        val jingleType: JingleType,
        /**
         * Minimum score to pass. If less than or equal to 0, the level is immediately passed when you get to the end.
         */
        val minimumScore: Int,

        val gamemodeFactory: (main: PRManiaGame) -> GameMode
) {

    val immediatePass: Boolean get() = minimumScore <= 0
    
    constructor(
            id: String, conditions: List<Condition>, requester: Requester, jingleType: JingleType, minimumScore: Int,
            gamemodeFactory: (main: PRManiaGame) -> GameMode
    ) : this(
            id, StoryL10N.getVar("contract.name.$id"), StoryL10N.getVar("contract.desc.$id"),
            StoryL10N.getVar("contract.tagline.$id"), conditions, requester, jingleType, minimumScore, gamemodeFactory
    )

}
