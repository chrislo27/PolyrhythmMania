package polyrhythmmania.storymode.inbox

import paintbox.Paintbox
import polyrhythmmania.storymode.contract.Contracts
import polyrhythmmania.storymode.inbox.InboxItem.ContractDoc.ContractSubtype


object InboxDB : InboxItems() {

    init {
        val toAdd = mutableListOf<InboxItem>()

        toAdd += InboxItem.Memo("intern_memo1", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.ContractDoc(Contracts["tutorial1"], subtype = ContractSubtype.TRAINING)
        toAdd += InboxItem.Memo("intern_memo2", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.InfoMaterial("info_on_contracts", hasSeparateListingName = true)
        
        
        
        run {// FIXME debug contracts
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
