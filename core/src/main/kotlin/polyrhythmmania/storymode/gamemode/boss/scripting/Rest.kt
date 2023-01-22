package polyrhythmmania.storymode.gamemode.boss.scripting

import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event


/**
 * A special event type used by [Script] to indicate a delay. Does nothing on its own.
 */
class Rest(val restDuration: Float, engine: Engine) : Event(engine) {

    init {
        this.width = restDuration
    }

    override fun readyToDelete(): Boolean {
        return true
    }
}
