package polyrhythmmania.world


enum class WorldType {
    POLYRHYTHM,
    DUNK,
    ASSEMBLE,
}

data class WorldMode(val type: WorldType, val showEndlessScore: Boolean)
