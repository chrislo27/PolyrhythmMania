package polyrhythmmania.storymode.screen.desktop

import polyrhythmmania.storymode.inbox.InboxItemCompletion
import polyrhythmmania.storymode.inbox.InboxItemState
import polyrhythmmania.storymode.inbox.InboxItems
import polyrhythmmania.storymode.inbox.InboxState
import polyrhythmmania.storymode.inbox.progression.Progression
import polyrhythmmania.storymode.inbox.progression.StageUnlockState


data class DesktopScenario(
        val inboxItems: InboxItems,
        val progression: Progression,
        val inboxState: InboxState,
) {
    
    companion object {
        private fun createNewInboxItemState(): InboxItemState = InboxItemState(completion = InboxItemCompletion.AVAILABLE, newIndicator = true)
    }

    fun updateProgression() {
        progression.updateUnlockStages(inboxState)
    }

    fun updateInboxItemAvailability() {
        progression.stages
                .filter { progression.getStageStateByID(it.id) != StageUnlockState.LOCKED }
                .forEach { stage ->
                    (stage.requiredInboxItems + stage.optionalInboxItems).forEach { itemID ->
                        val oldStateCompletion = inboxState.getItemState(itemID)?.completion ?: InboxItemCompletion.UNAVAILABLE
                        if (oldStateCompletion == InboxItemCompletion.UNAVAILABLE) {
                            inboxState.putItemState(itemID, createNewInboxItemState())
                        }
                    }
                }
    }

}
