package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar


data class Contract(
        val id: String,

        val name: ReadOnlyVar<String>,
        val desc: ReadOnlyVar<String>,

        val conditions: List<Condition>,

        val fpReward: Int,
)
