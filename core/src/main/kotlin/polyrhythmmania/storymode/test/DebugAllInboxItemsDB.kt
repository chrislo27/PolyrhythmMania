package polyrhythmmania.storymode.test

import polyrhythmmania.storymode.contract.Contracts
import polyrhythmmania.storymode.inbox.InboxDB
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.InboxItems


object DebugAllInboxItemsDB : InboxItems() {

    init {
        val toAdd = mutableListOf<InboxItem>()

        toAdd.addAll(InboxDB().items)
        toAdd.addAll(Contracts.DebugInboxItems.items)

        this.setItems(toAdd)
    }

}
