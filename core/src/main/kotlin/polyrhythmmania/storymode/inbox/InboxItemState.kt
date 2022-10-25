package polyrhythmmania.storymode.inbox

import java.time.LocalDateTime


sealed class InboxItemState {
    
    object Unavailable : InboxItemState() {
        override fun shouldCountAsCompleted(): Boolean {
            return false
        }
    }
    
    data class Available(val newIndicator: Boolean) : InboxItemState() {
        override fun shouldCountAsCompleted(): Boolean {
            return false
        }
    }

    object Skipped : InboxItemState() {
        override fun shouldCountAsCompleted(): Boolean {
            return true
        }
    }

    data class Completed(
            val stageCompletionData: StageCompletionData?
    ) : InboxItemState() {
        
        data class StageCompletionData(
                /**
                 * First time clearing the stage, in UTC.
                 */
                val firstClearTime: LocalDateTime,
                
                // TODO put score-related things here; score, skill star, no miss etc
        )
        
        
        override fun shouldCountAsCompleted(): Boolean {
            return true
        }
    }
    
    abstract fun shouldCountAsCompleted(): Boolean

}
