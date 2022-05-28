package polyrhythmmania.storymode.inbox

import polyrhythmmania.storymode.contract.Prereq


sealed class InboxItem(
        val id: String,
        
        val fpPrereq: Int,
        val otherPrereqs: Set<Prereq> = emptySet(),
)
