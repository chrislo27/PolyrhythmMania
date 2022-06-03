package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.storymode.StoryL10N


data class Contract(
        val id: String,

        val name: ReadOnlyVar<String>,
        val desc: ReadOnlyVar<String>,
        val conditions: List<Condition>,

        val fpReward: Int,

        val requester: Requester,
        val jingleType: JingleType,
) {
    companion object {
        fun createWithAutofill(
                id: String, conditions: List<Condition>, fpReward: Int, requester: Requester, jingleType: JingleType,
        ): Contract {
            return Contract(id, StoryL10N.getVar("contract.name.$id"), StoryL10N.getVar("contract.desc.$id"),
                    conditions, fpReward, requester, jingleType)
        }
    }
}
