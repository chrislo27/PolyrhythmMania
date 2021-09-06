package polyrhythmmania.soundsystem.sample

import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.UGen
import net.beadsproject.beads.ugens.SamplePlayer
import net.beadsproject.beads.ugens.Static


class MusicSamplePlayer(val musicSample: MusicSample, context: AudioContext)
    : PlayerLike(context, musicSample.nChannels, musicSample.nChannels) {

    /** The position in milliseconds.  */
    override var position: Double = 0.0
//    override var pitchUGen: UGen = Static(context, 1f)
    override var pitch: Float = 1f
    override val durationMs: Double
        get() = musicSample.lengthMs

    /**
     * Only loop types [SamplePlayer.LoopType.NO_LOOP_FORWARDS] and [SamplePlayer.LoopType.LOOP_FORWARDS]
     * are supported. All other loop types will cause this [MusicSamplePlayer] to not do anything.
     */
    override var loopType: SamplePlayer.LoopType = SamplePlayer.LoopType.LOOP_FORWARDS // SamplePlayer.LoopType.NO_LOOP_FORWARDS
    override var loopStartMs: Float = 0f
    override var loopEndMs: Float = 0f

    var interpolationType: SamplePlayer.InterpolationType = SamplePlayer.InterpolationType.ADAPTIVE

    private val tmpFrame: FloatArray = FloatArray(outs)

    init {
        outputInitializationRegime = OutputInitializationRegime.ZERO
        outputPauseRegime = OutputPauseRegime.ZERO
    }

    fun prepareStartBuffer() {
        musicSample.moveStartBuffer(musicSample.msToSamples(loopStartMs.toDouble()).toInt())
    }

    override fun reset() {
        position = 0.0
    }

    fun isLoopInvalid(): Boolean = loopEndMs <= loopStartMs

    override fun calculateBuffer() {
        val currentGain = gain
        val loopType = this.loopType
        if ((loopType != SamplePlayer.LoopType.NO_LOOP_FORWARDS && loopType != SamplePlayer.LoopType.LOOP_FORWARDS)
                || (loopType == SamplePlayer.LoopType.LOOP_FORWARDS && isLoopInvalid())) {
            for (out in 0 until outs) {
                for (i in 0 until bufferSize) {
                    bufOut[out][i] = 0f
                }
            }
            return
        }

        val interpType: SamplePlayer.InterpolationType = interpolationType
//        pitch.update()
        for (i in 0 until bufferSize) {
            val pitchValue = pitch // pitch.getValue(0, i)

            position += musicSample.samplesToMs(1.0) * pitchValue * (this.musicSample.sampleRate / context.sampleRate)
            var positionAddition = 0.0
            if (loopType == SamplePlayer.LoopType.LOOP_FORWARDS && !isLoopInvalid() && pitchValue > 0 && position > loopEndMs) {
                positionAddition = position - loopEndMs
                position = loopStartMs.toDouble()
            }
            if (position < 0.0) {
                for (out in 0 until outs) {
                    tmpFrame[out] = 0f
                }
            } else {
                when (interpType) {
                    SamplePlayer.InterpolationType.NONE -> {
                        musicSample.getFrameNoInterp(position, tmpFrame)
                    }
                    SamplePlayer.InterpolationType.LINEAR -> {
                        musicSample.getFrameLinear(position, tmpFrame)
                    }
                    SamplePlayer.InterpolationType.CUBIC -> {
                        musicSample.getFrameCubic(position, tmpFrame)
                    }
                    SamplePlayer.InterpolationType.ADAPTIVE -> {
                        if (pitchValue > SamplePlayer.ADAPTIVE_INTERP_HIGH_THRESH) {
                            musicSample.getFrameNoInterp(position, tmpFrame)
                        } else if (pitchValue > SamplePlayer.ADAPTIVE_INTERP_LOW_THRESH) {
                            musicSample.getFrameLinear(position, tmpFrame)
                        } else {
                            musicSample.getFrameCubic(position, tmpFrame)
                        }
                    }
                }
            }

            for (out in 0 until outs) {
                bufOut[out][i] = tmpFrame[out] * currentGain
            }
            
            position += positionAddition
        }
    }

}