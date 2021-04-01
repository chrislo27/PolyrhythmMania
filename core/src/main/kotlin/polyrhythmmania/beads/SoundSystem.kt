package polyrhythmmania.beads

import com.badlogic.gdx.utils.Disposable
import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.IOAudioFormat
import javax.sound.sampled.Mixer


/**
 * A wrapper for an [net.beadsproject.beads.core.AudioContext] and associated audio system utilities, using
 * [DaemonJavaSoundAudioIO] as the audio IO interface.
 */
class SoundSystem(private val mixer: Mixer,
                  val ioAudioFormat: IOAudioFormat = DEFAULT_AUDIO_FORMAT,
                  bufferSize: Int = AudioContext.DEFAULT_BUFFER_SIZE)
    : Disposable, TimingProvider {
    
    companion object {
        val AUDIO_FORMAT_44100: IOAudioFormat = IOAudioFormat(44_100f, 16, 2, 2, true, true)
        val AUDIO_FORMAT_48000: IOAudioFormat = IOAudioFormat(48_000f, 16, 2, 2, true, true)
        val DEFAULT_AUDIO_FORMAT: IOAudioFormat = AUDIO_FORMAT_44100
    }
    
    val audioContext: AudioContext =
            AudioContext(DaemonJavaSoundAudioIO().apply {
                selectMixer(mixer)
            }, bufferSize, ioAudioFormat)
    val timingProvider: TimingProvider = TimingBead(audioContext).apply { 
        audioContext.out.addDependent(this)
    }

    override var seconds: Float
        get() = timingProvider.seconds
        set(value) {
            timingProvider.seconds = value
        }
    override val listeners: MutableList<TimingListener> by timingProvider::listeners

    fun setPaused(paused: Boolean) {
        audioContext.out.pause(paused)
    }
    
    fun startRealtime() {
        audioContext.start()
    }
    
    fun stopRealtime() {
        audioContext.stop()
    }
    
    fun startNonrealtimeTimed(msToRun: Double) {
        audioContext.runForNMillisecondsNonRealTime(msToRun)
    }

    override fun dispose() {
        stopRealtime()
    }
}