package polyrhythmmania.storymode.inbox.progression


fun interface UnlockStageChecker {
    
    companion object {
        fun alwaysUnlocked(): UnlockStageChecker = UnlockStageChecker { true }
        
        fun stageToBeCompleted(stageID: String): UnlockStageChecker {
            return UnlockStageChecker { progression -> 
                progression.getStageStateByID(stageID) == StageUnlockState.COMPLETED
            }
        }
        
        fun stageToBeUnlocked(stageID: String): UnlockStageChecker {
            return UnlockStageChecker { progression -> 
                progression.getStageStateByID(stageID) == StageUnlockState.UNLOCKED
            }
        }
    }
    
    fun testShouldBeUnlocked(progression: Progression): Boolean
    
}