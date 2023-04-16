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
    
    open var gain: Float = 1f

    /**
     * Current position of the player in milliseconds
     */
    abstract var position: Double
    abstract val durationMs: Double
    abstract var pitch: Float
//    abstract var pitchUGen: UGen
    abstract var loopType: SamplePlayer.LoopType
    abstract var loopStartMs: Float
    abstract var loopEndMs: Float
    abstract var killOnEnd: Boolean

    fun useLoopParams(params: LoopParams) {
        this.loopType = params.loopType
        this.loopStartMs = params.startPointMs.toFloat()
        if (params.endPointMs > 0) {
            this.loopEndMs = params.endPointMs.toFloat()
        } else if (params.endPointMs <= 0) {
            this.loopEndMs = durationMs.toFloat()
        }
    }
    
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

