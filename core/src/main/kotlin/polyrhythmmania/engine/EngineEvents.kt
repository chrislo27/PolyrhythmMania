package polyrhythmmania.engine

import paintbox.registry.AssetRegistry
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.soundsystem.sample.PlayerLike
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

open class EventPlaySFX(engine: Engine, startBeat: Float,
                   val id: String, val allowOverlap: Boolean = false,
                   val callback: (PlayerLike) -> Unit = {})
    : Event(engine) {

    init {
        this.beat = startBeat
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        val beadsSound = AssetRegistry.get<BeadsSound>(id)
        if (allowOverlap) {
            engine.soundInterface.playAudio(beadsSound, callback)
        } else {
            engine.soundInterface.playAudioNoOverlap(beadsSound, callback)
        }
    }
}

class EventApplauseSFX(engine: Engine, startBeat: Float)
    : EventPlaySFX(engine, startBeat, "sfx_applause", allowOverlap = false)

class EventChangePlaybackSpeed(engine: Engine, val newSpeed: Float)
    : Event(engine) {
    
    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        engine.playbackSpeed = newSpeed
    }
}
