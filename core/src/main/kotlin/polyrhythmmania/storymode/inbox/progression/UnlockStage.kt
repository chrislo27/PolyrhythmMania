package polyrhythmmania.storymode.inbox.progression

import polyrhythmmania.storymode.inbox.InboxItemState
import polyrhythmmania.storymode.inbox.InboxState


data class UnlockStage(
        val id: String,
        val unlockReqs: UnlockStageChecker,
        val requiredInboxItems: List<String>,
        val optionalInboxItems: List<String> = emptyList(),
        val minRequiredToComplete: Int = requiredInboxItems.size
) {
    
    companion object {
        fun singleItem(inboxItemID: String, unlockReqs: UnlockStageChecker): UnlockStage {
            return UnlockStage(inboxItemID, unlockReqs, listOf(inboxItemID))
        }
    }
    
    fun isCompleted(inboxState: InboxState): Boolean {
        val numReqCompleted: Int = requiredInboxItems.count { itemID ->
            val inboxItemState = inboxState.getItemState(itemID) ?: InboxItemState.Unavailable
            inboxItemState is InboxItemState.Completed || inboxItemState is InboxItemState.Skipped
        }
        return numReqCompleted >= minRequiredToComplete.coerceAtMost(requiredInboxItems.size)
    }
    
}