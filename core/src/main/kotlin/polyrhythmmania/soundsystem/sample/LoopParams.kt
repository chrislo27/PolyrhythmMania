package polyrhythmmania.soundsystem.sample

import net.beadsproject.beads.ugens.SamplePlayer


data class LoopParams(val loopType: SamplePlayer.LoopType, val startPointMs: Double, val endPointMs: Double) {
    companion object {
        val NO_LOOP_FORWARDS = LoopParams(SamplePlayer.LoopType.NO_LOOP_FORWARDS, 0.0, 0.0)
        val LOOP_FORWARDS_ENTIRE = LoopParams(SamplePlayer.LoopType.LOOP_FORWARDS, 0.0, -1.0)
    }
}

