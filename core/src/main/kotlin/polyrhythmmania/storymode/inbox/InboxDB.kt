package polyrhythmmania.storymode.inbox

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
        addItem(InboxItem.ContractDoc(Contracts["first"], 0))
        addItem(InboxItem.Memo("fp_explanation", 100))

        // FIXME debug contracts
        Contracts.contracts.forEach { (key, contract) ->
            addItem(InboxItem.ContractDoc(contract, 200, id = "debugcontr_${contract.id}"))
        }
    }

}