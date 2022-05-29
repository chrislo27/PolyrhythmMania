package polyrhythmmania.storymode.inbox


data class InboxFolder(val id: String, val items: List<InboxItem>) {
    val firstItem: InboxItem = items.first()
}
