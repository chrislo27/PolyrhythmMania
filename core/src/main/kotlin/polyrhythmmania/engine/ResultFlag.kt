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

    object None : ResultFlag() {

        override fun toString(): String {
            return "ResultFlag.None"
        }
    }

    /**
     * Not currently used.
     */
    object Pass : ResultFlag() {

        override fun toString(): String {
            return "ResultFlag.Pass"
        }
    }

    open class Fail(val tagline: ReadOnlyVar<String>) : ResultFlag() {

        object Generic : Fail(Localization.getVar("resultFlag.fail.generic")) {

            override fun toString(): String {
                return "ResultFlag.Fail.Generic"
            }
        }

        object PerfectLost : Fail(Localization.getVar("resultFlag.fail.perfectLost")) {

            override fun toString(): String {
                return "ResultFlag.Fail.PerfectLost"
            }
        }

        class RanOutOfLives(val onlyOne: Boolean) : Fail(Localization.getVar(if (onlyOne) "resultFlag.fail.ranOutOfLives.one" else "resultFlag.fail.ranOutOfLives")) {

            override fun toString(): String {
                return "ResultFlag.Fail.RanOutOfLives(onlyOne=${onlyOne})"
            }
        }

        object TooManyDefectiveRods : Fail(Localization.getVar("resultFlag.fail.tooManyDefectiveRods")) {

            override fun toString(): String {
                return "ResultFlag.Fail.TooManyDefectiveRods"
            }
        }

        object MonsterGoal : Fail(Localization.getVar("resultFlag.fail.monsterGoal")) {

            override fun toString(): String {
                return "ResultFlag.Fail.MonsterGoal"
            }
        }

        object LostToBoss : Fail(Localization.getVar("resultFlag.fail.generic")) {

            override fun toString(): String {
                return "ResultFlag.Fail.LostToBoss"
            }
        }

        override fun toString(): String {
            return "ResultFlag.Fail(${tagline.getOrCompute()})"
        }
    }

}
