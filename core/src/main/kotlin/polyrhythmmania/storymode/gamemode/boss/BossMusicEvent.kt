package polyrhythmmania.storymode.gamemode.boss

import net.beadsproject.beads.ugens.Glide
import paintbox.Paintbox
import polyrhythmmania.engine.AudioEvent
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.InputCalibration
import polyrhythmmania.engine.SoundInterface
import polyrhythmmania.storymode.music.Stem
import polyrhythmmania.storymode.music.StemCache
import kotlin.math.absoluteValue


class BossMusicEvent(engine: Engine, val stems: StemCache, val stemID: String, beat: Float, duration: Float) : AudioEvent(engine) {

    private val stem: Stem? get() = stems.getOrLoad(stemID)?.takeIf { it.isSampleAccessible.get() }
    private var id: Long = -1L

    init {
        this.beat = beat
        this.width = duration
    }

    override fun onAudioStart(atBeat: Float, actualBeat: Float, inputCalibration: InputCalibration) {
        this.id = -1L

        val stem = this.stem
        val audio = stem?.beadsMusic
        val audioOffsetSec = inputCalibration.audioOffsetMs / 1000f
        
        if (stem == null) {
            Paintbox.LOGGER.warn("Stem for stem ID $stemID is null")
        } else if (audio == null) {
            Paintbox.LOGGER.warn("BeadsAudio for stem ID $stemID is null")
        } else {
            val secOffset = (engine.tempos.beatsToSeconds(actualBeat) - engine.tempos.beatsToSeconds(atBeat)) - audioOffsetSec
            val id = engine.soundInterface.playAudio(audio, SoundInterface.SFXType.DYNAMIC_MUSIC) { player ->
                player.killOnEnd = true
                
                if (secOffset.absoluteValue > 0.25f) {
                    // Adjust player position if off by too much
                    player.position = secOffset * 1000.0
                }
            }
            this.id = id
        }
    }

    override fun onAudioUpdate(atBeat: Float, actualBeat: Float) {
        val musicData = engine.musicData
        engine.soundInterface.getPlayer(this.id)?.apply {
            musicData.updatePlayerWithVolumeAndRate(this, volume = musicData.volumeMap.volumeAtBeat(atBeat))
        }
    }

    override fun onAudioEnd(atBeat: Float, actualBeat: Float) {
        engine.soundInterface.getPlayer(this.id)?.apply { 
            this.addDependent(object : Glide(this.context, this.gain, 100f){
                override fun calculateBuffer() {
                    super.calculateBuffer()
                    this@apply.gain = this.value
                }
            }.apply { 
                this.value = 0f
            })
        }
    }
}
