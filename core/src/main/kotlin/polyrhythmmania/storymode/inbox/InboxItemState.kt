package polyrhythmmania.storymode.inbox


data class InboxItemState(
        val completion: InboxItemCompletion,
        val newIndicator: Boolean,
        val stageCompletionData: StageCompletionData?,
        val playedBefore: Boolean,
        val failureCount: Int,
) {
    
    companion object {
        val DEFAULT_UNAVAILABLE: InboxItemState = InboxItemState(InboxItemCompletion.UNAVAILABLE, false, null, false, 0)
        val BRAND_NEW: InboxItemState = InboxItemState(InboxItemCompletion.UNAVAILABLE, true, null, false, 0)
    }
    
}
