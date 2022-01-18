package polyrhythmmania.soundsystem.beads.ugen

import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.UGen
import net.beadsproject.beads.core.UGenChain


/**
 * A one-after-another sequence of [UGen]s.
 */
open class UGenSeq(context: AudioContext, inouts: Int, val sequence: List<UGen>)
    : UGenChain(context, inouts, inouts) {

    init {
        outputInitializationRegime = OutputInitializationRegime.ZERO
        
        if (sequence.isNotEmpty()) {
            this.drawFromChainInput(sequence.first())
            
            for ((index, ugen) in sequence.withIndex()) {
                if (index == 0) continue
                ugen.addInput(sequence[index - 1])
            }
            
            this.addToChainOutput(sequence.last())
        }
    }

}