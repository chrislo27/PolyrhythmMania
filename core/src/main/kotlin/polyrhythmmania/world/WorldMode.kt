package polyrhythmmania.world

import polyrhythmmania.world.render.bg.NoOpWorldBackground
import polyrhythmmania.world.render.bg.WorldBackground


sealed class WorldType(val defaultBackground: WorldBackground) {
    
    class Polyrhythm() : WorldType(NoOpWorldBackground) {
        // Not an object to retain compatibility
    }
    
    object Dunk : WorldType(DunkWorldBackground.Default)
    
    object Assemble : WorldType(AssembleWorldBackground)
    
}

data class WorldMode(val worldType: WorldType)
