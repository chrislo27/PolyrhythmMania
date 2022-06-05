package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.PRManiaGame
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.storymode.StoryL10N


data class Contract(
        val id: String,

        val name: ReadOnlyVar<String>,
        val desc: ReadOnlyVar<String>,
        val conditions: List<Condition>,

        val fpReward: Int,

        val requester: Requester,
        val jingleType: JingleType,
        
        val gamemodeFactory: (main: PRManiaGame) -> GameMode
) {

    constructor(
            id: String, conditions: List<Condition>, fpReward: Int, requester: Requester, jingleType: JingleType,
            gamemodeFactory: (main: PRManiaGame) -> GameMode
    ) : this(
            id, StoryL10N.getVar("contract.name.$id"), StoryL10N.getVar("contract.desc.$id"), conditions, fpReward,
            requester, jingleType,
            gamemodeFactory
    )

}
