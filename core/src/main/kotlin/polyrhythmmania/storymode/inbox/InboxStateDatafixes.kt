package polyrhythmmania.storymode.inbox

import paintbox.Paintbox


object InboxStateDatafixes {
    
    private const val LOG_TAG: String = "InboxStateDatafixes"
    private const val CURRENT_JSON_VERSION: Int = InboxState.CURRENT_JSON_VERSION
    
    private const val VERSION_BOUNCY_ROAD_2_MOVE: Int = 1
    
    fun runDatafix(version: Int, inboxState: InboxState) {
        if (version == VERSION_BOUNCY_ROAD_2_MOVE) {
            datafixBouncyRoad2MonkeyWatchSwap(inboxState)
            datafixAddCatchyTune(inboxState)
        }
    }

    private fun genericDatafixItemMovedForward(
        inboxState: InboxState,
        itemIDThatMovedForward: String,
        itemIDImmediatelyAfterThat: String,
    ) {
        val movedItemState = inboxState.getItemState(itemIDThatMovedForward)
        val afterMovedCompletion =
            inboxState.getItemState(itemIDImmediatelyAfterThat)?.completion ?: InboxItemCompletion.UNAVAILABLE
        if (movedItemState != null && movedItemState.completion != InboxItemCompletion.UNAVAILABLE && afterMovedCompletion == InboxItemCompletion.UNAVAILABLE) {
            inboxState.putItemState(
                itemIDThatMovedForward,
                movedItemState.copy(completion = InboxItemCompletion.UNAVAILABLE)
            )
            Paintbox.LOGGER.info(
                "Set $itemIDThatMovedForward to be unavailable since $itemIDImmediatelyAfterThat was unavailable",
                tag = LOG_TAG
            )
        }
    }
    
    private fun datafixBouncyRoad2MonkeyWatchSwap(inboxState: InboxState) {
        /*
        In version 2, Bouncy Road 2 and Monkey Watch swapped places.
        Bouncy Road 2 became later in the list.
        To prevent accidental skipahead, if Bouncy Road 2 is available but the one following it isn't (Screwbots 2),
        it means the player unlocked BR2 earlier than when it now should be unlocked,
        so we set Bouncy Road 2 to be unavailable.
         */
        val bouncyRoad2Id = "contract_bouncy_road_2"
        val afterBouncyRoad2Id = "contract_screwbots2"
        genericDatafixItemMovedForward(inboxState, bouncyRoad2Id, afterBouncyRoad2Id)
    }
    
    private fun datafixAddCatchyTune(inboxState: InboxState) {
        /*
        In version 2, Catchy Tune took the place of Rhythm Rally 2,
        and RR2 was placed after Air Rally 2 and before Tram and Pauline.
        Same skipahead check as Bouncy Road 2 swap.
         */
        val rhythmRally2Id = "contract_rhythm_rally_2"
        val afterRhythmRally2Id = "contract_tram_and_pauline"
        genericDatafixItemMovedForward(inboxState, rhythmRally2Id, afterRhythmRally2Id)
    }
    
}