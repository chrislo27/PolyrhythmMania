package polyrhythmmania.storymode.inbox.unlock

import polyrhythmmania.storymode.inbox.unlock.UnlockStageChecker.Companion.stageCompleted

class StoryModeProgression() : Progression(listOf(
        UnlockStage.singleItem("tutorial1") { true /* First level, always unlocked */ },
        UnlockStage.singleItem("fillbots", stageCompleted("tutorial1")),
        UnlockStage.singleItem("shootemup", stageCompleted("fillbots")),
        UnlockStage.singleItem("rhythm_tweezers", stageCompleted("shootemup")),
        UnlockStage.singleItem("air_rally", stageCompleted("rhythm_tweezers")),
        UnlockStage.singleItem("bunny_hop", stageCompleted("air_rally")),
        UnlockStage.singleItem("fruit_basket", stageCompleted("bunny_hop")),
        UnlockStage.singleItem("fillbots2", stageCompleted("fruit_basket")),
        UnlockStage(id = "first_contact", unlockReqs = stageCompleted("fillbots2"), requiredInboxItems = listOf("first_contact"), optionalInboxItems = listOf("spaceball")),
        UnlockStage.singleItem("toss_boys", stageCompleted("first_contact")),
        UnlockStage.singleItem("rhythm_rally", stageCompleted("toss_boys")),
        UnlockStage.singleItem("hole_in_one", stageCompleted("rhythm_rally")),
        UnlockStage(id = "crop_stomp_and_ringside", unlockReqs = stageCompleted("hole_in_one"), requiredInboxItems = listOf("crop_stomp", "ringside")),
        UnlockStage.singleItem("built_to_scale_ds", stageCompleted("crop_stomp_and_ringside")),
        UnlockStage.singleItem("air_rally_2", stageCompleted("built_to_scale_ds")),
        // TODO BELOW: TBD!
        UnlockStage(id = "even_harder_rods_that_move_different_speeds", unlockReqs = stageCompleted("air_rally_2"), requiredInboxItems = listOf("screwbots", "tram_and_pauline")),
        UnlockStage.singleItem("hole_in_one_2", stageCompleted("even_harder_rods_that_move_different_speeds")),
        // TBD
))
