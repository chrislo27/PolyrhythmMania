package polyrhythmmania.storymode.inbox


enum class InboxItemCompletion(val jsonID: String) {
    
    UNAVAILABLE("unavailable"),
    AVAILABLE("available"),
    SKIPPED("skipped"),
    COMPLETED("completed"),
    ;
    
    companion object {
        val JSON_MAPPING: Map<String, InboxItemCompletion> = entries.associateBy { it.jsonID }
    }
    
    fun shouldCountAsCompleted(): Boolean {
        return when (this) {
            SKIPPED, COMPLETED -> true
            else -> false
        }
    }
    
}