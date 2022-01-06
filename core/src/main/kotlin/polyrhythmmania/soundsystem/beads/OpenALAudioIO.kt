package polyrhythmmania.soundsystem.beads

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.AudioDevice
import com.badlogic.gdx.utils.Disposable
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.AudioIO
import net.beadsproject.beads.core.AudioUtils
import net.beadsproject.beads.core.UGen
import net.beadsproject.beads.core.io.JavaSoundAudioIO
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRMania
import polyrhythmmania.soundsystem.MixerTracking
import polyrhythmmania.util.OnSpinWaitJ8
import polyrhythmmania.util.metrics.timeInline
import javax.sound.sampled.*
import kotlin.concurrent.thread
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * This is an implementation of [AudioIO] that sends audio data to a [AudioDevice].
 */
class OpenALAudioIO
    : AudioIO() {
    
    companion object {
        
        private val metricsPrepareBuffer: Timer = PRMania.metrics.timer(MetricRegistry.name(this::class.java, "prepareBuffer"))
        
    }
    
    private inner class Lifecycle(val audioDevice: AudioDevice) : Disposable {
        override fun dispose() {
            audioDevice.dispose()
        }
    }

    /** Thread for running realtime audio.  */
    @Volatile
    private var audioThread: Thread? = null

    /** The priority of the audio thread.  */
    private val threadPriority: Int = Thread.MAX_PRIORITY
    
    @Volatile
    private var lifecycleInstance: Lifecycle? = null

    private fun create(): Lifecycle? {
        if (this.lifecycleInstance != null) {
            return null
        }
        val ioAudioFormat = getContext().audioFormat
        
        val newAudioDevice: AudioDevice = Gdx.audio.newAudioDevice(ioAudioFormat.sampleRate.roundToInt(), ioAudioFormat.outputs == 1)
        val lifecycle = Lifecycle(newAudioDevice)
        this.lifecycleInstance = lifecycle
        
        return lifecycle
    }
    
    /** Update loop called from within audio thread (created in start() method). */
    private fun runRealTime() {
        val context = getContext()
        val ioAudioFormat = getContext().audioFormat
        val audioFormat = ioAudioFormat.toJavaAudioFormat()
        val bufferSizeInFrames = context.bufferSize
        val sampleBufferSize = bufferSizeInFrames * ioAudioFormat.outputs
        val sampleBuffer = FloatArray(sampleBufferSize)

        val lifecycle = this.lifecycleInstance ?: return

        fun primeBuffer() {
            metricsPrepareBuffer.timeInline {
                prepareLineBuffer(audioFormat, sampleBuffer, bufferSizeInFrames)
            }
        }

        while (context.isRunning) {
            primeBuffer()
            lifecycle.audioDevice.writeSamples(sampleBuffer, 0, sampleBufferSize)
        }
    }
    
    /**
     * Read audio from UGens and copy them into a buffer ready to write to Audio Line
     * @param audioFormat The AudioFormat
     * @param interleavedSamples Interleaved samples as floats
     * @param bufferSizeInFrames The size of interleaved samples in frames
     */
    private fun prepareLineBuffer(audioFormat: AudioFormat, interleavedSamples: FloatArray, bufferSizeInFrames: Int) {
        update() // This propagates update call to context from super-method
        var i = 0
        var counter = 0
        while (i < bufferSizeInFrames) {
            for (j in 0 until audioFormat.channels) {
                interleavedSamples[counter++] = context.out.getValue(j, i)
            }
            ++i
        }
    }

    /** Shuts down JavaSound elements, SourceDataLine and Mixer.  */
    private fun destroy(): Boolean {
        val lifecycle = this.lifecycleInstance
        if (lifecycle != null) {
            lifecycle.disposeQuietly()
        }
        return true
    }

    override fun start(): Boolean {
        while (audioThread != null) {
            Thread.sleep(10L)
        }
        audioThread = thread(start = true, isDaemon = true, name = "OpenALAudioIO", priority = threadPriority) {
            create()
            runRealTime()
            destroy()
            audioThread = null
        }
        return true
    }

    override fun stop(): Boolean {
        super.stop()
        while (audioThread != null) {
            Thread.sleep(10L)
        }
        return true
    }

    override fun getAudioInput(channels: IntArray): UGen {
        // NO-OP
        return NoOpAudioInput(context, channels.size)
    }

    private class NoOpAudioInput(context: AudioContext, outs: Int) : UGen(context, outs) {
        init {
            outputInitializationRegime = OutputInitializationRegime.ZERO
            pause(true)
        }
        
        override fun calculateBuffer() {
        }
    }

}