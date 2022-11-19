package polyrhythmmania.storymode.inbox


enum class InboxItemCompletion(val jsonID: String) {
    
    UNAVAILABLE("unavailable"),
    AVAILABLE("available"),
    SKIPPED("skipped"),
    COMPLETED("completed"),
    ;
    
    companion object {
        val VALUES: List<InboxItemCompletion> = values().toList()
        val JSON_MAPPING: Map<String, InboxItemCompletion> = VALUES.associateBy { it.jsonID }
    }
    
    fun shouldCountAsCompleted(): Boolean {
        return when (this) {
            SKIPPED, COMPLETED -> true
            else -> false
        }
    }
    
}