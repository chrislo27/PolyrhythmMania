package polyrhythmmania.storymode.inbox


data class InboxChain(val id: String, val items: List<InboxItem>) {
    val firstItem: InboxItem = items.first()
}
