package polyrhythmmania.storymode.inbox.unlock

import polyrhythmmania.storymode.inbox.state.InboxItemUnlockState
import polyrhythmmania.storymode.inbox.state.InboxState


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
            val inboxItemState = inboxState.getItemState(itemID)
            val unlockState = inboxItemState?.unlockState ?: InboxItemUnlockState.Unavailable
            unlockState is InboxItemUnlockState.Completed || unlockState is InboxItemUnlockState.Skipped
        }
        return numReqCompleted >= minRequiredToComplete.coerceAtMost(requiredInboxItems.size)
    }
    
}