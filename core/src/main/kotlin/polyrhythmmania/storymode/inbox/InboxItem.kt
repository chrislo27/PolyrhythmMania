package polyrhythmmania.storymode.inbox

import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.storymode.contract.Prereq


sealed class InboxItem(
        val id: String,
        
        val fpPrereq: Int,
        val otherPrereqs: Set<Prereq> = emptySet(),
) {
    
    class IndexCard(id: String, fpPrereq: Int, otherPrereqs: Set<Prereq> = emptySet())
        : InboxItem(id, fpPrereq, otherPrereqs)
    
    class Memo(id: String, fpPrereq: Int, otherPrereqs: Set<Prereq> = emptySet())
        : InboxItem(id, fpPrereq, otherPrereqs)
    
    class ContractDoc(val contract: Contract, fpPrereq: Int, otherPrereqs: Set<Prereq> = emptySet(), id: String = "contract_${contract.id}") 
        : InboxItem(id, fpPrereq, otherPrereqs)
    
}
