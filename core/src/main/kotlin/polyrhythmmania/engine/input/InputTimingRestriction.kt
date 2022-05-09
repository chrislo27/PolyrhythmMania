package polyrhythmmania.engine.input


enum class InputTimingRestriction {
    NORMAL, // Normal timing
    NO_BARELY, // Ace, Early, or Late. No Barely allowed
    ACES_ONLY, // Aces only
}
