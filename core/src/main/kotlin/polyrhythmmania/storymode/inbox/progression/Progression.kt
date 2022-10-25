package polyrhythmmania.storymode.inbox.progression

import polyrhythmmania.storymode.inbox.InboxState


open class Progression(val stages: List<UnlockStage>) {
    
    val stagesByID: Map<String, UnlockStage> = stages.associateBy { it.id }
    val unlocked: MutableMap<UnlockStage, StageUnlockState> = stages.associateWith { StageUnlockState.LOCKED }.toMutableMap()
    
    
    fun checkAll(inboxState: InboxState) {
        stages.forEach { unlocked[it] = StageUnlockState.LOCKED }
        stages.forEach { stage ->
            if (stage.unlockReqs.testShouldBeUnlocked(this)) {
                unlocked[stage] = StageUnlockState.UNLOCKED
            }
            if (stage.isCompleted(inboxState)) {
                unlocked[stage] = StageUnlockState.COMPLETED
            }
        }
    }
    
    fun getStageStateByID(stageID: String): StageUnlockState {
        return unlocked.getValue(stagesByID.getValue(stageID))
    }
    
}
