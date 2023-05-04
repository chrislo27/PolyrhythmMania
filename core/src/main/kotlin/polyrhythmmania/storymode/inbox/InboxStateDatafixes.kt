package polyrhythmmania.storymode.inbox

import paintbox.Paintbox


object InboxStateDatafixes {
    
    private const val LOG_TAG: String = "InboxStateDatafixes"
    private const val CURRENT_JSON_VERSION: Int = InboxState.CURRENT_JSON_VERSION
    
    private const val VERSION_BOUNCY_ROAD_2_MOVE: Int = 1
    
    fun runDatafix(version: Int, inboxState: InboxState) {
        if (version == VERSION_BOUNCY_ROAD_2_MOVE) {
            /*
            In version 2, Bouncy Road 2 and Monkey Watch swapped places.
            Bouncy Road 2 became later in the list.
            To prevent accidental skipahead, if Bouncy Road 2 is available but the one following it isn't (Screwbots 2),
            it means the player unlocked BR2 earlier than when it now should be unlocked,
            so we set Bouncy Road 2 to be unavailable.
             */
            val bouncyRoad2Id = "contract_bouncy_road_2"
            val afterBouncyRoad2Id = "contract_screwbots2"
            val br2ItemState = inboxState.getItemState(bouncyRoad2Id)
            val afterBr2Completion = inboxState.getItemState(afterBouncyRoad2Id)?.completion ?: InboxItemCompletion.UNAVAILABLE
            if (br2ItemState != null && br2ItemState.completion != InboxItemCompletion.UNAVAILABLE && afterBr2Completion == InboxItemCompletion.UNAVAILABLE) {
                inboxState.putItemState(bouncyRoad2Id, br2ItemState.copy(completion = InboxItemCompletion.UNAVAILABLE))
                Paintbox.LOGGER.info("Set $bouncyRoad2Id to be unavailable since $afterBouncyRoad2Id was unavailable", tag = LOG_TAG)
            }
        }
    }
    
}