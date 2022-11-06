package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.PRManiaGame
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.storymode.StoryL10N


data class Contract(
        val id: String,

        override val name: ReadOnlyVar<String>,
        override val desc: ReadOnlyVar<String>,
        override val tagline: ReadOnlyVar<String>,

        override val requester: Requester,
        val jingleType: JingleType,
        val attribution: Attribution?,
        /**
         * Minimum score to pass. If less than or equal to 0, the level is immediately passed when you get to the end.
         */
        val minimumScore: Int,

        val gamemodeFactory: (main: PRManiaGame) -> GameMode
) : IHasContractTextInfo {

    val immediatePass: Boolean get() = minimumScore <= 0
    
    constructor(
            id: String, requester: Requester, jingleType: JingleType, attribution: Attribution?,
            minimumScore: Int, gamemodeFactory: (main: PRManiaGame) -> GameMode
    ) : this(
            id, StoryL10N.getVar("contract.$id.name"), StoryL10N.getVar("contract.$id.desc"),
            StoryL10N.getVar("contract.$id.tagline"), requester, jingleType, attribution, minimumScore, gamemodeFactory
    )

}
