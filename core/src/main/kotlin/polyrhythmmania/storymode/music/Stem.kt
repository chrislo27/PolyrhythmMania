package polyrhythmmania.storymode.music

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.StreamUtils
import paintbox.Paintbox
import paintbox.binding.BooleanVar
import paintbox.binding.ReadOnlyBooleanVar
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.GdxAudioReader
import polyrhythmmania.soundsystem.sample.InMemoryMusicSample
import polyrhythmmania.soundsystem.sample.MusicSample
import java.io.Closeable
import kotlin.system.measureNanoTime

class Stem(val file: FileHandle, val inMemory: Boolean = false) : Closeable {
    
    lateinit var sample: MusicSample
        private set
    lateinit var beadsMusic: BeadsMusic
        private set
    
    val isSampleAccessible: ReadOnlyBooleanVar = BooleanVar(false)
    val enoughMusicLoaded: ReadOnlyBooleanVar = BooleanVar(false)
    val musicFinishedLoading: ReadOnlyBooleanVar = BooleanVar(false)
    
    init {
        if (!inMemory) {
            // DecodingMusicSample
            val audioLoadListener = object : GdxAudioReader.AudioLoadListener {
                override fun progress(bytesReadSoFar: Long, bytesReadThisChunk: Int) {
                    if (bytesReadSoFar > 100_000L && !enoughMusicLoaded.get()) {
                        Gdx.app.postRunnable {
                            (enoughMusicLoaded as BooleanVar).set(true)
                        }
                    }
                }

                override fun onFinished(totalBytesRead: Long) {
                    Gdx.app.postRunnable {
                        (musicFinishedLoading as BooleanVar).set(true)
                    }
                }
            }
            
            val (sample, handler) = GdxAudioReader.newDecodingMusicSample(file, audioLoadListener)

            StemLoader.enqueue {
                Paintbox.LOGGER.debug("Starting story music handler stem decode for $file")
                val nano = measureNanoTime {
                    handler.decode()
                }
                Paintbox.LOGGER.debug("Finished story music handler stem decode for $file - took ${(nano / 1_000_000.0).toFloat()} ms")
            }

            this.sample = sample
            this.beadsMusic = BeadsMusic(sample)
            (isSampleAccessible as BooleanVar).set(true)
        } else {
            // InMemoryMusicSample
            StemLoader.enqueue {
                Paintbox.LOGGER.debug("Starting story music handler stem decode for $file")

                val decodedSample: InMemoryMusicSample
                val nano = measureNanoTime {
                    decodedSample = GdxAudioReader.newInMemoryMusicSample(file)
                }

                Gdx.app.postRunnable { 
                    this.sample = decodedSample
                    this.beadsMusic = BeadsMusic(sample)

                    (isSampleAccessible as BooleanVar).set(true)
                    (enoughMusicLoaded as BooleanVar).set(true)
                    (musicFinishedLoading as BooleanVar).set(true)
                }

                Paintbox.LOGGER.debug("Finished story music handler stem decode for $file - took ${(nano / 1_000_000.0).toFloat()} ms")
            }
        }
    }

    override fun close() {
        if (isSampleAccessible.get()) {
            StreamUtils.closeQuietly(sample)
        }
    }
}
