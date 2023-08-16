package polyrhythmmania.soundsystem.sample

import com.badlogic.gdx.utils.StreamUtils
import com.codahale.metrics.Timer
import polyrhythmmania.util.metrics.timeInline
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.SeekableByteChannel
import kotlin.math.floor



/**
 * A MusicSample is like a [net.beadsproject.beads.data.Sample], but only a certain section
 * is loaded into memory and the rest is fetched from a file as needed.
 *
 * The intent of this data structure is to be able to very quickly play back content from a certain point to
 * give ample time to fetch future data, without storing the entire sample in memory.
 * This also allows for fast looping as the area around the loop point will be already in memory.
 *
 * The PCM data file shall have interleaved float information (ch0_0, ch1_0, ch0_1, ch1_1, ch0_2, ch1_2, etc for a 2-channel sample).
 *
 * This [MusicSample] should be [close]d after it is no longer needed to free the [byteChannel] in use (which may be a [FileChannel]).
 */
abstract class MusicSample(
        val byteChannel: SeekableByteChannel,
        val sampleRate: Float = 44_100f, val nChannels: Int = 2
) : Closeable {

    companion object {
        val DEFAULT_START_BUFFER_MS: Double = 500.0
        val DEFAULT_PLAYBACK_BUFFER_MS: Double = 250.0
    }

    private val interpCurrent: FloatArray = FloatArray(nChannels)
    private val interpNext: FloatArray = FloatArray(nChannels)

    protected open val fileSize: Long = synchronized(byteChannel) { byteChannel.size() }

    open val nFrames: Long get() = (fileSize / 2 /*2 bytes per float*/ / nChannels)
    open val lengthMs: Double get() = (nFrames / sampleRate * 1000.0)

    protected val startBuffer: Buffer by lazy(LazyThreadSafetyMode.PUBLICATION) { Buffer(this, msToSamples(DEFAULT_START_BUFFER_MS).toInt()) }
    protected val playbackBuffer: Buffer by lazy(LazyThreadSafetyMode.PUBLICATION) { Buffer(this, msToSamples(DEFAULT_PLAYBACK_BUFFER_MS).toInt()) }

    var metricsPopulateBuffer: Timer? = null
    
    open fun moveStartBuffer(toFrame: Int) {
        if (toFrame < 0 || toFrame >= nFrames || startBuffer.position == toFrame) {
            return
        }
        startBuffer.populate(toFrame)
    }
    
    override fun close() {
        StreamUtils.closeQuietly(byteChannel)
    }

    /**
     * Return a single frame.
     *
     * If the data is not readily available this doesn't do anything to
     * frameData.
     * @param frame Must be in range, else framedata is unchanged.
     * @param frameData
     * @see net.beadsproject.beads.data.Sample.getFrame
     */
    open fun getFrame(frame: Int, frameData: FloatArray) {
        if (frame < 0 || frame >= nFrames) {
            return
        }

        if (startBuffer.isSampleInBuffer(frame)) {
            for (i in 0..<nChannels) {
                frameData[i] = startBuffer.data[i][frame - startBuffer.position]
            }
        } else {
            if (!playbackBuffer.isSampleInBuffer(frame)) {
                metricsPopulateBuffer.timeInline {
                    playbackBuffer.populate((frame - 4).coerceAtLeast(0))
                }
            }
            for (i in 0..<nChannels) {
                frameData[i] = playbackBuffer.data[i][frame - playbackBuffer.position]
            }
        }
    }

    /**
     * Retrieves a frame of audio using no interpolation. If the frame is not in
     * the sample range then zeros are returned.
     * @param posInMS The frame to read -- will take the last frame before this one.
     * @param result The framedata to fill.
     * @see net.beadsproject.beads.data.Sample.getFrameNoInterp
     */
    fun getFrameNoInterp(posInMS: Double, result: FloatArray) {
        val frame = msToSamples(posInMS)
        val frameFloor = floor(frame).toInt()
        getFrame(frameFloor, result)
    }

    /**
     * Retrieves a frame of audio using linear interpolation. If the frame is
     * not in the sample range then zeros are returned.
     * @param posInMS The frame to read -- can be fractional (e.g., 4.4).
     * @param result The framedata to fill.
     * @see net.beadsproject.beads.data.Sample.getFrameLinear
     */
    fun getFrameLinear(posInMS: Double, result: FloatArray) {
        val frame = msToSamples(posInMS)
        val frameFloor = floor(frame).toInt()
        if (frameFloor in 1..<nFrames) {
            val frameFrac = frame - frameFloor
            if (frameFloor.toLong() == nFrames - 1) {
                getFrame(frameFloor, result)
            } else { /* lerp */
                getFrame(frameFloor, interpCurrent)
                getFrame(frameFloor + 1, interpNext)
                for (i in 0..<nChannels) {
                    result[i] = ((1 - frameFrac) * interpCurrent.get(i) + frameFrac * interpNext.get(i)).toFloat()
                }
            }
        } else {
            for (i in 0..<nChannels) {
                result[i] = 0.0f
            }
        }
    }

    /**
     * Retrieves a frame of audio using cubic interpolation. If the frame is not
     * in the sample range then zeros are returned.
     * @param posInMS The frame to read -- can be fractional (e.g., 4.4).
     * @param result The framedata to fill.
     * @see net.beadsproject.beads.data.Sample.getFrameCubic
     */
    fun getFrameCubic(posInMS: Double, result: FloatArray) {
        val frame = msToSamples(posInMS)
        var a0: Float
        var a1: Float
        var a2: Float
        var a3: Float
        var mu2: Float
        var ym1: Float
        var y0: Float
        var y1: Float
        var y2: Float
        for (i in 0..<nChannels) {
            var realCurrentSample = floor(frame).toInt()
            val fractionOffset = (frame - realCurrentSample).toFloat()
            if (realCurrentSample >= 0 && realCurrentSample < nFrames - 1) {
                realCurrentSample--
                if (realCurrentSample < 0) {
                    getFrame(0, interpCurrent)
                    ym1 = interpCurrent[i]
                    realCurrentSample = 0
                } else {
                    getFrame(realCurrentSample++, interpCurrent)
                    ym1 = interpCurrent[i]
                }
                getFrame(realCurrentSample++, interpCurrent)
                interpCurrent[i].also { y0 = it }
                y1 = if (realCurrentSample >= nFrames) {
                    getFrame(nFrames.toInt() - 1, interpCurrent)
                    interpCurrent[i] // ??
                } else {
                    getFrame(realCurrentSample++, interpCurrent)
                    interpCurrent[i]
                }
                y2 = if (realCurrentSample >= nFrames) {
                    getFrame(nFrames.toInt() - 1, interpCurrent)
                    interpCurrent[i] // ??
                } else {
                    getFrame(realCurrentSample/*++*/, interpCurrent)
                    interpCurrent[i]
                }
                mu2 = fractionOffset * fractionOffset
                a0 = y2 - y1 - ym1 + y0
                a1 = ym1 - y0 - a0
                a2 = y1 - ym1
                a3 = y0
                result[i] = a0 * fractionOffset * mu2 + a1 * mu2 + (a2 * fractionOffset) + a3
            } else {
                result[i] = 0.0f
            }
        }
    }

//    /**
//     * Get a series of frames. FrameData will only be filled with the available
//     * frames. It is the caller's responsibility to count how many frames are
//     * valid. `min(nFrames - frame, frameData[0].length)` frames in
//     * frameData are valid.
//     *
//     * If the data is not readily available this doesn't do anything.
//     *
//     * @param frame The frame number (NOTE: This parameter is in frames, not in ms!)
//     * @param frameData
//     * @see net.beadsproject.beads.data.Sample.getFrames
//     */
//    fun getFrames(frame: Int, frameData: Array<FloatArray>) {
//        if (frame >= nFrames) {
//            return
//        }
//        val numFloats = Math.min(frameData[0].size, (nFrames - frame).toInt())
//        for (i in 0 until nChannels) {
//            System.arraycopy(theSampleData.get(i), frame, frameData[i], 0, numFloats)
//        }
//    }

    /**
     * Converts from milliseconds to samples based on the sample rate.
     * @param msTime the time in milliseconds.
     * @return the time in samples.
     */
    fun msToSamples(msTime: Double): Double {
        return msTime * sampleRate / 1000.0
    }

    /**
     * Converts from samples to milliseconds based on the sample rate.
     * @param sampleTime the time in samples.
     * @return the time in milliseconds.
     */
    fun samplesToMs(sampleTime: Double): Double {
        return sampleTime / sampleRate * 1000.0
    }

    class Buffer(val musicSample: MusicSample, val samples: Int, val bytesPerSample: Int = 2) {

        val nChannels: Int = musicSample.nChannels
        val data: Array<FloatArray> = Array(nChannels) { FloatArray(samples) }
        var position: Int = 0
            private set
        var size: Int = 0
            private set
        private val tmpBuffer: ByteBuffer = ByteBuffer.allocate(256 * bytesPerSample * nChannels) // Buffer size must be a multiple of bytesPerSample

        /**
         * Attempts to populate this buffer as much as possible.
         */
        fun populate(startAtSample: Int) {
            // TODO support multiple bytes per sample not 2
            if (bytesPerSample != 2)
                error("n != 2 bytes per sample is not supported yet (n = $bytesPerSample)")

            val byteChannel = musicSample.byteChannel
            synchronized(byteChannel) {
                // Read a chunk of bytes at a time
                // Convert the bytes to floats and map them to the correct channel/sample
                // Repeat until this buffer's data is full or we hit EoF
                // Set size and position properties accordingly

                val bytesPerSampleChannel = nChannels * bytesPerSample
                val startBytePos: Long = 1L * startAtSample * bytesPerSampleChannel
                var lastBytePos: Long = startBytePos
                byteChannel.position(startBytePos)

                var samplesRead: Int = 0
                tmpBuffer.rewind()
                var bytesRead: Int = byteChannel.read(tmpBuffer)
                while (bytesRead > 0) {
                    val samplesReadable = (bytesRead / bytesPerSampleChannel).coerceAtMost(samples - samplesRead)
                    if (samplesReadable == 0) break

                    var tmpPtr: Int = 0
                    for (i in 0..<samplesReadable) {
                        for (ch in 0..<nChannels) {
                            data[ch][i + samplesRead] = ((tmpBuffer.get(tmpPtr).toInt() and 0xFF) or (tmpBuffer.get(tmpPtr + 1).toInt() shl 8)) / 32_768.0f
                            tmpPtr += bytesPerSample
                        }
                    }

                    lastBytePos += samplesReadable * bytesPerSampleChannel
                    byteChannel.position(lastBytePos)

                    samplesRead += samplesReadable
                    tmpBuffer.rewind()
                    bytesRead = byteChannel.read(tmpBuffer)
                }

                position = startAtSample
                size = samplesRead
            }
        }

        fun isSampleInBuffer(sample: Int): Boolean {
            return sample in position..<(position + size)
        }

    }

}