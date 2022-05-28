package polyrhythmmania.storymode.inbox


object InboxDB {
    
    val allItems: Map<String, InboxItem>
    val allChains: Map<String, InboxChain>

    init {
        this.allItems = mutableMapOf()
        this.allChains = mutableMapOf()

        fun add(inboxItem: InboxItem) {
            this.allItems[inboxItem.id] = inboxItem
        }
        fun add(chain: InboxChain) {
            this.allChains[chain.id] = chain
        }

        
    }
    
}