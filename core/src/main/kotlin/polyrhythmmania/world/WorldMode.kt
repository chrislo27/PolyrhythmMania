package polyrhythmmania.world

import polyrhythmmania.world.render.bg.NoOpWorldBackground
import polyrhythmmania.world.render.bg.WorldBackground


sealed class WorldType(val defaultBackground: WorldBackground) {
    
    class Polyrhythm(val isContinuous: Boolean)
        : WorldType(NoOpWorldBackground) {
        
        constructor() : this(false)
    }
    object Dunk : WorldType(DunkWorldBackground)
    object Assemble : WorldType(AssembleWorldBackground)
    
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

data class WorldMode(val worldType: WorldType, val endlessType: EndlessType)
