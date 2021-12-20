package polyrhythmmania.sidemodes.practice

import polyrhythmmania.PRManiaGame
import polyrhythmmania.container.GlobalContainerSettings
import polyrhythmmania.sidemodes.SideMode
import polyrhythmmania.statistics.PlayTimeType
import polyrhythmmania.world.WorldMode
import polyrhythmmania.world.WorldSettings
import polyrhythmmania.world.WorldType


abstract class Practice(main: PRManiaGame, playTimeType: PlayTimeType) : SideMode(main, playTimeType) {
    
    init {
        container.world.worldMode = WorldMode(WorldType.POLYRHYTHM, false)
        container.world.showInputFeedback = true // Overrides user settings
        container.world.worldSettings = WorldSettings(showInputIndicators = true)
    }

}