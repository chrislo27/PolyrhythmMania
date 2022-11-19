package polyrhythmmania.storymode.inbox.progression

import polyrhythmmania.storymode.inbox.InboxItemCompletion
import polyrhythmmania.storymode.inbox.InboxState


data class UnlockStage(
        val id: String,
        val unlockReqs: UnlockStageChecker,
        val requiredInboxItems: List<String>,
        val optionalInboxItems: List<String> = emptyList(),
        val minRequiredToComplete: Int = requiredInboxItems.size
) {
    
    companion object {
        fun singleItem(inboxItemID: String, unlockReqs: UnlockStageChecker, stageID: String = inboxItemID): UnlockStage {
            return UnlockStage(stageID, unlockReqs, listOf(inboxItemID))
        }
    }
    
    fun isCompleted(inboxState: InboxState): Boolean {
        val numReqCompleted: Int = requiredInboxItems.count { itemID ->
            val completion = inboxState.getItemState(itemID)?.completion ?: InboxItemCompletion.UNAVAILABLE
            completion.shouldCountAsCompleted()
        }
        return numReqCompleted >= minRequiredToComplete.coerceAtMost(requiredInboxItems.size)
    }
    
}