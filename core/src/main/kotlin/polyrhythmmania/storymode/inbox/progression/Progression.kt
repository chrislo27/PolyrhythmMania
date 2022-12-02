package polyrhythmmania.storymode.inbox.progression

import polyrhythmmania.storymode.inbox.InboxItems
import polyrhythmmania.storymode.inbox.InboxState


class Progression(val stages: List<UnlockStage>) {
    
    companion object {
        fun debugItemsInOrder(inboxItems: InboxItems): Progression {
            val stages = mutableListOf<UnlockStage>()

            var prevStageID = ""
            inboxItems.items.forEachIndexed { index, inboxItem ->
                val newStage = UnlockStage.singleItem(inboxItem.id, if (index == 0) UnlockStageChecker.alwaysUnlocked() else UnlockStageChecker.stageToBeCompleted(prevStageID))
                stages += newStage
                prevStageID = newStage.id
            }

            return Progression(stages)
        }
    }
    
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
            if (stage.unlockReqs.testShouldStageBecomeUnlocked(this, inboxState)) {
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
        stagesByID.getValue(stageID)
        return unlocked.getValue(stagesByID.getValue(stageID))
    }
    
}
