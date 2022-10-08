package polyrhythmmania.storymode.inbox

import paintbox.Paintbox
import paintbox.binding.asReadOnlyVar
import polyrhythmmania.storymode.contract.Contracts


object InboxDB {
    
    val allItems: Map<String, InboxItem>

    init {
        this.allItems = mutableMapOf()

        fun addItem(inboxItem: InboxItem) {
            this.allItems[inboxItem.id] = inboxItem
        }

        addItem(InboxItem.Memo("first_memo0", "first_memo0".asReadOnlyVar()))
        addItem(InboxItem.ContractDoc(Contracts["tutorial1"]))

        run {
            // FIXME debug contracts
            for ((id, contract) in Contracts.contracts) {
                if (id == "tutorial1") {
                    continue
                }
                addItem(InboxItem.ContractDoc(contract, itemID = "${contract.id}"))
            }
            Paintbox.LOGGER.debug("Added ${Contracts.contracts.size} debug contracts", "InboxDB")
        }
    }

}