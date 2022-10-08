package polyrhythmmania.storymode.inbox.unlock

import polyrhythmmania.storymode.inbox.state.InboxState


open class Progression(val inboxState: InboxState, val stages: List<UnlockStage>) {
    
    val stagesByID: Map<String, UnlockStage> = stages.associateBy { it.id }
    val unlocked: MutableMap<UnlockStage, StageUnlockState> = stages.associateWith { StageUnlockState.LOCKED }.toMutableMap()
    
    init {
        checkAll()
    }
    
    fun checkAll() {
        stages.forEach { unlocked[it] = StageUnlockState.LOCKED }
        stages.forEach { stage ->
            if (stage.unlockReqs.test(this)) {
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
