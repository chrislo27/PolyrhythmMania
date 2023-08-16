package polyrhythmmania.soundsystem.beads.ugen

import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.ugens.Gain
import kotlin.math.roundToInt


/**
 * Adds a delay. For testing purposes only.
 */
class Delay(context: AudioContext, val inouts: Int, val sampleDelay: Int)
    : Gain(context, inouts) {

    val delayBuffer: Array<FloatArray> = Array(inouts) { FloatArray(sampleDelay + bufferSize) }
    var bufferLocPut: Int = 0 // 0 until sampleDelay
    var bufferLocRead: Int = -sampleDelay // 0 until sampleDelay, negative values = no audio yet

    override fun calculateBuffer() {
        if (sampleDelay <= 0) {
            for (i in 0..<bufferSize) {
                val gain = this.gainUGen?.getValue(0, i) ?: this.gain
                for (ch in 0..<inouts) {
                    bufOut[ch][i] = gain * bufIn[ch][i]
                }
            }
        } else {
            for (i in 0..<bufferSize) {
                val gain = this.gainUGen?.getValue(0, i) ?: this.gain
                for (ch in 0..<inouts) {
                    bufOut[ch][i] = if (bufferLocRead < 0) 0f else (gain * delayBuffer[ch][bufferLocRead + i])
                }
            }
            
            bufferLocRead += bufferSize
            if (bufferLocRead > 0) {
                bufferLocRead %= sampleDelay
            }
            
            // Copy bufIn into delayBuffer
            for (ch in 0..<inouts) {
                for (i in 0..<bufferSize) {
                    delayBuffer[ch][bufferLocPut + i] = bufIn[ch][i]
                }
            }
            bufferLocPut += bufferSize
            bufferLocPut %= sampleDelay
        }
    }
}