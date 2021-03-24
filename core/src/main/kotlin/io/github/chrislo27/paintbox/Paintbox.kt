package io.github.chrislo27.paintbox

import com.badlogic.gdx.Input
import io.github.chrislo27.paintbox.logging.Logger

/**
 * Holds constants and some info about Paintbox.
 */
object Paintbox {

    @Volatile
    lateinit var LOGGER: Logger

    const val DEBUG_KEY: Int = Input.Keys.F8
    val DEBUG_KEY_NAME: String = Input.Keys.toString(DEBUG_KEY)
    var debugMode: Boolean = false
    var stageOutlines: StageOutlineMode = StageOutlineMode.NONE

    enum class StageOutlineMode {
        NONE, ALL, ONLY_VISIBLE
    }

}