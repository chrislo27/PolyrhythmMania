package polyrhythmmania.engine

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.Localization
import polyrhythmmania.engine.ResultFlag.None


/**
 * The result flag is used as a suggestion of how the player did when the end signal is fired in [Engine].
 * It can also be reacted to immediately when the flag changes.
 * 
 * It is reset to the [None] value in [Engine.resetMutableState].
 */
sealed class ResultFlag {

    object None : ResultFlag()

    /**
     * Not currently used.
     */
    object Pass : ResultFlag()
    
    open class Fail(val tagline: ReadOnlyVar<String>) : ResultFlag() {
        
        object Generic : Fail(Localization.getVar("resultFlag.fail.generic"))
        object PerfectLost : Fail(Localization.getVar("resultFlag.fail.perfectLost"))
        object RanOutOfLives : Fail(Localization.getVar("resultFlag.fail.ranOutOfLives"))
        object TooManyDefectiveRods : Fail(Localization.getVar("resultFlag.fail.tooManyDefectiveRods"))
        object MonsterGoal : Fail(Localization.getVar("resultFlag.fail.monsterGoal"))
        
    }
    
}
