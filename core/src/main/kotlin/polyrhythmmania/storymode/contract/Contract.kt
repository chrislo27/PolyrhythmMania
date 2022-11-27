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
        val extraConditions: List<Condition> = emptyList(),

        /**
         * Number of failures to have before being allowed to skip. If non-positive, you cannot skip it.
         */
        val skipAfterNFailures: Int = DEFAULT_SKIP_TIME,

        val gamemodeFactory: (main: PRManiaGame) -> GameMode
) : IHasContractTextInfo {
    
    companion object {
        val DEFAULT_SKIP_TIME: Int = 3
        val NOT_ALLOWED_TO_SKIP: Int = -1
    }

    val immediatePass: Boolean get() = minimumScore <= 0
    val canSkipLevel: Boolean get() = skipAfterNFailures > 0
    
    constructor(
            id: String, requester: Requester, jingleType: JingleType, attribution: Attribution?,
            minimumScore: Int, extraConditions: List<Condition> = emptyList(), 
            skipAfterNFailures: Int = DEFAULT_SKIP_TIME, gamemodeFactory: (main: PRManiaGame) -> GameMode
    ) : this(
            id, StoryL10N.getVar("contract.$id.name"), StoryL10N.getVar("contract.$id.desc"),
            StoryL10N.getVar("contract.$id.tagline"), requester, jingleType, attribution, minimumScore, extraConditions,
            skipAfterNFailures, gamemodeFactory
    )

}
