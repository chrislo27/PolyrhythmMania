package polyrhythmmania.storymode.inbox

import paintbox.Paintbox
import paintbox.binding.asReadOnlyVar
import polyrhythmmania.storymode.contract.Contracts


object InboxDB : InboxItems() {

    init {
        val toAdd = mutableListOf<InboxItem>()

        toAdd += InboxItem.Memo("first_memo0", "first_memo0".asReadOnlyVar())
        toAdd += InboxItem.ContractDoc(Contracts["tutorial1"])

        run {
            // FIXME debug contracts
            for ((id, contract) in Contracts.contracts) {
                if (id == "tutorial1") {
                    continue
                }
                toAdd += InboxItem.ContractDoc(contract, itemID = "debugcontr_${contract.id}")
            }
            Paintbox.LOGGER.debug("Added ${Contracts.contracts.size} debug contracts", "InboxDB")
        }
        
        this.setItems(toAdd)
    }

}
