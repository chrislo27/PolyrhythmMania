package polyrhythmmania.engine.input


enum class InputTimingRestriction(val id: Int) {
    
    NORMAL(0), // Normal timing
    NO_BARELY(1), // Ace, Early, or Late. No Barely allowed
    ACES_ONLY(2), // Aces only
    ;
    
    companion object {
        val MAP: Map<Int, InputTimingRestriction> = entries.associateBy { it.id }
    }
}
