package polyrhythmmania.storymode.inbox


object InboxDB {
    
    val allItems: Map<String, InboxItem>
    val allFolders: Map<String, InboxFolder>

    init {
        this.allItems = mutableMapOf()
        this.allFolders = mutableMapOf()

        fun addItem(inboxItem: InboxItem) {
            this.allItems[inboxItem.id] = inboxItem
        }
        fun addFolder(chain: InboxFolder) {
            this.allFolders[chain.id] = chain
        }
        fun buildAndAddFolder(id: String, vararg item: InboxItem): InboxFolder {
            val chain = InboxFolder(id, item.toList())
            item.forEach { addItem(it) }
            addFolder(chain)
            return chain
        }

        buildAndAddFolder("test_indexcard", InboxItem.IndexCard("test_indexcard", 0))
    }
    
}