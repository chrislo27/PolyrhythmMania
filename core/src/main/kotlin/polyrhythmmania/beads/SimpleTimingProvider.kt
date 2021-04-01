package polyrhythmmania.beads

import java.util.concurrent.CopyOnWriteArrayList


class SimpleTimingProvider : TimingProvider {
    
    override var seconds: Float = 0f
        set(value) {
            val old = field
            field = value
            onUpdate(old, value)
        }
    override val listeners: MutableList<TimingListener> = CopyOnWriteArrayList()
    
}