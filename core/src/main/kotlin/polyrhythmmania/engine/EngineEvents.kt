package polyrhythmmania.engine

import paintbox.registry.AssetRegistry
import polyrhythmmania.editor.block.BlockSongInfoCard
import polyrhythmmania.editor.block.RowSetting
import polyrhythmmania.engine.input.InputScore
import polyrhythmmania.soundsystem.BeadsSound
import polyrhythmmania.soundsystem.sample.PlayerLike
import polyrhythmmania.util.Semitones
import polyrhythmmania.world.EntityRodPR
import polyrhythmmania.world.Row


class EventCowbellSFX(engine: Engine, startBeat: Float, val useMeasures: Boolean)
    : AudioEvent(engine) {

    init {
        this.beat = startBeat
    }

    override fun onAudioStart(atBeat: Float, actualBeat: Float) {
        if (useMeasures) {
            val measurePart = engine.timeSignatures.getMeasurePart(this.beat)
            val pitch = if (measurePart <= -1) 1f else if (measurePart == 0) Semitones.getALPitch(8) else Semitones.getALPitch(3)
            engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_cowbell"), SoundInterface.SFXType.NORMAL) { player ->
                player.pitch = pitch
                player.gain = 0.65f
            }
        } else {
            engine.soundInterface.playAudioNoOverlap(AssetRegistry.get<BeadsSound>("sfx_cowbell"), SoundInterface.SFXType.NORMAL) { player ->
                player.gain = 0.65f
            }
        }
    }
}

open class EventPlaySFX(engine: Engine, startBeat: Float,
                   val id: String, val allowOverlap: Boolean = false,
                   val callback: (PlayerLike) -> Unit = {})
    : AudioEvent(engine) {

    init {
        this.beat = startBeat
    }

    override fun onAudioStart(atBeat: Float, actualBeat: Float) {
        val beadsSound = AssetRegistry.get<BeadsSound>(id)
        if (allowOverlap) {
            engine.soundInterface.playAudio(beadsSound, SoundInterface.SFXType.NORMAL, callback)
        } else {
            engine.soundInterface.playAudioNoOverlap(beadsSound, SoundInterface.SFXType.NORMAL, callback)
        }
    }
}

class EventChangePlaybackSpeed(engine: Engine, val newSpeed: Float)
    : Event(engine) {
    
    override fun onStart(currentBeat: Float) {
        super.onStart(currentBeat)
        engine.playbackSpeed = newSpeed
    }
}

open class EventConditionalOnRods(engine: Engine, startBeat: Float, val rowSetting: RowSetting,
                                  val mustHitAllInputs: Boolean,
                                  val onSuccess: () -> Unit)
    : Event(engine) {

    init {
        this.beat = startBeat
    }

    private fun doesValidRodExistOnRow(currentBeat: Float, row: Row): Boolean {
        val world = engine.world
        return world.entities.filterIsInstance<EntityRodPR>().any { rod ->
            val inputTracker = rod.inputTracker
            val numExpected = inputTracker.expected.count { it is EntityRodPR.ExpectedInput.Expected }
            val validResults = inputTracker.results.filter { it.inputScore != InputScore.MISS }
            val results = inputTracker.results
            rod.row == row && rod.acceptingInputs && currentBeat - rod.deployBeat >= 4.25f &&
                    (results.size > 0 || engine.autoInputs) && results.none { r -> r.inputScore == InputScore.MISS } &&
                    (!mustHitAllInputs || (validResults.size == numExpected || engine.autoInputs))
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
