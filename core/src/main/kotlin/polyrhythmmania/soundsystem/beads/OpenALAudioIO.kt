package polyrhythmmania.soundsystem.beads

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.AudioDevice
import com.badlogic.gdx.backends.lwjgl3.audio.OpenALAudioDevice
import com.badlogic.gdx.backends.lwjgl3.audio.OpenALLwjgl3Audio
import com.badlogic.gdx.utils.Disposable
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.AudioIO
import net.beadsproject.beads.core.UGen
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRMania
import polyrhythmmania.soundsystem.AudioDeviceSettings
import polyrhythmmania.util.metrics.timeInline
import javax.sound.sampled.*
import kotlin.concurrent.thread
import kotlin.math.roundToInt

/**
 * This is an implementation of [AudioIO] that sends audio data to a [AudioDevice].
 * Note there there is a rough warmup time of as much [latency][AudioDevice.getLatency] in milliseconds
 * where there may be OpenAL buffer underflows, i.e. unpredictable timings. It is best to not immediately play any audio
 * until that very brief period has elapsed.
 * 
 * Also note that [OpenALAudioDevice] has behaviour that doesn't follow the libGDX documentation for [AudioDevice]:
 *   - The buffer size is in *bytes* and not samples, with 4 bytes per sample
 *   - The return result of [AudioDevice.getLatency] is in *milliseconds* and not samples
 */
class OpenALAudioIO(val audioDeviceSettings: AudioDeviceSettings)
    : AudioIO() {
    
    companion object {
        private val metricsPrepareBuffer: Timer = PRMania.metrics.timer(MetricRegistry.name(this::class.java, "prepareBuffer"))
    }
    
    private inner class Lifecycle(val audioDevice: OpenALAudioDevice) : Disposable {
        val bufferSize: Int
        val bufferCount: Int
        
        init {
            val clazz = OpenALAudioDevice::class.java
            clazz.getDeclaredField("bufferSize").also { sz ->
                sz.isAccessible = true
                bufferSize = sz.getInt(audioDevice)
            }
            clazz.getDeclaredField("bufferCount").also { sz ->
                sz.isAccessible = true
                bufferCount = sz.getInt(audioDevice)
            }
        }
        
        override fun dispose() {
            audioDevice.dispose()
        }
    }

    private val threadPriority: Int = Thread.MAX_PRIORITY
    @Volatile
    private var audioThread: Thread? = null
    
    @Volatile
    private var lifecycleInstance: Lifecycle? = null

    private fun create(): Lifecycle? {
        if (this.lifecycleInstance != null) {
            return null
        }
        
        val ioAudioFormat = getContext().audioFormat
        val newAudioDevice = OpenALAudioDevice(Gdx.audio as OpenALLwjgl3Audio, ioAudioFormat.sampleRate.roundToInt(),
                ioAudioFormat.outputs == 1, audioDeviceSettings.bufferSize.coerceAtLeast(256), audioDeviceSettings.bufferCount.coerceAtLeast(3))
        val lifecycle = Lifecycle(newAudioDevice)
        this.lifecycleInstance = lifecycle
        
        return lifecycle
    }

    /**
     * Update loop called from within audio thread (thread is created in [start] function).
     */
    private fun runRealTime() {
        val context = getContext()
        val ioAudioFormat = context.audioFormat
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

    /**
     * Destroys and disposes of the active lifecycle.
     */
    private fun destroy(): Boolean {
        val lifecycle = this.lifecycleInstance
        lifecycle?.disposeQuietly()
        this.lifecycleInstance = null
        
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