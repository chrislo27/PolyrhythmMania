package polyrhythmmania.storymode.inbox

import polyrhythmmania.storymode.contract.Contract


sealed class InboxItem(
        val id: String,
        
        val unlockReqs: UnlockReqs,
) {
    
    class IndexCard(id: String, unlockReqs: UnlockReqs)
        : InboxItem(id, unlockReqs)
    
    class Memo(id: String, unlockReqs: UnlockReqs)
        : InboxItem(id, unlockReqs)
    
    class ContractDoc(val contract: Contract, unlockReqs: UnlockReqs, itemID: String = "contract_${contract.id}") 
        : InboxItem(itemID, unlockReqs)
    
}
