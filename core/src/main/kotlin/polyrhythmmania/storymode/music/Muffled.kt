package polyrhythmmania.storymode.music

import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.ugens.BiquadFilter
import polyrhythmmania.soundsystem.beads.ugen.UGenSeq


class Muffled(context: AudioContext, inouts: Int) : UGenSeq(
    context, inouts, listOf(
        BiquadFilter(context, inouts, BiquadFilter.Type.BESSEL_LP).setFrequency(350f),
    )
)