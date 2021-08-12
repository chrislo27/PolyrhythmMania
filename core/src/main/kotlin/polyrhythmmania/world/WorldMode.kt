package polyrhythmmania.world


enum class WorldMode(val showEndlessScore: Boolean) {
    POLYRHYTHM(false),
    POLYRHYTHM_ENDLESS(true),
    DUNK(true),
    ASSEMBLE(true),
    DASH(true),
}