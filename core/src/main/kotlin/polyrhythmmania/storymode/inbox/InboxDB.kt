package polyrhythmmania.storymode.inbox

import paintbox.Paintbox
import polyrhythmmania.storymode.contract.Contracts


object InboxDB {
    
    val allItems: Map<String, InboxItem>

    init {
        this.allItems = mutableMapOf()

        fun addItem(inboxItem: InboxItem) {
            this.allItems[inboxItem.id] = inboxItem
        }

        addItem(InboxItem.Memo("first_memo0", UnlockReqs.ALWAYS_AVAILABLE))
        addItem(InboxItem.ContractDoc(Contracts["tutorial1"], UnlockReqs.ALWAYS_AVAILABLE))

        run {
            // FIXME debug contracts
            Contracts.contracts.forEach { (key, contract) ->
                addItem(InboxItem.ContractDoc(contract, UnlockReqs.ALWAYS_AVAILABLE, itemID = "debugcontr_${contract.id}"))
            }
            Paintbox.LOGGER.debug("Added ${Contracts.contracts.size} debug contracts", "InboxDB")
        }
    }

}