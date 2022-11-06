package polyrhythmmania.storymode.inbox.progression

import polyrhythmmania.storymode.inbox.progression.UnlockStageChecker.Companion.alwaysUnlocked
import polyrhythmmania.storymode.inbox.progression.UnlockStageChecker.Companion.stageToBeCompleted

class StoryModeProgression private constructor(stages: List<UnlockStage>) : Progression(stages) {
    
    companion object {
        fun contractsOnly(): StoryModeProgression {
            return StoryModeProgression(listOf(
                    UnlockStage.singleItem("tutorial1", alwaysUnlocked()),
                    UnlockStage.singleItem("fillbots", stageToBeCompleted("tutorial1")),
                    UnlockStage.singleItem("shootemup", stageToBeCompleted("fillbots")),
                    UnlockStage.singleItem("rhythm_tweezers", stageToBeCompleted("shootemup")),
                    UnlockStage.singleItem("air_rally", stageToBeCompleted("rhythm_tweezers")),
                    UnlockStage.singleItem("bunny_hop", stageToBeCompleted("air_rally")),
                    UnlockStage.singleItem("fruit_basket", stageToBeCompleted("bunny_hop")),
                    UnlockStage.singleItem("fillbots2", stageToBeCompleted("fruit_basket")),
                    UnlockStage(id = "first_contact", unlockReqs = stageToBeCompleted("fillbots2"), requiredInboxItems = listOf("first_contact"), optionalInboxItems = listOf("spaceball")),
                    UnlockStage.singleItem("toss_boys", stageToBeCompleted("first_contact")),
                    UnlockStage.singleItem("rhythm_rally", stageToBeCompleted("toss_boys")),
                    UnlockStage.singleItem("hole_in_one", stageToBeCompleted("rhythm_rally")),
                    UnlockStage(id = "crop_stomp_and_ringside", unlockReqs = stageToBeCompleted("hole_in_one"), requiredInboxItems = listOf("crop_stomp", "ringside")),
                    UnlockStage.singleItem("built_to_scale_ds", stageToBeCompleted("crop_stomp_and_ringside")),
                    UnlockStage.singleItem("air_rally_2", stageToBeCompleted("built_to_scale_ds")),
                    // TODO BELOW: TBD!
                    UnlockStage(id = "even_harder_rods_that_move_different_speeds", unlockReqs = stageToBeCompleted("air_rally_2"), requiredInboxItems = listOf("screwbots", "tram_and_pauline")),
                    UnlockStage.singleItem("hole_in_one_2", stageToBeCompleted("even_harder_rods_that_move_different_speeds")),
                    // TBD
            ))
        }
        
        fun storyMode(): StoryModeProgression {
            return StoryModeProgression(listOf(
                    // TODO
            ))
        }
    }
}
