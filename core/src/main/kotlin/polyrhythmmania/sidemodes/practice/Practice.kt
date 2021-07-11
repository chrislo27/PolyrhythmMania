package polyrhythmmania.sidemodes.practice

import polyrhythmmania.PRManiaGame
import polyrhythmmania.sidemodes.SideMode
import polyrhythmmania.world.WorldMode
import polyrhythmmania.world.WorldSettings


abstract class Practice(main: PRManiaGame) : SideMode(main) {
    
    init {
        container.world.worldMode = WorldMode.POLYRHYTHM
        container.world.showInputFeedback = true // Overrides user settings
        container.world.worldSettings = WorldSettings(showInputIndicators = true)
    }
}