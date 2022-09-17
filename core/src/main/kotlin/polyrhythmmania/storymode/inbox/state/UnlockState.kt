package polyrhythmmania.storymode.inbox.state

import java.time.LocalDateTime


sealed class UnlockState {
    
    object Unavailable : UnlockState()
    
    data class Available(val newIndicator: Boolean) : UnlockState()

    object Skipped : UnlockState()

    data class Completed(
            val stageCompletionData: StageCompletionData?
    ) : UnlockState() {
        
        data class StageCompletionData(
                /**
                 * First time clearing the stage, in UTC.
                 */
                val firstClearTime: LocalDateTime
        )
    }

}
