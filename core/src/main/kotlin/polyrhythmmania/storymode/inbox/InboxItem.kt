package polyrhythmmania.storymode.inbox

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.storymode.contract.Contract


sealed class InboxItem(
        val id: String,
        
        val listingName: ReadOnlyVar<String>,
) {
    
    class Memo(id: String, listingName: ReadOnlyVar<String>)
        : InboxItem(id, listingName)
    
    class ContractDoc(
            val contract: Contract, itemID: String = "contract_${contract.id}",
            listingName: ReadOnlyVar<String>? = null,
    ) : InboxItem(itemID, listingName ?: contract.name)

}
