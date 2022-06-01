package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.storymode.StoryL10N


data class Contract(
        val id: String,

        val requester: Requester,
        val name: ReadOnlyVar<String>,
        val desc: ReadOnlyVar<String>,

        val conditions: List<Condition>,

        val fpReward: Int,
) {
    companion object {
        fun createWithAutofill(id: String, requester: Requester, conditions: List<Condition>, fpReward: Int): Contract {
            return Contract(id, requester, StoryL10N.getVar("contract.name.$id"), StoryL10N.getVar("contract.desc.$id"),
                    conditions, fpReward)
        }
    }
}
