package polyrhythmmania.engine

import paintbox.registry.AssetRegistry
import polyrhythmmania.editor.block.RowSetting
import polyrhythmmania.engine.input.InputScore
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.soundsystem.sample.PlayerLike
import polyrhythmmania.util.Semitones
import polyrhythmmania.world.EntityRodPR
import polyrhythmmania.world.Row


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
                player.gain = 0.65f
            }
        } else {
            engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_cowbell")) { player ->
                player.gain = 0.65f
            }
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

open class EventConditionalOnRods(engine: Engine, startBeat: Float, val rowSetting: RowSetting, val onSuccess: () -> Unit)
    : Event(engine) {

    init {
        this.beat = startBeat
    }

    private fun doesValidRodExistOnRow(currentBeat: Float, row: Row): Boolean {
        val world = engine.world
        return world.entities.filterIsInstance<EntityRodPR>().any {
            val results = it.inputTracker.results
            it.row == row && it.acceptingInputs && currentBeat - it.deployBeat >= 4.25f &&
                    (results.size > 0 || engine.autoInputs) && results.none { r -> r.inputScore == InputScore.MISS }
        }
    }

    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)

        val rowA = engine.world.rowA
        val rowDpad = engine.world.rowDpad
        val shouldPlay: Boolean = when (rowSetting) {
            RowSetting.ONLY_A -> doesValidRodExistOnRow(currentBeat, rowA)
            RowSetting.ONLY_DPAD -> doesValidRodExistOnRow(currentBeat, rowDpad)
            RowSetting.BOTH -> doesValidRodExistOnRow(currentBeat, rowA) && doesValidRodExistOnRow(currentBeat, rowDpad)
        }
        if (shouldPlay) {
            onSuccess()
        }
    }
}