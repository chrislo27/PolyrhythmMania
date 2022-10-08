package polyrhythmmania.storymode.inbox.unlock


fun interface UnlockStageChecker {
    
    companion object {
        fun stageCompleted(stageID: String): UnlockStageChecker {
            return UnlockStageChecker { progression -> 
                progression.getStageStateByID(stageID) == StageUnlockState.COMPLETED
            }
        }
    }
    
    fun test(progression: Progression): Boolean
    
}