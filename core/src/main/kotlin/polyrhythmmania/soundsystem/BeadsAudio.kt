package polyrhythmmania.soundsystem

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.data.Sample
import net.beadsproject.beads.ugens.SamplePlayer
import polyrhythmmania.soundsystem.sample.*


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



class BeadsSoundLoader(resolver: FileHandleResolver)
    : AsynchronousAssetLoader<BeadsSound, BeadsSoundLoader.BeadsSoundLoaderParam>(resolver) {
    class BeadsSoundLoaderParam : AssetLoaderParameters<BeadsSound>()

    var sound: BeadsSound? = null

    override fun getDependencies(fileName: String?, file: FileHandle?, parameter: BeadsSoundLoaderParam?): Array<AssetDescriptor<Any>>? {
        return null
    }

    override fun loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: BeadsSoundLoaderParam?) {
        sound = GdxAudioReader.newSound(file)
    }

    override fun loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: BeadsSoundLoaderParam?): BeadsSound? {
        val s = sound
        sound = null
        return s
    }
}

class BeadsMusicLoader(resolver: FileHandleResolver) 
    : AsynchronousAssetLoader<BeadsMusic, BeadsMusicLoader.BeadsMusicLoaderParam>(resolver) {
    class BeadsMusicLoaderParam : AssetLoaderParameters<BeadsMusic>()

    var music: BeadsMusic? = null

    override fun getDependencies(fileName: String?, file: FileHandle?, parameter: BeadsMusicLoaderParam?): Array<AssetDescriptor<Any>>? {
        return null
    }

    override fun loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: BeadsMusicLoaderParam?) {
        music = GdxAudioReader.newMusic(file)
    }

    override fun loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: BeadsMusicLoaderParam?): BeadsMusic? {
        val s = music
        music = null
        return s
    }
}

