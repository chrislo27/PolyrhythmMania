package polyrhythmmania.soundsystem.sample

import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.UGen
import net.beadsproject.beads.ugens.SamplePlayer


abstract class PlayerLike(context: AudioContext, ins: Int, outs: Int) : UGen(context, ins, outs) {

    companion object {
        val DEFAULT_LOOP_TYPE: SamplePlayer.LoopType = SamplePlayer.LoopType.NO_LOOP_FORWARDS
    }

    abstract var position: Double
    abstract var pitch: UGen
    abstract var loopType: SamplePlayer.LoopType
    abstract var loopStartMs: Float
    abstract var loopEndMs: Float

    /**
     * Call to reset the player.
     */
    abstract fun reset()
    
}

