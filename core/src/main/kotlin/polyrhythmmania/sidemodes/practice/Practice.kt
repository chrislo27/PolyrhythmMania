package polyrhythmmania.sidemodes.practice

import polyrhythmmania.PRManiaGame
import polyrhythmmania.sidemodes.SideMode
import polyrhythmmania.world.WorldMode
import polyrhythmmania.world.WorldSettings
import polyrhythmmania.world.WorldType


abstract class Practice(main: PRManiaGame) : SideMode(main) {
    
    init {
        container.world.worldMode = WorldMode(WorldType.POLYRHYTHM, false)
        container.world.showInputFeedback = true // Overrides user settings
        container.world.worldSettings = WorldSettings(showInputIndicators = true)
    }
}