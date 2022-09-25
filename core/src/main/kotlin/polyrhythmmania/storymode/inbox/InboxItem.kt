package polyrhythmmania.storymode.inbox

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.storymode.contract.Contract


sealed class InboxItem(
        val id: String,
        
        val listingName: ReadOnlyVar<String>,
        val unlockReqs: UnlockReqs,
) {
    
    class Memo(id: String, listingName: ReadOnlyVar<String>, unlockReqs: UnlockReqs)
        : InboxItem(id, listingName, unlockReqs)
    
    class ContractDoc(
            val contract: Contract, unlockReqs: UnlockReqs, itemID: String = "contract_${contract.id}",
            listingName: ReadOnlyVar<String>? = null,
    ) : InboxItem(itemID, listingName ?: contract.name, unlockReqs)

}
