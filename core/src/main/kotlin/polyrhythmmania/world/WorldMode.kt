package polyrhythmmania.world


enum class WorldType {
    POLYRHYTHM,
    DUNK,
    ASSEMBLE,
}

enum class EndlessType(val isEndless: Boolean) {
    /**
     * The world mode is not endless.
     */
    NOT_ENDLESS(false),

    /**
     * The world mode is an endless mode with a standard game over screen.
     */
    REGULAR_ENDLESS(true),
}

data class WorldMode(val type: WorldType, val endlessType: EndlessType)
