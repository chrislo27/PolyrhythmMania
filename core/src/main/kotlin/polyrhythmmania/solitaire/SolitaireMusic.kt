package polyrhythmmania.solitaire

import com.badlogic.gdx.Gdx
import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.ugens.SamplePlayer
import paintbox.Paintbox
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.GdxAudioReader
import polyrhythmmania.soundsystem.sample.MusicSample
import polyrhythmmania.soundsystem.sample.MusicSamplePlayer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread


object SolitaireMusic {

    private val isReadyFlag: AtomicBoolean = AtomicBoolean(false)
    private val musicSample: AtomicReference<MusicSample?> = AtomicReference(null)

    private val music: BeadsMusic by lazy { BeadsMusic(getSample()) }

    init {
        thread(start = true, isDaemon = true, name = "SolitaireMusic") {
            Paintbox.LOGGER.debug("Starting solitaire music decode")
            val loadedSample = GdxAudioReader.newInMemoryMusicSample(Gdx.files.internal("music/solitaire.ogg"))
            this.musicSample.set(loadedSample)
            isReadyFlag.set(true)
            Paintbox.LOGGER.debug("Finished solitaire music decode (${loadedSample.data.size / 1024} KiB)")
        }
    }

    fun isReady(): Boolean = isReadyFlag.get()
    
    private fun getSample(): MusicSample {
        return musicSample.get() ?: error("Attempt to get musicSample before it was ready")
    }

    /**
     * Note: before calling, check if music has been loaded with [isReady]
     */
    fun createMusicPlayer(audioContext: AudioContext): MusicSamplePlayer {
        return music.createPlayer(audioContext).also { player ->
            val sample = getSample()
            player.loopStartMs = sample.samplesToMs(0.0).toFloat()
//            player.loopEndMs = sample.samplesToMs(8_061_382.0).toFloat()
            player.loopEndMs = sample.lengthMs.toFloat()
            player.loopType = SamplePlayer.LoopType.LOOP_FORWARDS
        }
    }

}
