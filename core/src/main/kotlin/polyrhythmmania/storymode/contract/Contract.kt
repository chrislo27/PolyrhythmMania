package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.PRManiaGame
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.storymode.StoryL10N


data class Contract(
        val id: String,

        override val name: ReadOnlyVar<String>,
        override val listingName: ReadOnlyVar<String>?,
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

        val gamemodeFactory: GamemodeFactory
) : IHasContractTextInfo {
    
    companion object {
        val DEFAULT_SKIP_TIME: Int = 3
        val NOT_ALLOWED_TO_SKIP: Int = -1
    }
    
    fun interface GamemodeFactory {
        fun load(delta: Float, main: PRManiaGame): GameMode?
    }
    
    fun interface GamemodeFactoryInstant : GamemodeFactory {
        fun load(main: PRManiaGame): GameMode?
        
        override fun load(delta: Float, main: PRManiaGame): GameMode? = load(main)
    }

    val immediatePass: Boolean get() = minimumScore <= 0
    val canSkipLevel: Boolean get() = skipAfterNFailures > 0
    
    constructor(
            id: String, requester: Requester, jingleType: JingleType, attribution: Attribution?,
            minimumScore: Int, extraConditions: List<Condition> = emptyList(), 
            skipAfterNFailures: Int = DEFAULT_SKIP_TIME,
            noListingName: Boolean = false,
            gamemodeFactory: GamemodeFactory
    ) : this(
            id, StoryL10N.getVar("contract.$id.name"), 
            if (noListingName) null else StoryL10N.getVar("contract.$id.listingName"),
            StoryL10N.getVar("contract.$id.desc"), StoryL10N.getVar("contract.$id.tagline"),
            requester, jingleType, attribution, minimumScore, extraConditions, skipAfterNFailures, gamemodeFactory
    )

    constructor(
            id: String, requester: Requester, jingleType: JingleType, attribution: Attribution?,
            minimumScore: Int, extraConditions: List<Condition> = emptyList(),
            skipAfterNFailures: Int = DEFAULT_SKIP_TIME,
            noListingName: Boolean = false,
            gamemodeFactory: GamemodeFactoryInstant
    ) : this(id, requester, jingleType, attribution, minimumScore, extraConditions, skipAfterNFailures, noListingName,
            gamemodeFactory as GamemodeFactory)

}
