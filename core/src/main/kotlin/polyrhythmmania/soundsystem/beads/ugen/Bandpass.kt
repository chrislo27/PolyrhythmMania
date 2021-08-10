package polyrhythmmania.soundsystem.beads.ugen

import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.UGenChain
import net.beadsproject.beads.ugens.BiquadFilter
import net.beadsproject.beads.ugens.Gain


class Bandpass(context: AudioContext, ins: Int, outs: Int)
    : UGenChain(context, ins, outs) {

    val highPassFilter: BiquadFilter = BiquadFilter(context, outs, BiquadFilter.Type.BESSEL_HP).setFrequency(1500f)
    val lowPassFilter: BiquadFilter = BiquadFilter(context, outs, BiquadFilter.Type.BESSEL_LP).setFrequency(1000f)
    val gain: Gain = Gain(context, outs, 2f)
    
    init {
        // Inputs to Bandpass go to highPassFilter
        // Output of gain is Bandpass's output
        
        gain.addInput(lowPassFilter)
        lowPassFilter.addInput(highPassFilter)
        
        this.drawFromChainInput(highPassFilter)
        this.addToChainOutput(gain)
    }
}
