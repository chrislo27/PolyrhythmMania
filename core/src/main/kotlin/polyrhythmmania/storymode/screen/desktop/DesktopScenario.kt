package polyrhythmmania.storymode.screen.desktop

import polyrhythmmania.storymode.StorySavefile
import polyrhythmmania.storymode.inbox.*
import polyrhythmmania.storymode.inbox.progression.Progression
import polyrhythmmania.storymode.inbox.progression.StageUnlockState
import polyrhythmmania.storymode.inbox.progression.UnlockStage


data class DesktopScenario(
        val inboxItems: InboxItems,
        val progression: Progression,
        val savefile: StorySavefile,
) {
    
    companion object {
        private fun createNewAvailableInboxItemState(): InboxItemState {
            return InboxItemState(completion = InboxItemCompletion.AVAILABLE, newIndicator = true)
        }
    }
    
    val inboxState: InboxState get() = savefile.inboxState

    
    fun updateProgression(): Map<StageUnlockState, List<UnlockStage>> {
        return progression.updateUnlockStages(inboxState)
    }

    /**
     * Returns a list of inbox item IDs that became available
     */
    fun checkItemsThatWillBecomeAvailable(): List<InboxItem> {
        return progression.stages
                .filter { stage -> progression.getStageStateByID(stage.id) != StageUnlockState.LOCKED }
                .flatMap { stage ->
                    (stage.requiredInboxItems + stage.optionalInboxItems).mapNotNull { itemID ->
                        val oldStateCompletion = inboxState.getItemState(itemID)?.completion ?: InboxItemCompletion.UNAVAILABLE
                        if (oldStateCompletion == InboxItemCompletion.UNAVAILABLE) {
                            itemID
                        } else null
                    }
                }
                .distinct()
                .mapNotNull { k -> inboxItems.mapByID[k] }
    }

    fun updateInboxItemAvailability(newAvailable: List<InboxItem>) {
        newAvailable.forEach { item ->
            inboxState.putItemState(item, createNewAvailableInboxItemState())
        }
    }

}
