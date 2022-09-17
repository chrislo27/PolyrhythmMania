package polyrhythmmania.storymode.inbox

import polyrhythmmania.storymode.inbox.state.InboxState
import polyrhythmmania.storymode.inbox.state.UnlockState


fun interface UnlockReqs {
    
    companion object {
        val ALWAYS_AVAILABLE: UnlockReqs = UnlockReqs { true }
        
        fun itemIsCompletedOrSkipped(itemID: String): UnlockReqs = UnlockReqs { inboxState ->
            val itemState = inboxState.getItemState(itemID)
            val unlockState = itemState?.unlockState ?: UnlockState.Unavailable
            unlockState is UnlockState.Completed || unlockState is UnlockState.Skipped
        }

        /**
         * Combines the [firstReq] and [secondReq] and any [more] requirements with a boolean AND function.
         * This short-circuits.
         */
        fun and(firstReq: UnlockReqs, secondReq: UnlockReqs, vararg more: UnlockReqs): UnlockReqs {
            return UnlockReqs { state ->
                firstReq(state) && secondReq(state) && more.all { it.isAvailable(state) }
            }
        }
        
        /**
         * Combines the [firstReq] and [secondReq] and any [other] requirements with a boolean OR function.
         * This short-circuits.
         */
        fun or(firstReq: UnlockReqs, secondReq: UnlockReqs, vararg other: UnlockReqs): UnlockReqs {
            return UnlockReqs { state ->
                firstReq(state) || secondReq(state) || other.any { it.isAvailable(state) }
            }
        }
    }
    
    /**
     * Returns true if this requirement has been met, and the corresponding
     * [InboxItem] can transition from unavailable to available.
     */
    fun isAvailable(state: InboxState): Boolean
    
    operator fun invoke(state: InboxState): Boolean = this.isAvailable(state)
}
