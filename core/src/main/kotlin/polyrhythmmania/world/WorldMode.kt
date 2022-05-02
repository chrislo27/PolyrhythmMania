package polyrhythmmania.world

import polyrhythmmania.world.render.bg.NoOpWorldBackground
import polyrhythmmania.world.render.bg.WorldBackground


enum class WorldType(val defaultBackground: WorldBackground) {
    POLYRHYTHM(NoOpWorldBackground),
    DUNK(DunkWorldBackground),
    ASSEMBLE(AssembleWorldBackground),
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
