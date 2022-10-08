package polyrhythmmania.storymode.inbox.state

import java.time.LocalDateTime


sealed class InboxItemUnlockState {
    
    object Unavailable : InboxItemUnlockState()
    
    data class Available(val newIndicator: Boolean) : InboxItemUnlockState()

    object Skipped : InboxItemUnlockState()

    data class Completed(
            val stageCompletionData: StageCompletionData? // TODO doesn't apply to memos
    ) : InboxItemUnlockState() {
        
        data class StageCompletionData(
                /**
                 * First time clearing the stage, in UTC.
                 */
                val firstClearTime: LocalDateTime
        )
    }

}
