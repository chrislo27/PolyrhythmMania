package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar


data class Contract(
        val id: String,

        val name: ReadOnlyVar<String>,
        val desc: ReadOnlyVar<String>,

        val conditions: List<Condition>,

        val fpPrereq: Int,
        val fpReward: Int,
        val otherPrereqs: Set<Prereq> = emptySet(),
)
