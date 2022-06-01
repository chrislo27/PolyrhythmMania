package polyrhythmmania.storymode.inbox

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.storymode.contract.Condition
import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.storymode.contract.Contracts
import polyrhythmmania.storymode.contract.Requester


object InboxDB {
    
    val allItems: Map<String, InboxItem>
    val allFolders: Map<String, InboxFolder>

    init {
        this.allItems = mutableMapOf()
        this.allFolders = mutableMapOf()

        fun addItem(inboxItem: InboxItem) {
            this.allItems[inboxItem.id] = inboxItem
        }
        fun addFolder(chain: InboxFolder) {
            this.allFolders[chain.id] = chain
        }
        fun buildAndAddFolder(id: String, items: List<InboxItem>): InboxFolder {
            val chain = InboxFolder(id, items)
            items.forEach { addItem(it) }
            addFolder(chain)
            return chain
        }
        fun buildAndAddFolder(id: String, item: InboxItem) = buildAndAddFolder(id, listOf(item))

        buildAndAddFolder("test_indexcard", InboxItem.IndexCard("test_indexcard", -1))
//        buildAndAddFolder("test_memo", InboxItem.Memo("test_memo", 100))
//        buildAndAddFolder("test_contract", InboxItem.ContractDoc(Contract("test_contract", Requester.HR, ReadOnlyVar.const("Test Contract"), ReadOnlyVar.const("Test contract desc"), listOf(Condition.Debug("Condition 1"), Condition.Debug("Don't get chompulated")), 100), 0))
        
        buildAndAddFolder("first", listOf(
                InboxItem.Memo("first_memo0", 0),
                InboxItem.ContractDoc(Contracts["first"], 0),
        ))
        buildAndAddFolder("fp_explanation", InboxItem.Memo("fp_explanation", 100))
    }
    
}