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
        if (mapByID.size < items.size) {
            error("Duplicate IDs detected in this InboxItems instance: [${items.groupBy { it.id }.filter { it.value.size > 1 }.map { it.key }}]")
        }
    }
    
}