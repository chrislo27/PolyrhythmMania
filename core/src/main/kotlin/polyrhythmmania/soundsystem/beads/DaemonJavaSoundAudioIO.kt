package polyrhythmmania.soundsystem.beads

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.AudioIO
import net.beadsproject.beads.core.AudioUtils
import net.beadsproject.beads.core.UGen
import net.beadsproject.beads.core.io.JavaSoundAudioIO
import polyrhythmmania.PRMania
import polyrhythmmania.soundsystem.MixerTracking
import polyrhythmmania.util.metrics.timeInline
import javax.sound.sampled.*
import kotlin.concurrent.thread
import kotlin.math.min

/**
 * This is a mostly-similar implementation of [JavaSoundAudioIO], but using a daemon audio thread.
 * It also has other implementation changes for better behaviour.
 */
class DaemonJavaSoundAudioIO(startingMixer: Mixer, val systemBufferSizeInFrames: Int = DEFAULT_SYSTEM_BUFFER_SIZE)
    : AudioIO() {
    
    companion object {
        /** The default system buffer size.  */
        const val DEFAULT_SYSTEM_BUFFER_SIZE: Int = AudioContext.DEFAULT_BUFFER_SIZE * 2

        /** The number of prepared output buffers ready to go to AudioOutput  */
        const val NUM_OUTPUT_BUFFERS = 2
        
        private val metricsPrepareBuffer: Timer = PRMania.metrics.timer(MetricRegistry.name(this::class.java, "prepareBuffer"))
    }
    
    /** The mixer.  */
    private var mixer: Mixer = startingMixer

    /** The source data line.  */
    private var sourceDataLine: SourceDataLine? = null

    /** Thread for running realtime audio.  */
    @Volatile
    private var audioThread: Thread? = null

    /** The priority of the audio thread.  */
    private val threadPriority: Int = Thread.MAX_PRIORITY

    private fun create(): Boolean {
        val ioAudioFormat = getContext().audioFormat
        val audioFormat = AudioFormat(ioAudioFormat.sampleRate, ioAudioFormat.bitDepth, ioAudioFormat.outputs,
                ioAudioFormat.signed, ioAudioFormat.bigEndian)
        val mixer = this.mixer
        val info = DataLine.Info(SourceDataLine::class.java, audioFormat)
        val systemBufferSizeInFrames = this.systemBufferSizeInFrames
        val sourceDataLine = mixer.getLine(info) as SourceDataLine
        this.sourceDataLine = sourceDataLine
        if (systemBufferSizeInFrames < 0) {
            sourceDataLine.open(audioFormat)
        } else {
            val soundOutputBufferSize = systemBufferSizeInFrames * audioFormat.frameSize * 2
            sourceDataLine.open(audioFormat, soundOutputBufferSize)
        }
        MixerTracking.trackMixer(mixer)
        return true
    }
    
    /** Update loop called from within audio thread (created in start() method). */
    private fun runRealTime() {
        val context = getContext()
        val ioAudioFormat = getContext().audioFormat
        val audioFormat = ioAudioFormat.toJavaAudioFormat()
        val bufferSizeInFrames = context.bufferSize
        val outputBufferLength = bufferSizeInFrames * audioFormat.frameSize
        val outputBuffers: Array<ByteArray> = Array(NUM_OUTPUT_BUFFERS) { ByteArray(outputBufferLength) }
        val sampleBufferSize = audioFormat.channels * bufferSizeInFrames
        val interleavedOutput = FloatArray(sampleBufferSize)

        val sourceDataLine = this.sourceDataLine ?: return
        sourceDataLine.start()

        var buffersSent = 0
        var bufferOffset = 0

        fun primeBuffer(idx: Int) {
            metricsPrepareBuffer.timeInline {
                val currentBuffer = outputBuffers[idx]
                prepareLineBuffer(audioFormat, currentBuffer, interleavedOutput, bufferSizeInFrames, sampleBufferSize)
            }
        }

        // Prime the first output buffer
        if (context.isRunning) {
            primeBuffer(0)
        }

        var primed = 0

        while (context.isRunning) {
            val currentBuffer = outputBuffers[buffersSent % NUM_OUTPUT_BUFFERS]

            val available = sourceDataLine.available()
            if (available > 0) {
                val toWrite = min(outputBufferLength - bufferOffset, available)
                bufferOffset += sourceDataLine.write(currentBuffer, bufferOffset, toWrite)
                if (outputBufferLength - bufferOffset <= 0) {
                    buffersSent++
                    bufferOffset = 0
                }
            }
            if (primed <= buffersSent) {
                // we've started writing this buffer, so we can go ahead and get the next one primed
                primeBuffer((buffersSent + 1) % NUM_OUTPUT_BUFFERS)
                primed++
            } else {
                Thread.onSpinWait()
            }
        }
    }
    
    /**
     * Read audio from UGens and copy them into a buffer ready to write to Audio Line
     * @param audioFormat The AudioFormat
     * @param outputBuffer The buffer that will contain the prepared bytes for the AudioLine
     * @param interleavedSamples Interleaved samples as floats
     * @param bufferSizeInFrames The size of interleaved samples in frames
     * @param sampleBufferSize The size of our actual sample buffer size
     */
    private fun prepareLineBuffer(audioFormat: AudioFormat, outputBuffer: ByteArray, interleavedSamples: FloatArray, bufferSizeInFrames: Int, sampleBufferSize: Int) {
        update() // this propagates update call to context
        var i = 0
        var counter = 0
        while (i < bufferSizeInFrames) {
            for (j in 0 until audioFormat.channels) {
                interleavedSamples[counter++] = context.out.getValue(j, i)
            }
            ++i
        }
        AudioUtils.floatToByte(outputBuffer, 0, interleavedSamples, 0, sampleBufferSize, audioFormat.isBigEndian)
    }

    /** Shuts down JavaSound elements, SourceDataLine and Mixer.  */
    private fun destroy(): Boolean {
        val sourceDataLine = this.sourceDataLine
        if (sourceDataLine != null) {
//            sourceDataLine.drain() // Draining takes too long to complete
            sourceDataLine.stop()
            sourceDataLine.close()
            this.sourceDataLine = null
        }
        val mixer = this.mixer
        MixerTracking.untrackMixer(mixer)
        return true
    }

    override fun start(): Boolean {
        while (audioThread != null) {
            Thread.sleep(10L)
        }
        audioThread = thread(start = true, isDaemon = true, name = "DaemonJavaSoundAudioIO", priority = threadPriority) {
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