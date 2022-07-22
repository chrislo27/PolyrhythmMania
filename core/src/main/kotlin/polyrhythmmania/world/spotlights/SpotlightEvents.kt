package polyrhythmmania.world.spotlights

import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event


abstract class AbstractSpotlightEvent(engine: Engine, startBeat: Float) : Event(engine) {
    protected val spotlights: Spotlights 
        get() = engine.world.spotlights
}

class EventSpotlightDarknessEnable(engine: Engine, startBeat: Float, val enabled: Boolean)
    : AbstractSpotlightEvent(engine, startBeat) {

    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        engine.world.spotlights.ambientLight.enabled = this.enabled
    }
}
