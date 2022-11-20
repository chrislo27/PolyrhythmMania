package polyrhythmmania.storymode.inbox.progression

import polyrhythmmania.storymode.inbox.InboxState


open class Progression(val stages: List<UnlockStage>) {
    
    val stagesByID: Map<String, UnlockStage> = stages.associateBy { it.id }
    private val unlocked: MutableMap<UnlockStage, StageUnlockState> = stages.associateWith { StageUnlockState.LOCKED }.toMutableMap()


    /**
     * Returns a map of what stages got unlocked and what stages got completed.
     * Key is what the new [StageUnlockState] is, value is list of [UnlockStage] that changed to that state.
     */
    fun updateUnlockStages(inboxState: InboxState): Map<StageUnlockState, List<UnlockStage>> {
        val copy = unlocked.toMap()
        stages.forEach { unlocked[it] = StageUnlockState.LOCKED }
        stages.forEach { stage ->
            if (stage.unlockReqs.testShouldBeUnlocked(this)) {
                unlocked[stage] = StageUnlockState.UNLOCKED
            }
            if (stage.isCompleted(inboxState)) {
                unlocked[stage] = StageUnlockState.COMPLETED
            }
        }
        
        return stages.groupBy { stage ->
            val oldState = copy[stage]
            val currentState = unlocked[stage]
            
            if (currentState != null && currentState != oldState) {
                currentState
            } else {
                StageUnlockState.LOCKED
            }
        }.filterKeys { it != StageUnlockState.LOCKED }
    }
    
    fun getStageStateByID(stageID: String): StageUnlockState {
        return unlocked.getValue(stagesByID.getValue(stageID))
    }
    
}
