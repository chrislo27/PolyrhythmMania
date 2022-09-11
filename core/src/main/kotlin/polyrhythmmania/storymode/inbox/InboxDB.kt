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

        addItem(InboxItem.IndexCard("test_indexcard", -1))

        addItem(InboxItem.Memo("first_memo0", 0))
        addItem(InboxItem.ContractDoc(Contracts["tutorial1"], 0))

        run {
            // FIXME debug contracts
            Contracts.contracts.forEach { (key, contract) ->
                addItem(InboxItem.ContractDoc(contract, 200, id = "debugcontr_${contract.id}"))
            }
            Paintbox.LOGGER.debug("Added ${Contracts.contracts.size} debug contracts", "InboxDB")
        }
    }

}