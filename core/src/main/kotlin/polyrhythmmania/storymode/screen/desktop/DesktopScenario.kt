package polyrhythmmania.storymode.screen.desktop

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

    fun updateProgression() {
        progression.updateUnlockStages(inboxState)
    }

    fun updateInboxItemAvailability() {
        progression.stages
                .filter { progression.getStageStateByID(it.id) != StageUnlockState.LOCKED }
                .forEach { stage ->
                    (stage.requiredInboxItems + stage.optionalInboxItems).forEach { itemID ->
                        val oldState = inboxState.getItemState(itemID) ?: InboxItemState.Unavailable
                        if (oldState == InboxItemState.Unavailable) {
                            inboxState.putItemState(itemID, InboxItemState.Available(true))
                        }
                    }
                }
    }

}
