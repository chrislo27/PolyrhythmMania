package polyrhythmmania.soundsystem.sample

import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.UGen
import net.beadsproject.beads.ugens.SamplePlayer
import java.util.concurrent.CopyOnWriteArrayList


abstract class PlayerLike(context: AudioContext, ins: Int, outs: Int) : UGen(context, ins, outs) {

    companion object {
        val DEFAULT_LOOP_TYPE: SamplePlayer.LoopType = SamplePlayer.LoopType.NO_LOOP_FORWARDS
    }
    
    val killListeners: MutableList<(PlayerLike) -> Unit> = CopyOnWriteArrayList()

    abstract var position: Double
    abstract var pitch: UGen
    abstract var loopType: SamplePlayer.LoopType
    abstract var loopStartMs: Float
    abstract var loopEndMs: Float

    override fun kill() {
        val wasDeleted = this.isDeleted
        
        super.kill()
        
        if (!wasDeleted && this.isDeleted) {
            if (killListeners.isNotEmpty()) {
                killListeners.forEach { l ->
                    l.invoke(this)
                }
            }
        }
    }

    /**
     * Call to reset the player.
     */
    abstract fun reset()
    
}

