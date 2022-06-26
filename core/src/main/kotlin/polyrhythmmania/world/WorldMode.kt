package polyrhythmmania.world

import polyrhythmmania.world.render.bg.NoOpWorldBackground
import polyrhythmmania.world.render.bg.WorldBackground


sealed class WorldType(val defaultBackground: WorldBackground) {
    
    class Polyrhythm(
            /**
             * If true, this world has the wrapping effect and needs to be extended.
             * TODO: This should be changed to a separate ModifierModule
             */
            val isContinuous: Boolean
    ) : WorldType(NoOpWorldBackground) {
        constructor() : this(false)
    }
    
    object Dunk : WorldType(DunkWorldBackground)
    
    object Assemble : WorldType(AssembleWorldBackground)
    
}

data class WorldMode(val worldType: WorldType)
