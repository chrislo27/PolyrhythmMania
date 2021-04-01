package polyrhythmmania.beads

import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.UGen
import java.util.concurrent.CopyOnWriteArrayList


open class TimingBead(context: AudioContext) : UGen(context, 0, 0), TimingProvider {

    override val listeners: MutableList<TimingListener> = CopyOnWriteArrayList()
    
    private var backingSeconds: Float = 0f
    
    override var seconds: Float
        get() = backingSeconds
        set(value) {
            ms = value * 1000.0
        }
    
    private var ms: Double = 0.0
        set(value) {
            field = value
            backingSeconds = (ms / 1000.0).toFloat()
        }
    
    override fun calculateBuffer() {
        val msDelta = context.samplesToMs(bufferSize.toDouble())
        if (msDelta != 0.0) {
            val oldSeconds = seconds
            ms += msDelta
            val newSeconds = seconds
            onUpdate(oldSeconds, newSeconds)
        }
    }
}