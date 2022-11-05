package polyrhythmmania.storymode.inbox

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.storymode.contract.Contract


sealed class InboxItem(
        val id: String,

        val listingName: ReadOnlyVar<String>,
) {

    class Debug(id: String, listingName: String, val subtype: DebugSubtype, val description: String = "<no desc>")
        : InboxItem(id, ReadOnlyVar.const(listingName)) {
        
        enum class DebugSubtype {
            PROGRESSION_ADVANCER,
        }
    }

    class Memo(id: String, listingName: ReadOnlyVar<String>)
        : InboxItem(id, listingName)

    class ContractDoc(
            val contract: Contract, itemID: String = "contract_${contract.id}",
            listingName: ReadOnlyVar<String> = contract.name,
            val subtype: ContractSubtype = ContractSubtype.NORMAL,
    ) : InboxItem(itemID, listingName) {
        
        enum class ContractSubtype(val headingL10NKey: String) {
            NORMAL("inboxItem.contract.heading.normal"), 
            TRAINING("inboxItem.contract.heading.training"),
        }
    }

}
