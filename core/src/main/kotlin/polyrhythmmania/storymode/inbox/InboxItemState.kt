package polyrhythmmania.storymode.inbox


data class InboxItemState(
        val completion: InboxItemCompletion = InboxItemCompletion.UNAVAILABLE,
        val newIndicator: Boolean = true,
        val stageCompletionData: StageCompletionData? = null,
        val playedBefore: Boolean = false,
) {
    
    companion object {
        val DEFAULT_UNAVAILABLE: InboxItemState = InboxItemState(InboxItemCompletion.UNAVAILABLE, false, null, false)
    }
    
}
