package polyrhythmmania.storymode.inbox.progression

import polyrhythmmania.storymode.inbox.InboxState


fun interface UnlockStageChecker {
    
    companion object {
        fun alwaysUnlocked(): UnlockStageChecker = UnlockStageChecker { _, _ -> true }
        
        fun stageToBeCompleted(stageID: String): UnlockStageChecker {
            return UnlockStageChecker { progression, _ -> 
                progression.getStageStateByID(stageID) == StageUnlockState.COMPLETED
            }
        }
        
        fun stageToBeUnlocked(stageID: String): UnlockStageChecker {
            return UnlockStageChecker { progression, _ -> 
                progression.getStageStateByID(stageID) == StageUnlockState.UNLOCKED
            }
        }
    }
    
    fun testShouldStageBecomeUnlocked(progression: Progression, inboxState: InboxState): Boolean
    
}