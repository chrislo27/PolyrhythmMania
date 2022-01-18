package polyrhythmmania.soundsystem.beads.ugen

import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.UGenChain
import net.beadsproject.beads.ugens.BiquadFilter
import net.beadsproject.beads.ugens.Gain


class Bandpass(context: AudioContext, inouts: Int)
    : UGenSeq(context, inouts, listOf(
        BiquadFilter(context, inouts, BiquadFilter.Type.BESSEL_HP).setFrequency(1500f),
        BiquadFilter(context, inouts, BiquadFilter.Type.BESSEL_LP).setFrequency(1000f),
        Gain(context, inouts, 2f)
    ))
