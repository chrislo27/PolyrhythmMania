package polyrhythmmania.soundsystem.sample

import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.data.Sample
import net.beadsproject.beads.ugens.SamplePlayer
import kotlin.math.max
import kotlin.math.min


/**
 * A [SamplePlayer] that correctly supports a negative position when the loop type 
 * is [SamplePlayer.LoopType.NO_LOOP_FORWARDS], and correctly supports a greater-than-duration position when the
 * loop type is [SamplePlayer.LoopType.NO_LOOP_BACKWARDS].
 */
class DelayableSamplePlayer(context: AudioContext, buffer: Sample) : SamplePlayer(context, buffer) {
    override fun calculateNextPosition(i: Int) {
        if (positionEnvelope != null) {
            position = positionEnvelope.getValueDouble(0, i)
        } else {
            rate = rateEnvelope.getValue(0, i)
            when (loopType) {
                LoopType.NO_LOOP_FORWARDS, null -> {
                    position += positionIncrement * rate
                    if (position > sample.length) atEnd()
                }
                LoopType.NO_LOOP_BACKWARDS -> {
                    position -= positionIncrement * rate
                    if (position < 0) atEnd()
                }
                LoopType.LOOP_FORWARDS -> {
                    loopStart = loopStartEnvelope.getValue(0, i)
                    loopEnd = loopEndEnvelope.getValue(0, i)
                    position += positionIncrement * rate
                    if (rate > 0 && position > max(loopStart, loopEnd)) {
                        position = min(loopStart, loopEnd).toDouble()
                    } else if (rate < 0 && position < min(loopStart, loopEnd)) {
                        position = max(loopStart, loopEnd).toDouble()
                    }
                }
                LoopType.LOOP_BACKWARDS -> {
                    loopStart = loopStartEnvelope.getValue(0, i)
                    loopEnd = loopEndEnvelope.getValue(0, i)
                    position -= positionIncrement * rate
                    if (rate > 0 && position < min(loopStart, loopEnd)) {
                        position = max(loopStart, loopEnd).toDouble()
                    } else if (rate < 0 && position > max(loopStart, loopEnd)) {
                        position = min(loopStart, loopEnd).toDouble()
                    }
                }
                LoopType.LOOP_ALTERNATING -> {
                    loopStart = loopStartEnvelope.getValue(0, i)
                    loopEnd = loopEndEnvelope.getValue(0, i)
                    position += if (forwards) positionIncrement * rate else -positionIncrement * rate
                    if (forwards xor (rate < 0)) {
                        if (position > max(loopStart, loopEnd)) {
                            forwards = rate < 0
                            position = 2 * max(loopStart, loopEnd) - position
                        }
                    } else if (position < min(loopStart, loopEnd)) {
                        forwards = rate > 0
                        position = 2 * min(loopStart, loopEnd) - position
                    }
                }
            }
        }
    }
}