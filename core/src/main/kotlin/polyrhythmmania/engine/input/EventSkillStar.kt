package polyrhythmmania.engine.input

import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event


class EventSkillStar(engine: Engine, val atBeat: Float) : Event(engine) {
    override fun onStart(currentBeat: Float) {
        engine.inputter.skillStarBeat = atBeat
    }
}