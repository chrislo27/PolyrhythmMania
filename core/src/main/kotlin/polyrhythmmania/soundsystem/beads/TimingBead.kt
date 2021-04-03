package polyrhythmmania.soundsystem.beads

import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.UGen
import polyrhythmmania.soundsystem.TimingListener
import polyrhythmmania.soundsystem.TimingProvider
import java.util.concurrent.CopyOnWriteArrayList


/**
 * A [TimingProvider] that advances when the [calculateBuffer] method is called.
 * This is exactly timed to the advancement of the [AudioContext] update rate.
 * However, this may be subject to jitter in real time since the [AudioContext] may not necessarily be updated
 * at a steady rate.
 */
open class TimingBead(context: AudioContext) : UGen(context, 0, 0), TimingProvider {

    override val listeners: MutableList<TimingListener> = CopyOnWriteArrayList()
    
    @Volatile
    private var backingSeconds: Float = 0f
    
    override var seconds: Float
        get() = backingSeconds
        set(value) {
            ms = value * 1000.0
        }
    
    @Volatile
    private var ms: Double = 0.0
        set(value) {
            field = value
            backingSeconds = (ms / 1000.0).toFloat()
        }
    
    private var nanoTime = System.nanoTime()
    
    override fun calculateBuffer() {
        val msDelta = context.samplesToMs(bufferSize.toDouble())
        if (msDelta != 0.0) {
//            val realtimeMsDelta = (System.nanoTime() - nanoTime) / 1000000.0
//            nanoTime = System.nanoTime()
//            println("$msDelta    $realtimeMsDelta")
            
            val oldSeconds = seconds
            ms += msDelta
            val newSeconds = seconds
            onUpdate(oldSeconds, newSeconds)
        }
    }
}