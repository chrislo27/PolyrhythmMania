package polyrhythmmania.storymode.inbox


open class InboxItems {

    var items: List<InboxItem> = emptyList()
        private set
    var mapByID: Map<String, InboxItem> = emptyMap()
        private set
    
    constructor()
    
    constructor(initialItems: List<InboxItem>) {
        setItems(initialItems)
    }
    
    protected fun setItems(items: List<InboxItem>) {
        this.items = items
        this.mapByID = items.associateBy { it.id }
    }
    
}