package polyrhythmmania.engine

import paintbox.registry.AssetRegistry
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.util.Semitones


class EventCowbellSFX(engine: Engine, startBeat: Float, val useMeasures: Boolean)
    : Event(engine) {

    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        if (useMeasures) {
            val measurePart = engine.timeSignatures.getMeasurePart(currentBeat)
            val pitch = if (measurePart <= -1) 1f else if (measurePart == 0) Semitones.getALPitch(8) else Semitones.getALPitch(3)
            engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_cowbell")) { player ->
                player.pitch = pitch
            }
        } else {
            engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_cowbell"))
        }
    }
}

class EventApplauseSFX(engine: Engine, startBeat: Float)
    : Event(engine) {

    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_applause"))
    }
}
