package polyrhythmmania.soundsystem

import com.badlogic.gdx.utils.Disposable
import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.UGen
import net.beadsproject.beads.data.Sample
import net.beadsproject.beads.ugens.SamplePlayer
import polyrhythmmania.soundsystem.sample.MusicSample
import polyrhythmmania.soundsystem.sample.MusicSamplePlayer
import polyrhythmmania.soundsystem.sample.PlayerLike
import polyrhythmmania.soundsystem.sample.SamplePlayerWrapper


abstract class BeadsAudio(val channels: Int, val sampleRate: Float) {

    abstract fun createPlayer(context: AudioContext): PlayerLike
    
//    abstract override fun dispose()
}

/**
 * An implementation of [BeadsAudio] that uses the Beads [Sample] as the data source.
 */
class BeadsSound(val sample: Sample) 
    : BeadsAudio(sample.numChannels, sample.sampleRate) {
    
    override fun createPlayer(context: AudioContext): SamplePlayerWrapper {
        return SamplePlayerWrapper(SamplePlayer(context, sample))
    }

//    override fun dispose() {
//    }
}

/**
 * An implementation of [BeadsAudio] that uses [MusicSample] as the data source.
 */
class BeadsMusic(val musicSample: MusicSample)
    : BeadsAudio(musicSample.nChannels, musicSample.sampleRate) {
    
    override fun createPlayer(context: AudioContext): MusicSamplePlayer {
        return MusicSamplePlayer(this.musicSample, context)
    }

//    override fun dispose() {
//    }
}
